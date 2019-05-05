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
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.apache.log4j.Logger;
import org.powermock.reflect.Whitebox;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Base Test Case for K8s Clients.
 */
public class BaseTestCase {

    private static final Logger logger = Logger.getLogger(BaseTestCase.class.getName());

    protected static final String TEST_LABEL = "mesh-observability-test";
    protected static final int WAIT_TIME = 50;
    protected static final int TIMEOUT = 5000;

    protected static final String NODE_NAME = "node1";
    protected static final String POD_CREATION_TIMESTAMP_STRING = "2019-04-30T13:21:22Z";
    protected final long podCreationTimestamp;

    public BaseTestCase() throws Exception {
        podCreationTimestamp = new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US)
                .parse(POD_CREATION_TIMESTAMP_STRING).getTime();
    }

    protected KubernetesClient k8sClient;
    protected KubernetesServer k8sServer;

    @BeforeMethod
    public void initBaseTestCase() {
        k8sServer = new KubernetesServer(true, false);
        k8sServer.before();
        if (logger.isDebugEnabled()) {
            logger.debug("Started K8s Mock Server");
        }

        k8sClient = k8sServer.getClient();
        if (logger.isDebugEnabled()) {
            logger.debug("Initialized the K8s Client for the K8s Mock Server");
        }
        Whitebox.setInternalState(K8sClientHolder.class, "k8sClient", k8sClient);
    }

    @AfterMethod
    public void cleanupBaseTestCase() {
        k8sClient.close();
        if (logger.isDebugEnabled()) {
            logger.debug("Closed the K8s Client");
        }
        k8sServer.after();
        if (logger.isDebugEnabled()) {
            logger.debug("Closed the K8s Mock Server");
        }
    }

    /**
     * Generate a Cellery Component K8s pod object.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     * @return The generated pod
     */
    protected Pod generateCelleryComponentPod(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.COMPONENT_NAME_LABEL, cell + "--" + component);

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("Running")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Gateway K8s pod object.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell The Cell the Pod belongs to
     * @return The generated pod
     */
    protected Pod generateCelleryGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.GATEWAY_NAME_LABEL, cell + "--gateway");

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("Running")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Component K8s pod object with a failing state.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     * @return The generated pod
     */
    protected Pod generateFailingCelleryComponentPod(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.COMPONENT_NAME_LABEL, cell + "--" + component);

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("ErrImagePull")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Gateway K8s pod object with a failing state.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell      The Cell the Pod belongs to
     * @return The generated pod
     */
    protected Pod generateFailingCelleryGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.GATEWAY_NAME_LABEL, cell + "--gateway");

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("ErrImagePull")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a pod using the provided labels and container with the provided pod name.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param podName   The name of the new pod
     * @param labels    The set of labels to apply
     * @param status    The state
     * @return The generated pod
     */
    private Pod generatePod(String podName, Map<String, String> labels, PodStatus status) {
        labels.put(TEST_LABEL, "true");
        return new PodBuilder()
                .withNewMetadata()
                .withNamespace(Constants.NAMESPACE)
                .withCreationTimestamp(POD_CREATION_TIMESTAMP_STRING)
                .withName(podName)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withNodeName(NODE_NAME)
                .addNewContainer()
                .withName("test-container")
                .withNewImage("busybox")
                .withNewImagePullPolicy("IfNotPresent")
                .withCommand("tail", "-f", "/dev/null")
                .endContainer()
                .endSpec()
                .withStatus(status)
                .build();
    }
}
