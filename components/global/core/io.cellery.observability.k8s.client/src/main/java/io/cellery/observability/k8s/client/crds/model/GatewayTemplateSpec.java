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

package io.cellery.observability.k8s.client.crds.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import java.util.List;

/**
 * This represents the serializable class for GatewayTemplate spec in cell yaml.
 * */
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayTemplateSpec implements KubernetesResource {

    private static final long serialVersionUID = 1L;

    @JsonProperty("host")
    private String host;
    @JsonProperty("http")
    private List<HTTP> http = null;
    @JsonProperty("tcp")
    private List<TCP> tcp = null;
    @JsonProperty("grpc")
    private List<GRPC> grpc = null;
    @JsonProperty("type")
    private String type;

    @JsonProperty("host")
    public String getHost() {
        return host;
    }

    @JsonProperty("host")
    public void setHost(String host) {
        this.host = host;
    }

    @JsonProperty("http")
    public List<HTTP> getHttp() {
        return http;
    }

    @JsonProperty("http")
    public void setHttp(List<HTTP> http) {
        this.http = http;
    }

    @JsonProperty("tcp")
    public List<TCP> getTcp() {
        return tcp;
    }

    @JsonProperty("tcp")
    public void setTcp(List<TCP> tcp) {
        this.tcp = tcp;
    }

    @JsonProperty("grpc")
    public List<GRPC> getGrpc() {
        return grpc;
    }

    @JsonProperty("grpc")
    public void setGrpc(List<GRPC> grpc) {
        this.grpc = grpc;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

}
