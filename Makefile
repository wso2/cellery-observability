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
clean: clean.observability-agent
	mvn clean -f pom.xml

.PHONY: clean.observability-agent
clean.observability-agent:
	rm -rf ./components/global/observability-agent/target
	rm -rf ./docker/telemetry-agent/target
	rm -rf ./docker/tracing-agent/target


.PHONY: init
init: init.observability-agent

.PHONY: init.observability-agent
init.observability-agent: init.tools

.PHONY: init.tools
init.tools:
	@command -v goimports >/dev/null; \
	if [ $$? -ne 0 ]; then \
		echo "goimports not found. Running 'go get golang.org/x/tools/cmd/goimports'"; \
		go get golang.org/x/tools/cmd/goimports; \
	fi;


.PHONY: check-style
check-style: check-style.observability-agent

.PHONY: check-style.observability-agent
check-style.observability-agent: init.tools
	test -z "$$(goimports -local $(PROJECT_PKG) -l ./components/global/observability-agent | tee /dev/stderr)"


.PHONY: code-format
code-format: code-format.observability-agent

.PHONY: code-format.observability-agent
code-format.observability-agent:
	@goimports -w -local $(PROJECT_PKG) -l ./components/global/observability-agent


.PHONY: build
build: build.observability-agent
	mvn install -f components/pom.xml -Dmaven.test.skip=true

.PHONY: build.observability-agent
build.observability-agent:
	GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -o ./components/global/observability-agent/target/telemetry-agent ./components/global/observability-agent/cmd/telemetry-agent/
	GOOS=linux GOARCH=amd64 CGO_ENABLED=0 go build -o ./components/global/observability-agent/target/tracing-agent ./components/global/observability-agent/cmd/tracing-agent/


.PHONY: test
test: test.observability-agent
	mvn test -f components/pom.xml

.PHONY: test.observability-agent
test.observability-agent:
	go test `go list ./components/global/observability-agent/pkg/... | grep -v ./components/global/observability-agent/pkg/signals` -race -covermode=atomic -coverprofile=./components/global/observability-agent/target/coverage.txt


.PHONY: docker
docker:
	[ -d "docker/portal/target" ] || mvn initialize -f docker/pom.xml
	cd docker/portal; \
	docker build -t $(DOCKER_REPO)/observability-portal:$(DOCKER_IMAGE_TAG) .
	cd docker/sp; \
	docker build -t ${DOCKER_REPO}/sp-worker:${DOCKER_IMAGE_TAG} .
	@rm -rf ./docker/telemetry-agent/target
	@mkdir ./docker/telemetry-agent/target
	cp ./components/global/observability-agent/target/telemetry-agent ./docker/telemetry-agent/target/telemetry-agent
	cd docker/telemetry-agent; \
	docker build -t ${DOCKER_REPO}/telemetry-agent:${DOCKER_IMAGE_TAG} .
	@rm -rf ./docker/tracing-agent/target
	@mkdir ./docker/tracing-agent/target
	cp ./components/global/observability-agent/target/tracing-agent ./docker/tracing-agent/target/tracing-agent
	cd docker/tracing-agent; \
	docker build -t ${DOCKER_REPO}/tracing-agent:${DOCKER_IMAGE_TAG} .


.PHONY: docker-push
docker-push: docker
	docker push $(DOCKER_REPO)/sp-worker:$(DOCKER_IMAGE_TAG)
	docker push $(DOCKER_REPO)/observability-portal:$(DOCKER_IMAGE_TAG)
	docker push $(DOCKER_REPO)/telemetry-agent:$(DOCKER_IMAGE_TAG)
	docker push $(DOCKER_REPO)/tracing-agent:$(DOCKER_IMAGE_TAG)
