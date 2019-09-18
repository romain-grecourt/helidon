#!/bin/bash -e
#
# Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -o pipefail || true  # trace ERR through pipes
set -o errtrace || true # trace ERR through commands and functions
set -o errexit || true  # exit the script if any statement returns a non-true return value

on_error(){
    CODE="${?}" && \
    set +x && \
    printf "[ERROR] Error(code=%s) occurred at %s:%s command: %s\n" \
        "${CODE}" "${BASH_SOURCE}" "${LINENO}" "${BASH_COMMAND}"
}
trap on_error ERR

# Path to this script
if [ -h "${0}" ] ; then
    readonly SCRIPT_PATH="$(readlink "${0}")"
else
    readonly SCRIPT_PATH="${0}"
fi
readonly SCRIPT=$(basename ${SCRIPT_PATH})

usage(){
  cat <<EOF

DESCRIPTION: Push an image tar to a remote registry

USAGE:

$(basename ${SCRIPT}) [--help] [--load] --path=PATH --name=NAME

  --image=PATH
          Path to the image tar to be pushed.

  --user=USERNAME
          Registry username

  --password=PASSWORD
          Registry password

  --debug
          Print debug output.

  --help
          Prints the usage and exits.

EOF
}

# parse command line args
ARGS=( "${@}" )
for ((i=0;i<${#ARGS[@]};i++))
{
    ARG=${ARGS[${i}]}
    case ${ARG} in
    "--help")
        usage
        exit 0
        ;;
    "--debug")
        readonly DEBUG=true
        ;;
    "--image="*)
        readonly IMAGE_TAR=${ARG#*=}
        ;;
    "--user="*)
        readonly UNAME=${ARG#*=}
        ;;
    "--password="*)
        readonly UPASSWD=${ARG#*=}
        ;;
    *)
        echo "ERROR: unkown option: ${ARG}"
        usage
        exit 1
        ;;
  esac
}

if [ -z "${IMAGE_TAR}" ] ; then
    echo "ERROR: --image option is required"
    usage
    exit 1
elif [ ! -f "${IMAGE_TAR}" ] ; then
    echo "ERROR: ${IMAGE_TAR} is not a valid file"
    exit 1
fi

if [ -z "${UNAME}" ] ; then
    echo "ERROR: --user option is required"
    usage
    exit 1
fi

if [ -z "${UPASSWD}" ] ; then
    echo "ERROR: --password option is required"
    usage
    exit 1
fi

if ! type jq > /dev/null 2>&1; then
    echo "ERROR: jq not found in PATH"
    exit 1
fi

if [ -z "${DEBUG}" ] ; then
    exec 2> /dev/null
    readonly DEBUG=false
fi

readonly IMAGE_MANIFEST="$(mktemp -t XXX}).json"
tar --to-stdout -xf ${IMAGE_TAR} manifest.json > ${IMAGE_MANIFEST}
echo "INFO: image_manifest=${IMAGE_MANIFEST}"

