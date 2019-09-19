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

DESCRIPTION: Build a single layer image from a directory

USAGE:

$(basename ${SCRIPT}) [--help] [--load] --path=PATH --name=NAME

  --path=PATH
          Path to the source directory to create the image.

  --name=NAME
          Image name of the form repository:tag.

  --comment=TEXT
          Comment text to include in the metatada.

  --includes=INCLUDES
          List of relative include paths patterns

  --load
          Load the created image to the Docker daemon.

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
    "--path="*)
        readonly SOURCE_PATH=$(cd ${ARG#*=} ; pwd -P)
        ;;
    "--name="*)
        readonly IMAGE_NAME=${ARG#*=}
        ;;
    "--comment="*)
        readonly COMMENT=${ARG#*=}
        ;;
    "--includes="*)
        readonly INCLUDES=${ARG#*=}
        ;;
    "--load")
        readonly LOAD=true
        ;;
    "--output-file="*)
        readonly OUTPUT_FILE=${ARG#*=}
        ;;
    *)
        echo "ERROR: unkown option: ${ARG}"
        usage
        exit 1
        ;;
  esac
}

if [ -z "${IMAGE_NAME}" ] ; then
    echo "ERROR: --name option is required"
    usage
    exit 1
fi

if [ -z "${SOURCE_PATH}" ] ; then
    echo "ERROR: --path option is required"
    usage
    exit 1
fi

if [ -z "${LOAD}" ] ; then
    readonly LOAD=false
fi

if [ -z "${COMMENT}" ] ; then
    readonly COMMENT="created by ${SCRIPT}"
fi

if [ -z "${DEBUG}" ] ; then
    exec 2> /dev/null
    readonly DEBUG=false
fi

if ! type sha256sum > /dev/null 2>&1; then
    if ! type shasum > /dev/null 2>&1; then
        echo "ERROR: none of sha256sum shasum found in PATH"
        exit 1
    else
        # MacOS
        readonly SHASUM="shasum -a 256"
    fi
else
    # Linux
    readonly SHASUM="sha256sum"
fi

random_id(){
  local N B T
  for (( N=0; N < 32; ++N ))
  do
    B=$(( $RANDOM%255 ))
    if (( N == 6 ))
    then
        printf '4%x' $(( B%15 ))
    elif (( N == 8 ))
    then
        local C='89ab'
        printf '%c%x' ${C:$(( $RANDOM%${#C} )):1} $(( B%15 ))
    else
        printf '%02x' $B
    fi
  done
}

readonly IMAGE_REPO=$(echo ${IMAGE_NAME} | cut -d ':' -f1)
readonly IMAGE_TAG=$(echo ${IMAGE_NAME} | cut -d ':' -f2)
readonly WORKDIR=$(mktemp -d -t "XXX${SCRIPT}")
echo "INFO: source = ${SOURCE_PATH}"
echo "INFO: name = ${IMAGE_NAME}"
echo "INFO: image_repo = ${IMAGE_REPO}"
echo "INFO: image_tag = ${IMAGE_TAG}"
echo "INFO: workdir ${WORKDIR}"

readonly CACHE_ID=$(random_id)
mkdir -p ${WORKDIR}/${CACHE_ID}

readonly TAR_MANIFEST=$(mktemp -t "XXXtar-manifest")
echo "INFO: tar_manifest=${TAR_MANIFEST}"
if [ -z "${INCLUDES}" ] ; then
    find ${SOURCE_PATH} -type f | sed s@"${SOURCE_PATH}/"@@g > ${TAR_MANIFEST}
else
    for includedir in ${INCLUDES} ; do
        find ${SOURCE_PATH}/${includedir} -type f | sed s@"${SOURCE_PATH}/"@@g >> ${TAR_MANIFEST}
    done
fi

readonly LAYER_TAR="${WORKDIR}/${CACHE_ID}/layer.tar"
echo "INFO: creating ${LAYER_TAR}"
cat ${TAR_MANIFEST} | tar -cvf ${LAYER_TAR} --files-from - -C ${SOURCE_PATH}

echo "1.0" > ${WORKDIR}/${CACHE_ID}/VERSION
cat << EOF > ${WORKDIR}/${CACHE_ID}/json
{
  "id": "${CACHE_ID}",
  "architecture": "amd64",
  "os": "linux"
}
EOF

readonly CREATED_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
readonly LAYER_ID=$(${SHASUM} ${WORKDIR}/${CACHE_ID}/layer.tar | cut -d ' ' -f1)

cat << EOF > ${WORKDIR}/config.json
{
  "architecture": "amd64",
  "created": "${CREATED_DATE}",
  "comment": "${COMMENT}",
  "os": "linux",
  "rootfs": {
    "type": "layers",
    "diff_ids": [
      "sha256:${LAYER_ID}"
    ]
  }
}
EOF

readonly IMAGE_ID=$(${SHASUM} ${WORKDIR}/config.json | cut -d ' ' -f1)
mv ${WORKDIR}/config.json ${WORKDIR}/${IMAGE_ID}.json

cat << EOF > ${WORKDIR}/repositories
{
  "${IMAGE_REPO}": {
    "${IMAGE_TAG}": "${CACHE_ID}"
  }
}
EOF

cat << EOF > ${WORKDIR}/manifest.json
[
  {
    "Config": "${IMAGE_ID}.json",
    "RepoTags": [
      "${IMAGE_REPO}:${IMAGE_TAG}"
    ],
    "Layers": [
      "${CACHE_ID}/layer.tar"
    ]
  }
]
EOF

if [ ! -z "${OUTPUT_FILE}" ] ; then
    readonly IMAGE_TAR="${OUTPUT_FILE}"
else
    readonly IMAGE_TAR="$(mktemp -t XXX${SCRIPT}).tar"
fi

echo "INFO: creating ${IMAGE_TAR}"
tar -cvf ${IMAGE_TAR} -C ${WORKDIR} .

# load the image
if ${LOAD} ; then
    echo "INFO: loading the image to Docker"
    if ${DEBUG} ; then
        docker load -i ${IMAGE_TAR}
    else
        docker load -i ${IMAGE_TAR} 1> /dev/null
    fi
fi

if ${LOAD} && [ -z "${OUTPUT_FILE}" ] ; then
    echo "INFO: cleaning up image tar..."
    rm -f ${IMAGE_TAR}
fi