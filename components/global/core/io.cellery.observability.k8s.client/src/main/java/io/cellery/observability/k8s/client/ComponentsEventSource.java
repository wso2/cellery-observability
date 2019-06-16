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

package io.cellery.observability.k8s.client;

import io.cellery.observability.k8s.client.cells.Cell;
import io.cellery.observability.k8s.client.cells.CellList;
import io.cellery.observability.k8s.client.cells.DoneableCell;
import io.cellery.observability.k8s.client.cells.model.HTTP;
import io.cellery.observability.k8s.client.cells.model.IngressTypes;
import io.cellery.observability.k8s.client.cells.model.TCP;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.apache.commons.lang3.StringUtils;
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
        name = "k8s-cellery-components",
        namespace = "source",
        description = "This is an event source which emits events upon changes to Cellery cells deployed as" +
                "Kubernetes resource",
        examples = {
                @Example(
                        syntax = "@source(type='k8s-cellery-components', @map(type='keyvalue', " +
                                "fail.on.missing.attribute='false'))\n" +
                                "define stream K8sComponentEventSourceStream (cell string, component string," +
                                " creationTimestamp long, lastKnownActiveTimestamp long, Ingress_Types string" +
                                ", action string)",

                        description = "This will listen for kubernetes cell events and emit events upon changes " +
                                "to the pods"
                )
        }
)
public class ComponentsEventSource extends Source {

    private static final Logger logger = Logger.getLogger(ComponentsEventSource.class.getName());

    private static final String CELL_CRD_GROUP = "mesh.cellery.io";
    private static final String CELL_CRD_NAME = "cells." + CELL_CRD_GROUP;
    private static final String INGRESS_TYPES = "Ingress_Types";
    private static final String HTTP = "HTTP";
    private static final String WEB = "Web";
    private static final String TCP = "TCP";
    private static final String VERSION = "/v1alpha1";
    private static final String DEFAULT_NAMESPACE = "default";
    private static final String CELL = "Cell";
    private static final String LAST_KNOWN_ACTIVE_TIMESTAMP = "lastKnownActiveTimestamp";
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
        // Register the custom resource kind cell to Kubernetes deserializer to perform deserialization of cell objects.
        KubernetesDeserializer.registerCustomKind(CELL_CRD_GROUP + VERSION, CELL, Cell.class);

        // Create client for cell resource
        NonNamespaceOperation<Cell, CellList, DoneableCell, Resource<Cell, DoneableCell>> cellClient =
                k8sClient.customResources(cellCRD, Cell.class, CellList.class, DoneableCell.class)
                        .inNamespace(DEFAULT_NAMESPACE);

        // Cell watcher for Cellery cell updates
        cellClient.watch(new Watcher<Cell>() {
            @Override
            public void eventReceived(Action action, Cell cell) {
                List<HTTP> httpObjectsList = cell.getSpec().getGatewayTemplate().getSpec().getHttp();
                List<TCP> tcpObjectsList = cell.getSpec().getGatewayTemplate().getSpec().getTcp();
                IngressTypes ingressList = new IngressTypes();
                try {
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put(Constants.ATTRIBUTE_CELL, cell.getMetadata().getName());
                    attributes.put(Constants.ATTRIBUTE_CREATION_TIMESTAMP,
                            cell.getMetadata().getCreationTimestamp() == null
                                    ? -1
                                    : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                    cell.getMetadata().getCreationTimestamp()).getTime());
                    attributes.put(Constants.ATTRIBUTE_ACTION, action.toString());
                    attributes.put(LAST_KNOWN_ACTIVE_TIMESTAMP, 0L);

                    // Checks and add HTTP as Ingress type
                    addHTTPTypes(httpObjectsList, sourceEventListener, attributes, ingressList);
                    // Checks and add TCP as Ingress type
                    addTCPTypes(tcpObjectsList, sourceEventListener, attributes, ingressList);
                    // Checks and add HTTP as Ingress type
                    addWEBTypes(httpObjectsList, sourceEventListener, attributes, cell, ingressList);

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
     * This method checks if the ingress type is of HTTP and adds it to the relevant cell data
     */
    public void addHTTPTypes(List<HTTP> httpObjectsList, SourceEventListener sourceEventListener,
                             Map<String, Object> attributes, IngressTypes ingressTypes) {
        if (httpObjectsList != null) {
            for (HTTP httpObj : httpObjectsList) {
                if (httpObj.getBackend() != null && !httpObj.getBackend().isEmpty()) {
                    ingressTypes.addType(HTTP);
                    attributes.put(INGRESS_TYPES, StringUtils.join(ingressTypes.getTypes(), ','));
                    attributes.put(Constants.ATTRIBUTE_COMPONENT, httpObj.getBackend());
                    sourceEventListener.onEvent(attributes, new String[0]);
                }
            }
        }
    }

    /**
     * This method checks if the ingress type is of TCP and adds it to the relevant cell data
     */
    public void addTCPTypes(List<TCP> tcpObjectsList, SourceEventListener sourceEventListener,
                            Map<String, Object> attributes, IngressTypes ingressTypes) {
        if (tcpObjectsList != null) {
            for (TCP tcpObject : tcpObjectsList) {
                if (tcpObject.getBackendHost() != null && !tcpObject.getBackendHost().isEmpty()) {
                    ingressTypes.addType(TCP);
                    attributes.put(INGRESS_TYPES, StringUtils.join(ingressTypes.getTypes(), ','));
                    attributes.put(Constants.ATTRIBUTE_COMPONENT, tcpObject.getBackendHost());
                    sourceEventListener.onEvent(attributes, new String[0]);
                }
            }
        }
    }

    /**
     * This method checks if the ingress type is of WEB type and adds it to the relevant cell data
     */
    public void addWEBTypes(List<HTTP> httpObjectsList, SourceEventListener sourceEventListener,
                            Map<String, Object> attributes, Cell cell, IngressTypes ingressTypes) {
        if (cell.getSpec().getGatewayTemplate().getSpec().getHost() != null) {
            ingressTypes.addType(WEB);
            attributes.put(INGRESS_TYPES, StringUtils.join(ingressTypes.getTypes(), ','));
            attributes.put(Constants.ATTRIBUTE_COMPONENT, httpObjectsList.get(0).getBackend());
            sourceEventListener.onEvent(attributes, new String[0]);
        }
    }
}
