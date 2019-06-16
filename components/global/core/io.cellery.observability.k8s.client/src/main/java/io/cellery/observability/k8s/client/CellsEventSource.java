package io.cellery.observability.k8s.client;

import io.cellery.observability.k8s.client.crds.Cell;
import io.cellery.observability.k8s.client.crds.CellList;
import io.cellery.observability.k8s.client.crds.DoneableCell;
import io.cellery.observability.k8s.client.crds.model.Http;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.apache.log4j.Logger;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.ConnectionUnavailableException;
import org.wso2.siddhi.core.stream.input.source.Source;
import org.wso2.siddhi.core.stream.input.source.SourceEventListener;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.core.util.transport.OptionHolder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class implements the Event Source which can be used to listen for K8s cell changes.
 */
@Extension(
        name = "k8s-cellery-cells",
        namespace = "source",
        description = "This is an event source which emits events upon changes to Cellery cells deployed as" +
                "Kubernetes resource",
        examples = {
                @Example(
                        syntax = "@source(type='k8s-cellery-cells', @map(type='keyvalue', " +
                                "fail.on.missing.attribute='false'))\n" +
                                "define stream k8sCellEvents (cell string, creationTimestamp long, " +
                                "action string, Ingress_Types string, component string)",
                        description = "This will listen for kubernetes cell events and emit events upon changes " +
                                "to the pods"
                )
        }
)
public class CellsEventSource extends Source {

    private static final Logger logger = Logger.getLogger(CellsEventSource.class.getName());

    private static final String CELL_CRD_GROUP = "mesh.cellery.io";
    private static final String CELL_CRD_NAME = "cells." + CELL_CRD_GROUP;
    private static final String INGRESS_TYPES = "Ingress_Types";
    private static final String HTTP = "HTTP";
    private static final String WEB = "WEB";
    private static final String VERSION = "/v1alpha1";
    private static final String DEFAULT_NAMESPACE = "default";
    private static final String CELL = "Cell";
    private KubernetesClient k8sClient;
    private SourceEventListener sourceEventListener;

    @Override
    public void init(SourceEventListener sourceEventListener, OptionHolder optionHolder, String[] strings,
                     ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        this.sourceEventListener = sourceEventListener;
    }

    @Override
    public Class[] getOutputEventClasses() {
        return new Class[0];
    }

    @Override
    public void connect(ConnectionCallback connectionCallback) throws ConnectionUnavailableException {
        // Initializing the K8s API Client
        k8sClient = K8sClientHolder.getK8sClient();
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved API server client instance");
        }

        // Get CRD for cell resource
        CustomResourceDefinitionList crdList = k8sClient.customResourceDefinitions().list();
        List<CustomResourceDefinition> crdsItems = crdList.getItems();
        CustomResourceDefinition cellCRD = null;
        for (CustomResourceDefinition crd : crdsItems) {
            ObjectMeta metadata = crd.getMetadata();
            if (metadata != null) {
                String name = metadata.getName();
                if (CELL_CRD_NAME.equals(name)) {
                    cellCRD = crd;
                }
            }
        }
        KubernetesDeserializer.registerCustomKind(CELL_CRD_GROUP + VERSION, CELL, Cell.class);

        // Create client for cell resource
        NonNamespaceOperation<Cell, CellList, DoneableCell, Resource<Cell, DoneableCell>> cellClient =
                k8sClient.customResources(cellCRD, Cell.class, CellList.class, DoneableCell.class)
                        .inNamespace(DEFAULT_NAMESPACE);

        // Cell watcher for Cellery cell updates
        cellClient.watch(new Watcher<Cell>() {
            @Override
            public void eventReceived(Action action, Cell cell) {
                List<Http> httpObjectsList = cell.getSpec().getGatewayTemplate().getSpec().getHttp();
                try {
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put(Constants.ATTRIBUTE_CELL, cell.getMetadata().getName());
                    attributes.put(Constants.ATTRIBUTE_CREATION_TIMESTAMP,
                            cell.getMetadata().getCreationTimestamp() == null
                                    ? -1
                                    : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                    cell.getMetadata().getCreationTimestamp()).getTime());
                    attributes.put(Constants.ATTRIBUTE_ACTION, action.toString());

//                    for (ServicesTemplate data : cell.getSpec().getServicesTemplates()) {
//                        attributes.put("component", data.getMetadata().getName());
//                        sourceEventListener.onEvent(attributes, new String[0]);
//                    }

                    // Checks and add HTTP as Ingress type
                    addHTTPTypes(httpObjectsList, sourceEventListener, attributes);
                    // Checks and add HTTP as Ingress type
                    addWEBTypes(httpObjectsList, sourceEventListener, attributes, cell);

                } catch (ParseException e) {
                    logger.error("Ignored cell change due to creation timestamp parse failure", e);
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
                if (cause == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Kubernetes cell watcher closed successfully");
                    }
                } else {
                    logger.error("Kubernetes cell watcher closed with error", cause);
                }
            }
        });
        if (logger.isDebugEnabled()) {
            logger.debug("Created cell watcher");
        }
    }

    @Override
    public void disconnect() {
        if (k8sClient != null) {
            k8sClient.close();
            if (logger.isDebugEnabled()) {
                logger.debug("Closed API server client");
            }
        }
    }

    @Override
    public void destroy() {
        // Do nothing
    }

    @Override
    public void pause() {
        // Do nothing
    }

    @Override
    public void resume() {
        // Do nothing
    }

    @Override
    public Map<String, Object> currentState() {
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> map) {
        // Do nothing
    }

    /**
     * This method checks if the ingress type is of HTTP type and adds it to the relevant cell data
     */
    public void addHTTPTypes(List<Http> httpObjectsList, SourceEventListener sourceEventListener,
                             Map<String, Object> attributes) {
        if (httpObjectsList != null) {
            for (Http httpObj : httpObjectsList) {
                if (httpObj.getBackend() != null && !httpObj.getBackend().isEmpty()) {
                    attributes.put(INGRESS_TYPES, HTTP);
                    attributes.put(Constants.ATTRIBUTE_COMPONENT, httpObj.getBackend());
                    sourceEventListener.onEvent(attributes, new String[0]);
                }
            }
        }
    }

    /**
     * This method checks if the ingress type is of WEB type and adds it to the relevant cell data
     */
    public void addWEBTypes(List<Http> httpObjectsList, SourceEventListener sourceEventListener,
                            Map<String, Object> attributes, Cell cell) {
        if (cell.getSpec().getGatewayTemplate().getSpec().getHost() != null) {
            attributes.put(INGRESS_TYPES, WEB);
            attributes.put(Constants.ATTRIBUTE_COMPONENT, httpObjectsList.get(0).getBackend());
            sourceEventListener.onEvent(attributes, new String[0]);
        }
    }
}
