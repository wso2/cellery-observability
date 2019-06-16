package io.cellery.observability.k8s.client.crds.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import java.util.HashMap;
import java.util.Map;

/**
 * This represents the serializable class for HTTP in cell yaml.
 * */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "authenticate",
        "backend",
        "context",
        "definitions",
        "global"
})
@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class Http implements KubernetesResource {

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
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
