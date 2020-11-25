/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pipeline {
  agent {
    label "linux"
  }
  options {
    parallelsAlwaysFailFast()
  }
  environment {
    NPM_CONFIG_REGISTRY = credentials('npm-registry')
  }
  stages {
    stage('test-mysql') {
      agent {
        kubernetes {
          label 'linux-mysql'
          yaml """
spec:
  containers:
  - name: mysql
    image: mysql:8
    ports:
    - containerPort: 3306
    env:
    - name: MYSQL_USER
      value: "user"
    - name: MYSQL_PASSWORD
      value: "password"
    - name: MYSQL_ROOT_PASSWORD
      value: "root"
    - name: MYSQL_DATABASE
      value: "pokemon"
    command:
    - cat
    tty: true
  - name: jnlp
    volumeMounts:
    - mountPath: "/ci"
      name: "workspace-volume"
      readOnly: false
    - mountPath: "/tools"
      name: "jenkins-tools-volume"
    - mountPath: "/cache"
      name: "shared-cache-volume"
    - mountPath: "/home/jenkins/.gradle"
      name: "gradle-cache-volume"
    - mountPath: "/home/jenkins/.m2/repository"
      name: "maven-local-repo-cache-volume"
    - mountPath: "/home/jenkins/.m2"
      name: "maven-settings-volume"
    - mountPath: "/home/jenkins/.gradle/init.gradle"
      name: "gradle-user-home-volume"
      subPath: "init.gradle"
    - mountPath: "/home/jenkins/.gradle/gradle.properties"
      name: "gradle-user-home-volume"
      subPath: "gradle.properties"
    - mountPath: "/home/jenkins/.ssh"
      name: "ssh-config-volume"
    - mountPath: "/home/jenkins/.m2/repository/io/helidon"
      name: "maven-empty-repo-volume"
    - mountPath: "/tmp"
      name: "tmp-volume"
    workingDir: "/ci"
          """
        }
      }
      steps {
        container('mysql') {
          sh 'ps -ax'
        }
        container('jnlp') {
          sh './etc/scripts/test-integ-mysql.sh'
        }
      }
    }
    stage('release') {
      when {
        branch '**/release-*'
      }
      environment {
        GITHUB_SSH_KEY = credentials('helidonrobot-github-ssh-private-key')
        MAVEN_SETTINGS_FILE = credentials('helidonrobot-maven-settings-ossrh')
        GPG_PUBLIC_KEY = credentials('helidon-gpg-public-key')
        GPG_PRIVATE_KEY = credentials('helidon-gpg-private-key')
        GPG_PASSPHRASE = credentials('helidon-gpg-passphrase')
      }
      steps {
        sh './etc/scripts/release.sh release_build'
      }
    }
  }
}