package io.cellery.observability.k8s.client.crds;

import io.fabric8.kubernetes.client.CustomResourceList;

/**
 * This class implements the Event Source which can be used to listen for k8s pod changes.
 */
public class CellList extends CustomResourceList<Cell> {
    private static final long serialVersionUID = 1L;
}
