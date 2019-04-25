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
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.util.HashMap;
import java.util.Map;

/**
 * Base Test Case for K8s Clients.
 */
public class BaseTestCase {
    private static final Logger logger = Logger.getLogger(BaseTestCase.class.getName());

    protected static final String TEST_LABEL = "mesh-observability-test";
    protected static final int WAIT_TIME = 50;
    protected static final int TIMEOUT = 5000;

    protected KubernetesClient k8sClient;

    @BeforeClass
    public void initBaseTestCase() {
        k8sClient = new DefaultKubernetesClient();
        k8sClient.namespaces().list();     // To validate if the access to the K8s cluster is accurate
    }

    @AfterClass
    public void cleanupTestCase() {
        k8sClient.close();
    }

    /**
     * Create and wait for a K8s pod to start.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     */
    protected void createCelleryComponent(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.COMPONENT_NAME_LABEL, podName);

        createPod(podName, labels, "busybox");
        waitForPodStartup(podName);
    }

    /**
     * Create and wait for a K8s pod to start.
     *
     * @param cell The Cell the Pod belongs to
     */
    protected void createCelleryGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.GATEWAY_NAME_LABEL, podName);

        createPod(podName, labels, "busybox");
        waitForPodStartup(podName);
    }

    /**
     * Create and wait for a K8s pod to start.
     *
     * @param podName The name of the normal pod
     */
    protected void createNormalPod(String podName) {
        createPod(podName, new HashMap<>(), "busybox");
        waitForPodStartup(podName);
    }

    /**
     * Create a pod that would fail.
     *
     * @param podName The name of the pod to create
     */
    protected void createFailingContainer(String podName) {
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
                .withName(podName)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
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
     * Wait for a pod to startup.
     *
     * @param podName The name of the pod to wait for
     */
    private void waitForPodStartup(String podName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Waiting for pod " + podName + " to startup");
        }
        while (true) {
            Pod createdPod = k8sClient.pods()
                    .inNamespace(Constants.NAMESPACE)
                    .withName(podName)
                    .get();
            if ("Running".equals(createdPod.getStatus().getPhase())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Started pod " + podName);
                }
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
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
