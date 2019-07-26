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
PROJECT_PKG := github.com/cellery-io/mesh-observability
DOCKER_REPO ?= wso2cellery
DOCKER_IMAGE_TAG ?= latest


all: clean init build docker


.PHONY: clean
clean: clean.mixer-adapter
	mvn clean -f pom.xml

.PHONY: clean.mixer-adapter
clean.mixer-adapter:
	rm -rf ./components/global/mixer-adapter/target
	rm -rf ./docker/mixer-adapter/target


.PHONY: init
init: init.mixer-adapter

.PHONY: init.mixer-adapter
init.mixer-adapter: init.tools
	dep ensure

.PHONY: init.tools
init.tools:
	@command -v goimports >/dev/null; \
	if [ $$? -ne 0 ]; then \
		echo "goimports not found. Running 'go get golang.org/x/tools/cmd/goimports'"; \
		go get golang.org/x/tools/cmd/goimports; \
	fi;


.PHONY: check-style
check-style: check-style.mixer-adapter

.PHONY: check-style.mixer-adapter
check-style.mixer-adapter:
	test -z "$$(goimports -local $(PROJECT_PKG) -l ./components/global/mixer-adapter | tee /dev/stderr)"


.PHONY: code-format
code-format: code-format.mixer-adapter

.PHONY: code-format.mixer-adapter
code-format.mixer-adapter:
	@goimports -w -local $(PROJECT_PKG) -l ./components/global/mixer-adapter


.PHONY: build
build: build.mixer-adapter
	mvn install -f components/pom.xml -Dmaven.test.skip=true

.PHONY: build.mixer-adapter
build.mixer-adapter:
	GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -o ./components/global/mixer-adapter/target/mixer-adapter ./components/global/mixer-adapter/cmd/mixer-adapter/


.PHONY: test
test: test.mixer-adapter
	mvn test -f components/pom.xml

.PHONY: test.mixer-adapter
test.mixer-adapter:
	go test -race -covermode=atomic -coverprofile=./components/global/mixer-adapter/target/coverage.txt ./components/global/mixer-adapter/pkg/$(TARGET)/...


.PHONY: docker
docker:
	[ -d "docker/portal/target" ] || mvn initialize -f docker/pom.xml
	cd docker/portal; \
	docker build -t $(DOCKER_REPO)/observability-portal:$(DOCKER_IMAGE_TAG) .
	cd docker/sp; \
	docker build -t ${DOCKER_REPO}/sp-worker:${DOCKER_IMAGE_TAG} .
	@rm -rf ./docker/mixer-adapter/target
	@mkdir ./docker/mixer-adapter/target
	cp ./components/global/mixer-adapter/target/mixer-adapter ./docker/mixer-adapter/target/mixer-adapter
	cd docker/mixer-adapter; \
	docker build -t ${DOCKER_REPO}/mixer-adapter:${DOCKER_IMAGE_TAG} .


.PHONY: docker-push
docker-push: docker
	docker push $(DOCKER_REPO)/sp-worker:$(DOCKER_IMAGE_TAG)
	docker push $(DOCKER_REPO)/observability-portal:$(DOCKER_IMAGE_TAG)
	docker push $(DOCKER_REPO)/mixer-adapter:$(DOCKER_IMAGE_TAG)
