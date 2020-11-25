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
          inheritFrom 'k8s-slave'
          defaultContainer 'jnlp'
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
          """
        }
      }
      steps {
        container('jnlp') {
          sh './etc/scripts/test-integ-mysql.sh'
        }
      }
    }
  }
}