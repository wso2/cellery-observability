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
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.log4j.Logger;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class implements the Stream Processor which can be used to call the K8s API Server and get data about Cellery
 * Component pods deployed in a namespace.
 */
@Extension(
        name = "getComponentPods",
        namespace = "k8sClient",
        description = "This is a client which calls the Kubernetes API server based on the received parameters and " +
                "adds the pod details received. Each pod will be a separate event duplicated from the original event" +
                "sent to this stream processor. If m number of multiple events are sent while n pods are present," +
                "m x n events will be sent out. This read the Service Account Token loaded into the pod and calls " +
                "the API Server using that.",
        examples = {
                @Example(
                        syntax = "k8sClient:getComponentPods()",
                        description = "This will fetch the currently running pods from the K8s API Servers"
                )
        }
)
public class GetComponentPodsStreamProcessor extends StreamProcessor {

    private static final Logger logger = Logger.getLogger(GetComponentPodsStreamProcessor.class.getName());
    private KubernetesClient k8sClient;

    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition,
                                   ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                                   SiddhiAppContext siddhiAppContext) {
        int attributeLength = attributeExpressionExecutors.length;
        if (attributeLength != 0) {
            throw new SiddhiAppValidationException("k8sClient:getComponentPods() expects exactly zero input " +
                    "parameters, but " + attributeExpressionExecutors.length + " attributes found");
        }

        List<Attribute> appendedAttributes = new ArrayList<>();
        appendedAttributes.add(new Attribute(Constants.Attribute.INSTANCE, Attribute.Type.STRING));
        appendedAttributes.add(new Attribute(Constants.Attribute.COMPONENT, Attribute.Type.STRING));
        appendedAttributes.add(new Attribute(Constants.Attribute.POD_NAME, Attribute.Type.STRING));
        appendedAttributes.add(new Attribute(Constants.Attribute.INSTANCE_KIND, Attribute.Type.STRING));
        appendedAttributes.add(new Attribute(Constants.Attribute.CREATION_TIMESTAMP, Attribute.Type.LONG));
        appendedAttributes.add(new Attribute(Constants.Attribute.NODE_NAME, Attribute.Type.STRING));
        return appendedAttributes;
    }

    @Override
    public void start() {
        k8sClient = K8sClientHolder.getK8sClient();
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved API server client instance");
        }
    }

    @Override
    public void stop() {
        if (k8sClient != null) {
            k8sClient.close();
            if (logger.isDebugEnabled()) {
                logger.debug("Closed API server client");
            }
        }
    }

    @Override
    public Map<String, Object> currentState() {
        // No State
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        // Do Nothing
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        ComplexEventChunk<StreamEvent> outputStreamEventChunk = new ComplexEventChunk<>(true);
        while (streamEventChunk.hasNext()) {
            StreamEvent incomingStreamEvent = streamEventChunk.next();
            try {
                addComponentPods(outputStreamEventChunk, incomingStreamEvent, Constants.CELL_NAME_LABEL,
                        Constants.COMPONENT_NAME_LABEL);
                addComponentPods(outputStreamEventChunk, incomingStreamEvent, Constants.CELL_NAME_LABEL,
                        Constants.GATEWAY_NAME_LABEL);
                addComponentPods(outputStreamEventChunk, incomingStreamEvent, Constants.COMPOSITE_NAME_LABEL,
                        Constants.COMPONENT_NAME_LABEL);
            } catch (ParseException e) {
                // This should not happen unless the K8s date-time format changed (eg:- K8s version upgrade)
                logger.error("Failed to parse K8s timestamp", e);
            }
        }
        if (outputStreamEventChunk.getFirst() != null) {
            nextProcessor.process(outputStreamEventChunk);
        }
    }

    /**
     * Add component pods to output stream event chunk.
     * A new event will be cloned from the incoming stream event for each pod and added to the output event chunk.
     *
     * @param outputStreamEventChunk The output stream event chunk which will be sent to the next processor
     * @param incomingStreamEvent    The incoming stream event which will be cloned and used
     * @param instanceNameLabel      The name of the label applied to store the instance name
     * @param componentNameLabel     The name of the label applied to store the component/gateway name
     */
    private void addComponentPods(ComplexEventChunk<StreamEvent> outputStreamEventChunk,
                                  StreamEvent incomingStreamEvent, String instanceNameLabel,
                                  String componentNameLabel) throws ParseException {
        // Calling the K8s API Servers to fetch component pods
        PodList componentPodList = null;
        try {
            componentPodList = k8sClient.pods()
                    .inNamespace(Constants.NAMESPACE)
                    .withLabel(instanceNameLabel)
                    .withLabel(componentNameLabel)
                    .withField(Constants.STATUS_FIELD, Constants.STATUS_FIELD_RUNNING_VALUE)
                    .list();
        } catch (Throwable e) {
            logger.error("Failed to fetch current pods for components", e);
        }
        String kind;
        if (Constants.CELL_NAME_LABEL.equals(instanceNameLabel)) {
            kind = Constants.CELL_KIND;
        } else if (Constants.COMPOSITE_NAME_LABEL.equals(instanceNameLabel)) {
            kind = Constants.COMPOSITE_KIND;
        } else {
            kind = "";
        }

        if (componentPodList != null) {
            for (Pod pod : componentPodList.getItems()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Added event - pod " + pod.getMetadata().getName() + " belonging to " + kind
                            + " " + pod.getMetadata().getLabels().get(instanceNameLabel) + " of type " +
                            (Constants.COMPONENT_NAME_LABEL.equals(componentNameLabel) ? "component" : "gateway") +
                            " to the event");
                }

                Object[] newData = new Object[6];
                newData[0] = pod.getMetadata().getLabels().getOrDefault(instanceNameLabel, "");
                newData[1] = Utils.getComponentName(pod);
                newData[2] = pod.getMetadata().getName();
                newData[3] = kind;
                newData[4] = new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US)
                        .parse(pod.getMetadata().getCreationTimestamp()).getTime();
                newData[5] = pod.getSpec().getNodeName();

                StreamEvent streamEventCopy = streamEventCloner.copyStreamEvent(incomingStreamEvent);
                complexEventPopulater.populateComplexEvent(streamEventCopy, newData);
                outputStreamEventChunk.add(streamEventCopy);
            }
        }
    }
}
