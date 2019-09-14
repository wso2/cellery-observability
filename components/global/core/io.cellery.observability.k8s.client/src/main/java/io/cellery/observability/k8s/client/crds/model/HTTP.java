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

/**
 * This represents the serializable class for HTTP in cell yaml.
 * */
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HTTP implements KubernetesResource {

    private static final long serialVersionUID = 1L;

    @JsonProperty("authenticate")
    private Boolean authenticate;
    @JsonProperty("backend")
    private String backend;
    @JsonProperty("context")
    private String context;
    @JsonProperty("definitions")
    private Object definitions;
    @JsonProperty("global")
    private Boolean global;

    @JsonProperty("authenticate")
    public Boolean getAuthenticate() {
        return authenticate;
    }

    @JsonProperty("authenticate")
    public void setAuthenticate(Boolean authenticate) {
        this.authenticate = authenticate;
    }

    @JsonProperty("backend")
    public String getBackend() {
        return backend;
    }

    @JsonProperty("backend")
    public void setBackend(String backend) {
        this.backend = backend;
    }

    @JsonProperty("context")
    public String getContext() {
        return context;
    }

    @JsonProperty("context")
    public void setContext(String context) {
        this.context = context;
    }

    @JsonProperty("definitions")
    public Object getDefinitions() {
        return definitions;
    }

    @JsonProperty("definitions")
    public void setDefinitions(Object definitions) {
        this.definitions = definitions;
    }

    @JsonProperty("global")
    public Boolean getGlobal() {
        return global;
    }

    @JsonProperty("global")
    public void setGlobal(Boolean global) {
        this.global = global;
    }

}
