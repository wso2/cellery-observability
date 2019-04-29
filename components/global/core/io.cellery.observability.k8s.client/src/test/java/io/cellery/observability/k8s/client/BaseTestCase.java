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
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base Test Case for K8s Clients.
 */
public class BaseTestCase {

    private static final Logger logger = Logger.getLogger(BaseTestCase.class.getName());

    protected static final String TEST_LABEL = "mesh-observability-test";
    protected static final int WAIT_TIME = 50;
    protected static final int TIMEOUT = 5000;
    protected static final String NODE_NAME = "node1";


    protected KubernetesClient k8sClient;
    protected KubernetesServer k8sServer;

    @BeforeClass
    public void initBaseTestCase() {
        k8sServer = new KubernetesServer(true, true);
        k8sServer.before();
        k8sClient = k8sServer.getClient();
        k8sClient.getConfiguration().setNamespace(Constants.NAMESPACE);
        k8sClient.namespaces().list();     // To validate if the access to the K8s cluster is accurate
        K8sClientHolder.setK8sClient(k8sClient);
        k8sClient.nodes().createNew().withNewMetadata().withName(NODE_NAME).endMetadata().done();
    }

    @AfterClass
    public void cleanupTestCase() {
        cleanUpTestPods();
        k8sClient.close();
        k8sServer.after();
    }

    /**
     * Create and check for a K8s pod creation.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     */
    protected void createCelleryComponentPod(String cell, String component) throws Exception {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.COMPONENT_NAME_LABEL, cell + "--" + component);

        createPod(podName, labels, "busybox");
        checkPodCreation(podName);
    }

    /**
     * Create and check for a K8s pod creation.
     *
     * @param cell The Cell the Pod belongs to
     */
    protected void createCelleryGatewayPod(String cell) throws Exception {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.GATEWAY_NAME_LABEL, cell + "--gateway");

        createPod(podName, labels, "busybox");
        checkPodCreation(podName);
    }

    /**
     * Create and check for a K8s pod creation.
     *
     * @param podName The name of the normal pod
     */
    protected void createNormalPod(String podName) throws Exception {
        createPod(podName, new HashMap<>(), "busybox");
        checkPodCreation(podName);
    }

    /**
     * Create a cellery component pod that would fail.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     */
    protected void createFailingCelleryComponentPod(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.COMPONENT_NAME_LABEL, cell + "--" + component);

        createPod(podName, labels, "non-existent-container");
    }

    /**
     * Create a cellery gateway pod that would fail.
     *
     * @param cell      The Cell the Pod belongs to
     */
    protected void createFailingCelleryGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.GATEWAY_NAME_LABEL, cell + "--gateway");

        createPod(podName, labels, "non-existent-container");
    }

    /**
     * Create a pod that would fail.
     *
     * @param podName The name of the pod to create
     */
    protected void createFailingNormalPod(String podName) {
        createPod(podName, new HashMap<>(), "non-existent-container");
    }

    /**
     * Delete and wait for K8s pod to be removed.
     *
     * @param podName The name of the pod to be deleted
     */
    protected void deletePod(String podName) {
        k8sClient.pods()
                .inNamespace(Constants.NAMESPACE)
                .withName(podName)
                .delete();
        waitForPodRemove(podName);
    }

    /**
     * Removes all the test pods created.
     */
    protected void cleanUpTestPods() {
        List<String> podNames = k8sClient.pods()
                .withLabel(TEST_LABEL)
                .list()
                .getItems()
                .stream()
                .map((pod) -> pod.getMetadata().getName())
                .collect(Collectors.toList());
        k8sClient.pods()
                .withLabel(TEST_LABEL)
                .delete();
        for (String podName : podNames) {
            waitForPodRemove(podName);
        }
    }

    /**
     * Create a pod using the provided labels and container with the provided pod name.
     *
     * @param podName   The name of the new pod
     * @param labels    The set of labels to apply
     * @param container The container to use
     */
    private void createPod(String podName, Map<String, String> labels, String container) {
        labels.put(TEST_LABEL, "true");
        k8sClient.pods()
                .createNew()
                .withNewMetadata()
                .withNamespace(Constants.NAMESPACE)
                .withCreationTimestamp("2019-04-30T13:21:22Z")
                .withName(podName)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withNodeName(NODE_NAME)
                .addNewContainer()
                .withName("test-container")
                .withNewImage(container)
                .withNewImagePullPolicy("IfNotPresent")
                .withCommand("tail", "-f", "/dev/null")
                .endContainer()
                .endSpec()
                .done();
    }

    /**
     * Check for a pod creation.
     *
     * @param podName The name of the pod to wait for
     */
    private void checkPodCreation(String podName) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Checking pod " + podName);
        }

        Pod createdPod = k8sClient.pods()
                .inNamespace(Constants.NAMESPACE)
                .withName(podName)
                .get();
        if (createdPod == null || !createdPod.getMetadata().getName().equalsIgnoreCase(podName)) {
            throw new Exception("Pod :" + podName + " is not created!");
        }
    }

    /**
     * Wait for a pod to be removed.
     *
     * @param podName The name of the pod to be deleted
     */
    private void waitForPodRemove(String podName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Waiting for pod " + podName + " to be removed");
        }
        while (true) {
            Pod pod = k8sClient.pods()
                    .inNamespace(Constants.NAMESPACE)
                    .withName(podName)
                    .get();
            if (pod == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removed pod " + podName);
                }
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
