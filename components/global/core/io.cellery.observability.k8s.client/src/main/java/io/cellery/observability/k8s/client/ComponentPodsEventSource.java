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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.log4j.Logger;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.core.config.SiddhiAppContext;
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
 * This class implements the Event Source which can be used to listen for k8s pod changes.
 */
@Extension(
        name = "k8s-component-pods",
        namespace = "source",
        description = "This is an event source which emits events upon changes to Cellery Components deployed as" +
                "Kubernetes Pods",
        examples = {
                @Example(
                        syntax = "@source(type='k8s-component-pods', @map(type='keyvalue', " +
                                "fail.on.missing.attribute='false'))\n" +
                                "define stream K8sPodEvents (instance string, kind string, component string, " +
                                "podName string, creationTimestamp long, deletionTimestamp long, nodeName string, " +
                                "status string, action string)",
                        description = "This will listen for kubernetes pod events and emit events upon changes " +
                                "to the pods"
                )
        }
)
public class ComponentPodsEventSource extends Source {

    private static final Logger logger = Logger.getLogger(ComponentPodsEventSource.class.getName());

    private KubernetesClient k8sClient;
    private SourceEventListener sourceEventListener;
    private List<Watch> k8sWatches;

    @Override
    public void init(SourceEventListener sourceEventListener, OptionHolder optionHolder, String[] strings,
                     ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        this.k8sWatches = new ArrayList<>(3);
        this.sourceEventListener = sourceEventListener;
    }

    @Override
    public void connect(ConnectionCallback connectionCallback) {
        // Initializing the K8s API Client
        k8sClient = K8sClientHolder.getK8sClient();
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved API server client instance");
        }

        // Pod watcher for Cellery Cell components
        Watch cellComponentsWatch = k8sClient.pods()
                .inNamespace(Constants.NAMESPACE)
                .withLabel(Constants.CELL_NAME_LABEL)
                .withLabel(Constants.COMPONENT_NAME_LABEL)
                .watch(new PodWatcher(this.sourceEventListener, Constants.COMPONENT_NAME_LABEL));
        k8sWatches.add(cellComponentsWatch);
        if (logger.isDebugEnabled()) {
            logger.debug("Created pod watcher for Cell components");
        }
        // Pod watcher for Cellery Composite components
        Watch compositeComponentsWatch = k8sClient.pods()
                .inNamespace(Constants.NAMESPACE)
                .withLabel(Constants.COMPOSITE_NAME_LABEL)
                .withLabel(Constants.COMPONENT_NAME_LABEL)
                .watch(new PodWatcher(this.sourceEventListener, Constants.COMPONENT_NAME_LABEL));
        k8sWatches.add(compositeComponentsWatch);
        if (logger.isDebugEnabled()) {
            logger.debug("Created pod watcher for Composite components");
        }
        // Pod watcher for Cellery cell gateways
        Watch gatewaysWatch = k8sClient.pods()
                .inNamespace(Constants.NAMESPACE)
                .withLabel(Constants.CELL_NAME_LABEL)
                .withLabel(Constants.GATEWAY_NAME_LABEL)
                .watch(new PodWatcher(this.sourceEventListener, Constants.GATEWAY_NAME_LABEL));
        k8sWatches.add(gatewaysWatch);
        if (logger.isDebugEnabled()) {
            logger.debug("Created pod watcher for gateways");
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
        // Do nothing
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        // Do nothing
    }

    @Override
    public Class[] getOutputEventClasses() {
        return new Class[]{Map.class};
    }

    /**
     * Pod watcher which can listen for pod changes and create events based on them.
     */
    private static class PodWatcher implements Watcher<Pod> {

        private static final Logger logger = Logger.getLogger(PodWatcher.class.getName());
        private final SourceEventListener sourceEventListener;
        private final String componentNameLabel;

        PodWatcher(SourceEventListener sourceEventListener, String componentNameLabel) {
            this.sourceEventListener = sourceEventListener;
            this.componentNameLabel = componentNameLabel;
        }

        @Override
        public void eventReceived(Action action, Pod pod) {
            try {
                String instance;
                String kind;
                if (pod.getMetadata().getLabels().containsKey(Constants.CELL_NAME_LABEL)) {
                    instance = pod.getMetadata().getLabels().get(Constants.CELL_NAME_LABEL);
                    kind = Constants.CELL_KIND;
                } else if (pod.getMetadata().getLabels().containsKey(Constants.COMPOSITE_NAME_LABEL)) {
                    instance = pod.getMetadata().getLabels().get(Constants.COMPOSITE_NAME_LABEL);
                    kind = Constants.COMPOSITE_KIND;
                } else {
                    instance = "";
                    kind = "";
                }
                Map<String, Object> attributes = new HashMap<>();
                attributes.put(Constants.Attribute.INSTANCE, instance);
                attributes.put(Constants.Attribute.COMPONENT, Utils.getComponentName(pod));
                attributes.put(Constants.Attribute.POD_NAME, pod.getMetadata().getName());
                attributes.put(Constants.Attribute.INSTANCE_KIND, kind);
                attributes.put(Constants.Attribute.CREATION_TIMESTAMP, pod.getMetadata().getCreationTimestamp() == null
                        ? -1
                        : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                pod.getMetadata().getCreationTimestamp()).getTime());
                attributes.put(Constants.Attribute.DELETION_TIMESTAMP, pod.getMetadata().getDeletionTimestamp() == null
                        ? -1
                        : new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US).parse(
                                pod.getMetadata().getDeletionTimestamp()).getTime());
                attributes.put(Constants.Attribute.NODE_NAME,
                        pod.getSpec().getNodeName() == null ? "" : pod.getSpec().getNodeName());
                attributes.put(Constants.Attribute.STATUS, pod.getStatus().getPhase());
                attributes.put(Constants.Attribute.ACTION, action.toString());

                sourceEventListener.onEvent(attributes, new String[0]);
                if (logger.isDebugEnabled()) {
                    logger.debug("Emitted event - pod " + pod.getMetadata().getName() + " with resource version " +
                            pod.getMetadata().getCreationTimestamp() + " belonging to cell " +
                            pod.getMetadata().getLabels().get(Constants.CELL_NAME_LABEL) + " of type " +
                            (Constants.COMPONENT_NAME_LABEL.equals(componentNameLabel) ? "component" : "gateway"));
                }
            } catch (ParseException e) {
                // This should not happen unless the K8s date-time format changed (eg:- K8s version upgrade)
                logger.error("Ignored pod change due to creation timestamp parse failure", e);
            }
        }

        @Override
        public void onClose(KubernetesClientException cause) {
            if (cause == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Kubernetes " +
                            (Constants.COMPONENT_NAME_LABEL.equals(componentNameLabel) ? "component" : "gateway") +
                            " pod watcher closed successfully");
                }
            } else {
                logger.error("Kubernetes " +
                        (Constants.COMPONENT_NAME_LABEL.equals(componentNameLabel) ? "component" : "gateway") +
                        " pod watcher closed with error", cause);
            }
        }
    }
}
