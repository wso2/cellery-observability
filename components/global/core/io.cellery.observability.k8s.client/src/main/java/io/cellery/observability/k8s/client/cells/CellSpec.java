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

package io.cellery.observability.k8s.client.cells;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cellery.observability.k8s.client.cells.model.GatewayTemplate;
import io.cellery.observability.k8s.client.cells.model.STSTemplate;
import io.cellery.observability.k8s.client.cells.model.ServicesTemplate;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.annotation.Generated;

/**
 * This represents the serializable class for cell spec.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "gatewayTemplate",
        "servicesTemplates",
        "stsTemplate"
})
@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class CellSpec implements KubernetesResource {
    private static final long serialVersionUID = 1L;

    @JsonProperty("gatewayTemplate")
    private GatewayTemplate gatewayTemplate;
    @JsonProperty("servicesTemplates")
    private List<ServicesTemplate> servicesTemplates = null;
    @JsonProperty("stsTemplate")
    private STSTemplate stsTemplate;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("gatewayTemplate")
    public GatewayTemplate getGatewayTemplate() {
        return gatewayTemplate;
    }

    @JsonProperty("gatewayTemplate")
    public void setGatewayTemplate(GatewayTemplate gatewayTemplate) {
        this.gatewayTemplate = gatewayTemplate;
    }

    @JsonProperty("servicesTemplates")
    public List<ServicesTemplate> getServicesTemplates() {
        return servicesTemplates;
    }

    @JsonProperty("servicesTemplates")
    public void setServicesTemplates(List<ServicesTemplate> servicesTemplates) {
        this.servicesTemplates = servicesTemplates;
    }

    @JsonProperty("stsTemplate")
    public STSTemplate getStsTemplate() {
        return stsTemplate;
    }

    @JsonProperty("stsTemplate")
    public void setStsTemplate(STSTemplate stsTemplate) {
        this.stsTemplate = stsTemplate;
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
