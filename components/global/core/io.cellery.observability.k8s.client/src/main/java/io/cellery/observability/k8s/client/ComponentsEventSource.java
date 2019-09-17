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

import io.cellery.observability.k8s.client.crds.cell.CellImpl;
import io.cellery.observability.k8s.client.crds.cell.CellList;
import io.cellery.observability.k8s.client.crds.cell.DoneableCell;
import io.cellery.observability.k8s.client.crds.composite.CompositeImpl;
import io.cellery.observability.k8s.client.crds.composite.CompositeList;
import io.cellery.observability.k8s.client.crds.composite.DoneableComposite;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class implements the Event Source which can be used to listen for K8s component changes.
 */
@Extension(
        name = "k8s-components",
        namespace = "source",
        description = "This is an event source which emits events upon changes to Cellery components deployed as" +
                "Kubernetes resource",
        examples = {
                @Example(
                        syntax = "@source(type='k8s-components', @map(type='keyvalue', " +
                                "fail.on.missing.attribute='false'))\n" +
                                "define stream K8sComponentEventSourceStream (instance string, kind string, " +
                                "component string, creationTimestamp long, ingressTypes string, action string)",
                        description = "This will listen for kubernetes component events and emit events upon changes " +
                                "to the components"
                )
        }
)
public class ComponentsEventSource extends Source {

    private static final Logger logger = Logger.getLogger(ComponentsEventSource.class.getName());

    private KubernetesClient k8sClient;
    private SourceEventListener sourceEventListener;
    private List<Watch> k8sWatches;

    @Override
    public void init(SourceEventListener sourceEventListener, OptionHolder optionHolder, String[] strings,
                     ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        this.k8sWatches = new ArrayList<>(2);
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
        CustomResourceDefinition compositeCrd = k8sClient.customResourceDefinitions()
                .withName(Constants.COMPOSITE_CRD_NAME)
                .get();

        // Cell watch for Cellery cell updates
        Watch cellWatch = k8sClient.customResources(cellCrd, CellImpl.class, CellList.class, DoneableCell.class)
                .inNamespace(Constants.NAMESPACE)
                .watch(new Watcher<CellImpl>() {
                    @Override
                    public void eventReceived(Action action, CellImpl cell) {
                        try {
                            long creationTimestamp = StringUtils.isEmpty(cell.getMetadata().getCreationTimestamp())
                                    ? -1
                                    : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                            cell.getMetadata().getCreationTimestamp()).getTime();
                            Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(cell);

                            for (Map.Entry<String, List<String>> entry : componentIngressTypes.entrySet()) {
                                Map<String, Object> attributes = new HashMap<>();
                                attributes.put(Constants.Attribute.INSTANCE, cell.getMetadata().getName());
                                attributes.put(Constants.Attribute.KIND, Constants.CELL_KIND);
                                attributes.put(Constants.Attribute.COMPONENT, entry.getKey());
                                attributes.put(Constants.Attribute.CREATION_TIMESTAMP, creationTimestamp);
                                attributes.put(Constants.Attribute.INGRESS_TYPES,
                                        StringUtils.join(entry.getValue(), ","));
                                attributes.put(Constants.Attribute.ACTION, action.toString());
                                sourceEventListener.onEvent(attributes, new String[0]);
                            }
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
        k8sWatches.add(cellWatch);
        if (logger.isDebugEnabled()) {
            logger.debug("Created cell watcher");
        }
        // Component watch for Cellery component updates
        Watch compositeWatch = k8sClient.customResources(compositeCrd, CompositeImpl.class, CompositeList.class,
                DoneableComposite.class)
                .inNamespace(Constants.NAMESPACE)
                .watch(new Watcher<CompositeImpl>() {
                    @Override
                    public void eventReceived(Action action, CompositeImpl composite) {
                        try {
                            long creationTimestamp = StringUtils.isEmpty(composite.getMetadata().getCreationTimestamp())
                                    ? -1
                                    : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                    composite.getMetadata().getCreationTimestamp()).getTime();
                            Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(composite);

                            for (Map.Entry<String, List<String>> entry : componentIngressTypes.entrySet()) {
                                Map<String, Object> attributes = new HashMap<>();
                                attributes.put(Constants.Attribute.INSTANCE, composite.getMetadata().getName());
                                attributes.put(Constants.Attribute.KIND, Constants.COMPOSITE_KIND);
                                attributes.put(Constants.Attribute.COMPONENT, entry.getKey());
                                attributes.put(Constants.Attribute.CREATION_TIMESTAMP, creationTimestamp);
                                attributes.put(Constants.Attribute.INGRESS_TYPES,
                                        StringUtils.join(entry.getValue(), ","));
                                attributes.put(Constants.Attribute.ACTION, action.toString());
                                sourceEventListener.onEvent(attributes, new String[0]);
                            }
                        } catch (ParseException e) {
                            logger.error("Ignored composite change due to creation timestamp parse failure", e);
                        }
                    }

                    @Override
                    public void onClose(KubernetesClientException cause) {
                        if (cause == null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Kubernetes composite watcher closed successfully");
                            }
                        } else {
                            logger.error("Kubernetes composite watcher closed with error", cause);
                        }
                    }
                });
        k8sWatches.add(compositeWatch);
        if (logger.isDebugEnabled()) {
            logger.debug("Created composite watcher");
        }
    }

    @Override
    public void disconnect() {
        while (k8sWatches.size() > 0) {
            Watch watch = k8sWatches.remove(0);
            watch.close();
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
}
