#  Copyright (c) 2019 WSO2 Inc. (http:www.wso2.org) All Rights Reserved.
#
#  WSO2 Inc. licenses this file to you under the Apache License,
#  Version 2.0 (the "License"); you may not use this file except
#  in compliance with the License.
#  You may obtain a copy of the License at
#
#  http:www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.

PROJECT_ROOT := $(realpath $(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
DOCKER_REPO ?= celleryio
DOCKER_IMAGE_TAG ?= latest


all: clean build docker


.PHONY: clean
clean:
	mvn clean -f pom.xml


.PHONY: build
build:
	mvn install -f components/pom.xml


.PHONY: docker
docker: build
	mvn install -f docker/pom.xml -Ddocker.repo.name=${DOCKER_REPO} -Ddocker.image.tag=${DOCKER_IMAGE_TAG}


.PHONY: docker-push
docker-push:
	docker push ${DOCKER_REPO}/sp-worker:${DOCKER_IMAGE_TAG}
	docker push ${DOCKER_REPO}/observability-portal:${DOCKER_IMAGE_TAG}
