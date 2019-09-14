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

import io.cellery.observability.k8s.client.crds.cell.Cell;
import io.cellery.observability.k8s.client.crds.composite.Composite;
import io.cellery.observability.k8s.client.crds.gateway.GRPC;
import io.cellery.observability.k8s.client.crds.gateway.HTTP;
import io.cellery.observability.k8s.client.crds.gateway.TCP;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities test case.
 */
public class UtilsTestCase extends BaseTestCase {

    UtilsTestCase() throws Exception {
        super();
    }

    @Test
    public void testGetComponentNameWithComponentLabel() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.COMPONENT_NAME_LABEL, "pet-fe--test-c");
        Pod pod = generatePod(labels);

        Assert.assertEquals(Utils.getComponentName(pod), "test-c");
    }

    @Test
    public void testGetComponentNameWithGatewayLabel() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.GATEWAY_NAME_LABEL, "pet-fe--gateway");
        Pod pod = generatePod(labels);

        Assert.assertEquals(Utils.getComponentName(pod), "gateway");
    }

    @Test
    public void testGetComponentNameWithNoLabels() {
        Pod pod = generatePod(new HashMap<>());

        Assert.assertEquals(Utils.getComponentName(pod), "");
    }

    @Test
    public void testGetComponentNameWithInvalidComponentLabel() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.COMPONENT_NAME_LABEL, "pet-fe");
        Pod pod = generatePod(labels);

        Assert.assertEquals(Utils.getComponentName(pod), "");
    }

    @Test
    public void testGetComponentNameWithInvalidGatewayLabel() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.GATEWAY_NAME_LABEL, "pet-fe");
        Pod pod = generatePod(labels);

        Assert.assertEquals(Utils.getComponentName(pod), "");
    }

    @Test
    public void testGetComponentIngressTypesOfCellWithHttpIngresses() {
        HTTP ingressA = new HTTP();
        ingressA.setAuthenticate(true);
        ingressA.setBackend("controller");
        ingressA.setContext("/controller");
        ingressA.setGlobal(true);
        HTTP ingressB = new HTTP();
        ingressB.setAuthenticate(true);
        ingressB.setBackend("customers");
        ingressB.setContext("/customers");
        ingressB.setGlobal(false);
        Cell cell = generateCell("pet-be",
                generateGatewayTemplate("Envoy", null, Arrays.asList(ingressA, ingressB), null, null),
                Arrays.asList(generateServicesTemplate("controller", "HTTP"),
                        generateServicesTemplate("catalog", "GRPC"),
                        generateServicesTemplate("orders", "GRPC"),
                        generateServicesTemplate("customers", "HTTP")));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(cell);
        Assert.assertEquals(componentIngressTypes.size(), 4);
        {
            List<String> ingressTypes = componentIngressTypes.get("controller");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"HTTP"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("catalog");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("customers");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"HTTP"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("orders");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCellWithWebIngresses() {
        HTTP ingressA = new HTTP();
        ingressA.setAuthenticate(true);
        ingressA.setBackend("portal");
        ingressA.setContext("/portal");
        ingressA.setGlobal(true);
        Cell cell = generateCell("pet-fe",
                generateGatewayTemplate("Envoy", "pet-store.com", Collections.singletonList(ingressA), null, null),
                Arrays.asList(generateServicesTemplate("portal", "HTTP"),
                        generateServicesTemplate("store", "HTTP")));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(cell);
        Assert.assertEquals(componentIngressTypes.size(), 2);
        {
            List<String> ingressTypes = componentIngressTypes.get("portal");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"WEB"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("store");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCellWithTcpIngresses() {
        TCP ingressA = new TCP();
        ingressA.setBackendHost("controller");
        ingressA.setBackendPort(13123);
        ingressA.setPort(9231);
        TCP ingressB = new TCP();
        ingressB.setBackendHost("orders");
        ingressB.setBackendPort(13123);
        ingressB.setPort(9231);
        Cell cell = generateCell("pet-be",
                generateGatewayTemplate("Envoy", null, null, Arrays.asList(ingressA, ingressB), null),
                Arrays.asList(generateServicesTemplate("controller", "TCP"),
                        generateServicesTemplate("catalog", "GRPC"),
                        generateServicesTemplate("orders", "TCP"),
                        generateServicesTemplate("customers", "HTTP")));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(cell);
        Assert.assertEquals(componentIngressTypes.size(), 4);
        {
            List<String> ingressTypes = componentIngressTypes.get("controller");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"TCP"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("catalog");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("customers");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("orders");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"TCP"});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCellWithGrpcIngresses() {
        GRPC ingressA = new GRPC();
        ingressA.setBackendHost("controller");
        ingressA.setBackendPort(13123);
        ingressA.setPort(9231);
        GRPC ingressB = new GRPC();
        ingressB.setBackendHost("catalog");
        ingressB.setBackendPort(13123);
        ingressB.setPort(9231);
        Cell cell = generateCell("pet-be",
                generateGatewayTemplate("Envoy", null, null, null, Arrays.asList(ingressA, ingressB)),
                Arrays.asList(generateServicesTemplate("controller", "GRPC"),
                        generateServicesTemplate("catalog", "GRPC"),
                        generateServicesTemplate("orders", "TCP"),
                        generateServicesTemplate("customers", "HTTP")));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(cell);
        Assert.assertEquals(componentIngressTypes.size(), 4);
        {
            List<String> ingressTypes = componentIngressTypes.get("controller");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"GRPC"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("catalog");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"GRPC"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("customers");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("orders");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCellWithNoIngresses() {
        Cell cell = generateCell("pet-fe",
                generateGatewayTemplate("Envoy", null, null, null, null),
                Arrays.asList(generateServicesTemplate("portal", null),
                        generateServicesTemplate("store", "TCP")));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(cell);
        Assert.assertEquals(componentIngressTypes.size(), 2);
        {
            List<String> ingressTypes = componentIngressTypes.get("portal");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("store");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCompositeWithHttpIngresses() {
        Composite composite = generateComposite("pet-fe",
                Arrays.asList(generateServicesTemplate("portal", "HTTP"),
                        generateServicesTemplate("tester", "HTTP"),
                        generateServicesTemplate("store", null)));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(composite);
        Assert.assertEquals(componentIngressTypes.size(), 3);
        {
            List<String> ingressTypes = componentIngressTypes.get("portal");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"HTTP"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("tester");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"HTTP"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("store");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCompositeWithTcpIngresses() {
        Composite composite = generateComposite("employee-comp",
                Arrays.asList(generateServicesTemplate("employee", "TCP"),
                        generateServicesTemplate("salary", "TCP")));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(composite);
        Assert.assertEquals(componentIngressTypes.size(), 2);
        {
            List<String> ingressTypes = componentIngressTypes.get("employee");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"TCP"});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("salary");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"TCP"});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCompositeWithGrpcIngresses() {
        Composite composite = generateComposite("stock-comp",
                Arrays.asList(generateServicesTemplate("stock", "GRPC")));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(composite);
        Assert.assertEquals(componentIngressTypes.size(), 1);
        {
            List<String> ingressTypes = componentIngressTypes.get("stock");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{"GRPC"});
        }
    }

    @Test
    public void testGetComponentIngressTypesOfCompositeWithNoIngresses() {
        Composite composite = generateComposite("test-comp",
                Arrays.asList(generateServicesTemplate("tester", null),
                        generateServicesTemplate("mocker", null)));

        Map<String, List<String>> componentIngressTypes = Utils.getComponentIngressTypes(composite);
        Assert.assertEquals(componentIngressTypes.size(), 2);
        {
            List<String> ingressTypes = componentIngressTypes.get("tester");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
        {
            List<String> ingressTypes = componentIngressTypes.get("mocker");
            Assert.assertNotNull(ingressTypes);
            Assert.assertEqualsNoOrder(ingressTypes.toArray(), new String[]{});
        }
    }

    /**
     * Generate a K8s Pod object.
     *
     * @param labels The labels to be applied to the Pod
     * @return The generated pod
     */
    private Pod generatePod(Map<String, String> labels) {
        ObjectMeta podMetadata = Mockito.mock(ObjectMeta.class);
        Mockito.when(podMetadata.getLabels()).thenReturn(labels);
        Pod pod = Mockito.mock(Pod.class);
        Mockito.when(pod.getMetadata()).thenReturn(podMetadata);
        return pod;
    }
}
