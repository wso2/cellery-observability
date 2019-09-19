/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.cellery.observability.k8s.client.crds.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.List;

/**
 * This class implements the Ingress in Gateway.
 * */
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ingress implements KubernetesResource {

    private static final long serialVersionUID = 1L;

    @JsonProperty("extensions")
    private Extensions extensions;
    @JsonProperty("http")
    private List<HTTP> http;
    @JsonProperty("grpc")
    private List<GRPC> grpc;
    @JsonProperty("tcp")
    private List<TCP> tcp;

    @JsonProperty("extensions")
    public Extensions getExtensions() {
        return extensions;
    }

    @JsonProperty("extensions")
    public void setExtensions(Extensions extensions) {
        this.extensions = extensions;
    }

    @JsonProperty("http")
    public List<HTTP> getHttp() {
        return http;
    }

    @JsonProperty("http")
    public void setHttp(List<HTTP> http) {
        this.http = http;
    }

    @JsonProperty("grpc")
    public List<GRPC> getGrpc() {
        return grpc;
    }

    @JsonProperty("grpc")
    public void setGrpc(List<GRPC> grpc) {
        this.grpc = grpc;
    }

    @JsonProperty("tcp")
    public List<TCP> getTcp() {
        return tcp;
    }

    @JsonProperty("tcp")
    public void setTcp(List<TCP> tcp) {
        this.tcp = tcp;
    }

}
