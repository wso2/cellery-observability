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

package io.cellery.observability.k8s.client.cells.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import java.util.HashMap;
import java.util.Map;

/**
 * This represents the serializable class for ServicesTemplate spec in cell yaml.
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "autoscaling",
        "container",
        "protocol",
        "replicas",
        "serviceAccountName",
        "servicePort"
})
@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)

public class ServicesTemplateSpec implements KubernetesResource {

    private static final long serialVersionUID = 1L;

    @JsonProperty("autoscaling")
    private Object autoscaling;
    @JsonProperty("container")
    private Container container;
    @JsonProperty("protocol")
    private String protocol;
    @JsonProperty("replicas")
    private Integer replicas;
    @JsonProperty("serviceAccountName")
    private String serviceAccountName;
    @JsonProperty("servicePort")
    private Integer servicePort;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("autoscaling")
    public Object getAutoscaling() {
        return autoscaling;
    }

    @JsonProperty("autoscaling")
    public void setAutoscaling(Object autoscaling) {
        this.autoscaling = autoscaling;
    }

    @JsonProperty("container")
    public Container getContainer() {
        return container;
    }

    @JsonProperty("container")
    public void setContainer(Container container) {
        this.container = container;
    }

    @JsonProperty("protocol")
    public String getProtocol() {
        return protocol;
    }

    @JsonProperty("protocol")
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @JsonProperty("replicas")
    public Integer getReplicas() {
        return replicas;
    }

    @JsonProperty("replicas")
    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    @JsonProperty("serviceAccountName")
    public String getServiceAccountName() {
        return serviceAccountName;
    }

    @JsonProperty("serviceAccountName")
    public void setServiceAccountName(String serviceAccountName) {
        this.serviceAccountName = serviceAccountName;
    }

    @JsonProperty("servicePort")
    public Integer getServicePort() {
        return servicePort;
    }

    @JsonProperty("servicePort")
    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
