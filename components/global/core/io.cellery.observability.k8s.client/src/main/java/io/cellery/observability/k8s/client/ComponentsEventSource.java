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
import io.cellery.observability.k8s.client.cells.model.GRPC;
import io.cellery.observability.k8s.client.cells.model.HTTP;
import io.cellery.observability.k8s.client.cells.model.ServicesTemplate;
import io.cellery.observability.k8s.client.cells.model.TCP;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * This class implements the Event Source which can be used to listen for K8s cell changes.
 */
@Extension(
        name = "k8s-components",
        namespace = "source",
        description = "This is an event source which emits events upon changes to Cellery cells deployed as" +
                "Kubernetes resource",
        examples = {
                @Example(
                        syntax = "@source(type='k8s-components', @map(type='keyvalue', " +
                                "fail.on.missing.attribute='false'))\n" +
                                "define stream K8sComponentEventSourceStream (cell string, component string," +
                                " creationTimestamp long, lastKnownActiveTimestamp long, ingressTypes string" +
                                ", action string)",

                        description = "This will listen for kubernetes cell events and emit events upon changes " +
                                "to the cells"
                )
        }
)
public class ComponentsEventSource extends Source {

    private static final Logger logger = Logger.getLogger(ComponentsEventSource.class.getName());

    private static final String CELL_CRD_GROUP = "mesh.cellery.io";
    private static final String CELL_CRD_NAME = "cells." + CELL_CRD_GROUP;
    private static final String INGRESS_TYPES = "ingressTypes";
    private static final String CELL_CRD_VERSION = "v1alpha1";
    private static final String DEFAULT_NAMESPACE = "default";
    private static final String CELL = "Cell";
    private Watch cellWatcher;
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

        // Register the custom resource kind cell to Kubernetes deserializer to perform deserialization of cell objects.
        KubernetesDeserializer.registerCustomKind(CELL_CRD_GROUP + "/" + CELL_CRD_VERSION, CELL, Cell.class);

        // Create client for cell resource
        MixedOperation<Cell, CellList, DoneableCell, Resource<Cell, DoneableCell>> cellClient =
                k8sClient.customResources(getCellCRD(), Cell.class, CellList.class, DoneableCell.class);

        // Cell watcher for Cellery cell updates
        cellWatcher = cellClient.inNamespace(DEFAULT_NAMESPACE).watch(new Watcher<Cell>() {
            @Override
            public void eventReceived(Action action, Cell cell) {
                List<HTTP> httpObjectsList = cell.getSpec().getGatewayTemplate().getSpec().getHttp();
                List<TCP> tcpObjectsList = cell.getSpec().getGatewayTemplate().getSpec().getTcp();
                List<GRPC> grpcObjectList = cell.getSpec().getGatewayTemplate().getSpec().getGrpc();
                boolean isWebCell = !StringUtils.isEmpty(cell.getSpec().getGatewayTemplate().getSpec().getHost());

                try {
                    Map<String, Object> attributes = new HashMap<>();
                    attributes.put(Constants.ATTRIBUTE_CELL, cell.getMetadata().getName());
                    attributes.put(Constants.ATTRIBUTE_CREATION_TIMESTAMP,
                            cell.getMetadata().getCreationTimestamp() == null
                                    ? -1
                                    : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                    cell.getMetadata().getCreationTimestamp()).getTime());
                    attributes.put(Constants.ATTRIBUTE_ACTION, action.toString());
                    attributes.put(Constants.ATTRIBUTE_LAST_KNOWN_ACTIVE_TIMESTAMP, 0L);

                    addComponentsInfo(cell, httpObjectsList, tcpObjectsList, grpcObjectList, attributes, isWebCell);

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
        if (cellWatcher != null) {
            cellWatcher.close();
            if (logger.isDebugEnabled()) {
                logger.debug("Closed cell watcher");
            }
        }
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
    private void addHttpType(List<HTTP> httpObjectsList, String componentName,
                             List<String> ingressTypes, boolean isWebCell) {
        if (httpObjectsList != null) {
            boolean isHttp = httpObjectsList.stream().anyMatch(httpObj -> httpObj.getBackend()
                    .equals(componentName));
            // Check for Web ingresses
            if (isHttp && isWebCell) {
                ingressTypes.add(Constants.INGRESS_TYPE_WEB);
            } else if (isHttp) {
                ingressTypes.add(Constants.INGRESS_TYPE_HTTP);
            }
        }
    }

    /**
     * This method checks if the ingress type is of TCP type and adds it to the relevant cell data
     */
    private void addTcpType(List<TCP> tcpObjectsList, String componentName, List<String> ingressTypes) {
        if (tcpObjectsList != null) {
            if (tcpObjectsList.stream().anyMatch(tcpObject -> tcpObject.getBackendHost()
                    .equals(componentName))) {
                ingressTypes.add(Constants.INGRESS_TYPE_TCP);
            }
        }
    }

    /**
     * This method checks if the ingress type is of GRPC type and adds it to the relevant cell data
     */
    private void addGrpcType(List<GRPC> grpcObjectsList, String componentName, List<String> ingressTypes) {
        if (grpcObjectsList != null) {
            if (grpcObjectsList.stream().anyMatch(grpcObject -> grpcObject.getBackendHost()
                    .equals(componentName))) {
                ingressTypes.add(Constants.INGRESS_TYPE_GRPC);
            }
        }
    }

    /**
     * This method returns the components inside the specified cell.
     */
    private Set<String> getComponentsList(Cell cell) {
        Set<String> componentsList = new HashSet<String>();
        List<ServicesTemplate> objectMetas = cell.getSpec().getServicesTemplates();
        for (ServicesTemplate service : objectMetas) {
            componentsList.add(service.getMetadata().getName());
        }
        return componentsList;
    }

    /**
     * This method returns the cell CRD.
     */
    private CustomResourceDefinition getCellCRD() {
        // Get CRD for cell resource
        CustomResourceDefinitionList crdList = k8sClient.customResourceDefinitions().list();
        List<CustomResourceDefinition> crdsItems = crdList.getItems();
        for (CustomResourceDefinition crd : crdsItems) {
            ObjectMeta metadata = crd.getMetadata();
            if (metadata != null) {
                if (CELL_CRD_NAME.equalsIgnoreCase(metadata.getName())) {
                    return crd;
                }
            }
        }
        return null;
    }

    /**
     * This method maps the respective components with the Ingress types it exposes.
     */
    private void addComponentsInfo(Cell cell, List<HTTP> httpObjectsList, List<TCP> tcpObjectsList,
                                   List<GRPC> grpcObjectList, Map<String, Object> attributes, boolean isWebCell) {
        for (String componentName : getComponentsList(cell)) {
            List<String> ingressTypesList = new ArrayList<>();
            addHttpType(httpObjectsList, componentName, ingressTypesList, isWebCell);
            addTcpType(tcpObjectsList, componentName, ingressTypesList);
            addGrpcType(grpcObjectList, componentName, ingressTypesList);
            attributes.put(Constants.ATTRIBUTE_COMPONENT, componentName);
            attributes.put(INGRESS_TYPES, StringUtils.join(ingressTypesList, ','));
            sourceEventListener.onEvent(attributes, new String[0]);
        }
    }
}
