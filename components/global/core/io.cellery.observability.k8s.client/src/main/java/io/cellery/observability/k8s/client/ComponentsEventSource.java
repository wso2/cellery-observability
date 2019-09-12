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

import io.cellery.observability.k8s.client.crds.CellImpl;
import io.cellery.observability.k8s.client.crds.CellList;
import io.cellery.observability.k8s.client.crds.DoneableCell;
import io.cellery.observability.k8s.client.crds.model.GRPC;
import io.cellery.observability.k8s.client.crds.model.HTTP;
import io.cellery.observability.k8s.client.crds.model.ServicesTemplate;
import io.cellery.observability.k8s.client.crds.model.TCP;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
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

    private Watch cellWatch;
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
        CustomResourceDefinition cellCrd = k8sClient.customResourceDefinitions()
                .withName(Constants.CELL_CRD_NAME)
                .get();

        // Cell watch for Cellery cell updates
        cellWatch = k8sClient.customResources(cellCrd, CellImpl.class, CellList.class, DoneableCell.class)
                .inNamespace(Constants.NAMESPACE)
                .watch(new Watcher<CellImpl>() {
                    @Override
                    public void eventReceived(Action action, CellImpl cell) {
                        List<HTTP> httpObjectsList = cell.getSpec().getGatewayTemplate().getSpec().getHttp();
                        List<TCP> tcpObjectsList = cell.getSpec().getGatewayTemplate().getSpec().getTcp();
                        List<GRPC> grpcObjectList = cell.getSpec().getGatewayTemplate().getSpec().getGrpc();

                        try {
                            Map<String, Object> attributes = new HashMap<>();
                            attributes.put(Constants.Attribute.CELL, cell.getMetadata().getName());
                            attributes.put(Constants.Attribute.CREATION_TIMESTAMP,
                                    StringUtils.isEmpty(cell.getMetadata().getCreationTimestamp())
                                            ? -1
                                            : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                                    cell.getMetadata().getCreationTimestamp()).getTime());
                            attributes.put(Constants.Attribute.ACTION, action.toString());

                            addComponentsInfo(cell, httpObjectsList, tcpObjectsList, grpcObjectList, attributes);
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
        if (cellWatch != null) {
            cellWatch.close();
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
            boolean isHttp = httpObjectsList.stream().anyMatch(httpObj -> httpObj.getBackend().equals(componentName));
            // Check for Web ingresses
            if (isHttp && isWebCell) {
                ingressTypes.add(Constants.IngressType.WEB);
            } else if (isHttp) {
                ingressTypes.add(Constants.IngressType.HTTP);
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
                ingressTypes.add(Constants.IngressType.TCP);
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
                ingressTypes.add(Constants.IngressType.GRPC);
            }
        }
    }

    /**
     * This method returns the components inside the specified cell.
     */
    private Set<String> getComponentsList(CellImpl cell) {
        Set<String> componentsList = new HashSet<>();
        List<ServicesTemplate> objectMetas = cell.getSpec().getServicesTemplates();
        for (ServicesTemplate service : objectMetas) {
            componentsList.add(service.getMetadata().getName());
        }
        return componentsList;
    }

    /**
     * This method maps the respective components with the Ingress types it exposes.
     */
    private void addComponentsInfo(CellImpl cell, List<HTTP> httpObjectsList, List<TCP> tcpObjectsList,
                                   List<GRPC> grpcObjectList, Map<String, Object> attributes) {
        for (String componentName : getComponentsList(cell)) {
            List<String> ingressTypesList = new ArrayList<>();
            boolean isWebCell = StringUtils.isNotEmpty(cell.getSpec().getGatewayTemplate().getSpec().getHost());
            addHttpType(httpObjectsList, componentName, ingressTypesList, isWebCell);
            addTcpType(tcpObjectsList, componentName, ingressTypesList);
            addGrpcType(grpcObjectList, componentName, ingressTypesList);

            attributes.put(Constants.Attribute.COMPONENT, componentName);
            attributes.put(Constants.Attribute.INGRESS_TYPES,  StringUtils.join(ingressTypesList, ","));
            sourceEventListener.onEvent(attributes, new String[0]);
        }
    }
}
