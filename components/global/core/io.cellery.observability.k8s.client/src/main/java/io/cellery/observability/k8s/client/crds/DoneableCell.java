package io.cellery.observability.k8s.client.crds;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

/**
 * This class implements the Event Source which can be used to listen for k8s pod changes.
 */
public class DoneableCell extends CustomResourceDoneable<Cell> {
    public DoneableCell(Cell resource, Function<Cell, Cell> function) {
        super(resource, function);
    }
}
