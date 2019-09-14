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

package io.cellery.observability.k8s.client.crds.cell;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cellery.observability.k8s.client.crds.gateway.GatewayTemplate;
import io.cellery.observability.k8s.client.crds.service.ServicesTemplate;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import java.util.List;

/**
 * This represents the serializable class for cell spec.
 */
@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CellSpec implements KubernetesResource {

    private static final long serialVersionUID = 1L;

    @JsonProperty("gatewayTemplate")
    private GatewayTemplate gatewayTemplate;
    @JsonProperty("servicesTemplates")
    private List<ServicesTemplate> servicesTemplates = null;

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

}
