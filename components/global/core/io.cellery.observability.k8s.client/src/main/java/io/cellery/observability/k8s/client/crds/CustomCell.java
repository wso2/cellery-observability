package io.cellery.observability.k8s.client.crds;

import io.fabric8.kubernetes.api.model.HasMetadata;

/**
 * This class implements the Event Source which can be used to listen for k8s pod changes.
 */
public interface CustomCell extends HasMetadata {
    CellSpec getSpec();
    String getKind();
}