readonly REPO=$(jq -r '.[].RepoTags[0]' ${IMAGE_MANIFEST})
readonly IMAGE_NAMESPACE=${REPO%%/*}
readonly IMAGE_NAME=$(echo ${REPO} | sed -E s@'.*/(.*):.*'@'\1'@g)
readonly IMAGE_TAG=${REPO##*:}
echo "INFO: image_namespace=${IMAGE_NAMESPACE}"
echo "INFO: image_name=${IMAGE_NAME}"
echo "INFO: image_tag=${IMAGE_TAG}"

readonly AUTH_DOMAIN="auth.docker.io"
readonly AUTH_SERVICE="registry.docker.io"
readonly API_URL_BASE="https://registry-1.docker.io/v2/${IMAGE_NAMESPACE}/${IMAGE_NAME}"

get_token(){
    local tokenOps="service=${AUTH_SERVICE}"
    tokenOps="${tokenOps}&scope=repository:${IMAGE_NAMESPACE}/${IMAGE_NAME}:push,pull"
    tokenOps="${tokenOps}&offline_token=1&client_id=${SCRIPT}"
    curl -X GET -u ${UNAME}:${UPASSWD} "https://${AUTH_DOMAIN}/token?${tokenOps}" | jq -r '.token'
}

echo "INFO: retrieving auth token"
readonly TOKEN=$(get_token)
readonly AUTH_HEADER="Authorization: Bearer ${TOKEN}"

get_header(){
    grep -i "${1}: " ${2} | awk '{print $2}' | tr -d '\r'
}

create_blob(){
    local responseHeaders=$(mktemp -t XXX})
    curl -v -X POST \
        -D ${responseHeaders} \
        -H "${AUTH_HEADER}" \
       "${API_URL_BASE}/blobs/uploads/"
    local status=$(head -1 ${responseHeaders})
    if [[ ${status} =~ .*[202].* ]] ; then
        UPLOAD_LOCATION=$(get_header "Location" ${responseHeaders})
    else
        echo "ERROR: ${status}"
        return 1
    fi
}

blob_size(){
    local blobLocation=$(get_header "Location" ${RESPONSE_HEADERS})
    curl -v -L --head -H "${AUTH_HEADER}" "${blobLocation}" > ${RESPONSE_HEADERS}
    BLOB_SIZE=$(get_header "Content-Length" ${RESPONSE_HEADERS})
}

readonly IMAGE_CONFIG="$(mktemp -t XXX}).json"
readonly IMAGE_CONFIG_ENTRY=$(jq -r '.[0].Config' ${IMAGE_MANIFEST})
tar --to-stdout -xf ${IMAGE_TAR} ${IMAGE_CONFIG_ENTRY} > ${IMAGE_CONFIG}
echo "INFO: image_config=${IMAGE_CONFIG}"

readonly IMAGE_CONFIG_DIGEST="sha256:${IMAGE_CONFIG_ENTRY%%.json}"
readonly LAYER_DIGEST=$(jq -r '.rootfs.diff_ids[0]' ${IMAGE_CONFIG})
echo "INFO: image_config_digest=${IMAGE_CONFIG_DIGEST}"
echo "INFO: layer_digest=${LAYER_DIGEST}"

RESPONSE_HEADERS=$(mktemp -t XXX})

# upload layer
create_blob
tar --to-stdout -xf "${IMAGE_TAR}" $(jq -r '.[0].Layers[0]' ${IMAGE_MANIFEST}) | \
    curl -v -X PUT --data-binary @- \
        -D ${RESPONSE_HEADERS} \
        -H "${AUTH_HEADER}" \
        -H "Content-Type: application/octet-stream" \
        "${UPLOAD_LOCATION}&digest=${LAYER_DIGEST}"

STATUS=$(head -1 ${RESPONSE_HEADERS})
if ! [[ ${STATUS} =~ .*[201].* ]] ; then
    echo "ERROR: ${status}"
    return 1
fi

blob_size
readonly LAYER_SIZE=${BLOB_SIZE}
echo "INFO: layer_size=${LAYER_SIZE}"

# upload image config
create_blob
curl -v -X PUT --data-binary @${IMAGE_CONFIG} \
    -D ${RESPONSE_HEADERS} \
    -H "${AUTH_HEADER}" \
    -H "Content-Type: application/vnd.docker.container.image.v1+json" \
    "${UPLOAD_LOCATION}&digest=${IMAGE_CONFIG_DIGEST}"

STATUS=$(head -1 ${RESPONSE_HEADERS})
if ! [[ ${STATUS} =~ .*[201].* ]] ; then
    echo "ERROR: ${status}"
    return 1
fi

blob_size
readonly CONFIG_SIZE=${BLOB_SIZE}
echo "INFO: config_size=${CONFIG_SIZE}"

# create distribution manifest
readonly DISTRIBUTION_MANIFEST="$(mktemp -t XXX}).json"
cat << EOF > ${DISTRIBUTION_MANIFEST}
{
    "schemaVersion": 2,
    "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
    "config": {
        "mediaType": "application/vnd.docker.container.image.v1+json",
        "size": ${CONFIG_SIZE},
        "digest": "${IMAGE_CONFIG_DIGEST}"
    },
    "layers": [
        {
            "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
            "size": ${LAYER_SIZE},
            "digest": "${LAYER_DIGEST}"
        }
    ]
}
EOF

# upload manifest
curl -v -X PUT --data-binary @${DISTRIBUTION_MANIFEST} \
    -H "${AUTH_HEADER}" \
    -D ${RESPONSE_HEADERS} \
    -H "Content-Type: application/vnd.docker.distribution.manifest.v2+json" \
    "${API_URL_BASE}/manifests/${IMAGE_TAG}"

STATUS=$(head -1 ${RESPONSE_HEADERS})
if ! [[ ${STATUS} =~ .*[201].* ]] ; then
    echo "ERROR: ${status}"
    return 1
fi

echo "DONE!"