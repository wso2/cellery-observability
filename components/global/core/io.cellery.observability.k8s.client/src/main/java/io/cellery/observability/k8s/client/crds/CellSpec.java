package io.cellery.observability.k8s.client.crds;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.cellery.observability.k8s.client.crds.model.GatewayTemplate;
import io.cellery.observability.k8s.client.crds.model.STSTemplate;
import io.cellery.observability.k8s.client.crds.model.ServicesTemplate;
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
