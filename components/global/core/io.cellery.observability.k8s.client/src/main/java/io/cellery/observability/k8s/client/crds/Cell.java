package io.cellery.observability.k8s.client.crds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;

import javax.validation.Valid;

/**
 * This class implements the Event Source which can be used to listen for k8s pod changes.
 */

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cell extends CustomResource implements CustomCell {

    @JsonProperty("spec")
    @Valid
    private CellSpec spec;
    private static final long serialVersionUID = 1L;

    public CellSpec getSpec() {
        return spec;
    }

    public void setSpec(CellSpec spec) {
        this.spec = spec;
    }

    public String getKind() {
        return "Cell";
    }


    @Override
    public String toString() {
        return "Cell{" +
                "apiVersion='" + getApiVersion() + '\'' +
                ", metadata=" + getMetadata() +
                ", spec=" + spec +
                '}';
    }
}
