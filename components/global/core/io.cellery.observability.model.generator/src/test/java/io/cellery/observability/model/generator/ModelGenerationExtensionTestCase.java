/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.cellery.observability.model.generator;

import io.cellery.observability.model.generator.internal.ModelStoreManager;
import io.cellery.observability.model.generator.internal.ServiceHolder;
import io.cellery.observability.model.generator.model.Edge;
import io.cellery.observability.model.generator.model.EdgeNode;
import io.cellery.observability.model.generator.model.Model;
import io.cellery.observability.model.generator.model.ModelManager;
import io.cellery.observability.model.generator.model.Node;
import org.apache.log4j.Logger;
import org.powermock.api.mockito.PowerMockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.config.ConfigProviderFactory;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.datasource.core.DataSourceManager;
import org.wso2.carbon.datasource.core.impl.DataSourceServiceImpl;
import org.wso2.carbon.secvault.SecureVault;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.persistence.InMemoryPersistenceStore;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the unit test case for the model generation use cases.
 */
public class ModelGenerationExtensionTestCase {

    private static final Logger logger = Logger.getLogger(ModelGenerationExtensionTestCase.class.getName());
    private static final String CARBON_HOME_ENV = "carbon.home";
    private static final String WSO2_RUNTIME_ENV = "wso2.runtime";
    private static final String INPUT_STREAM = "inputStream";

    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> sentEvents;
    private List<Event> receivedEvents;

    @BeforeClass
    private void initTestCase() {
        String carbonHomePath = this.getClass().getResource("/").getFile();
        System.setProperty(CARBON_HOME_ENV, carbonHomePath);
        System.setProperty(WSO2_RUNTIME_ENV, "worker");
    }

    @AfterClass
    public void cleanUpTestCase() {
        System.setProperty(CARBON_HOME_ENV, "");
        System.setProperty(WSO2_RUNTIME_ENV, "");
    }

    @BeforeMethod
    public void initTest() throws Exception {
        sentEvents = new ArrayList<>();
        receivedEvents = new ArrayList<>();

        // Initialize data source service
        DataSourceServiceImpl dataSourceService = new DataSourceServiceImpl();
        SecureVault secureVault = PowerMockito.mock(SecureVault.class);
        ConfigProvider configProvider = ConfigProviderFactory.getConfigProvider(Paths.get(
                System.getProperty(CARBON_HOME_ENV), "conf" + File.separator + "deployment.yaml"), secureVault);
        DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        dataSourceManager.initDataSources(configProvider);
        ServiceHolder.setDataSourceService(dataSourceService);

        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());
    }

    @AfterMethod
    public void cleanUpTest() throws Exception {
        Assert.assertEquals(receivedEvents, sentEvents);
        if (siddhiAppRuntime != null) {
            siddhiAppRuntime.shutdown();
        }
        if (ServiceHolder.getModelStoreManager() != null) {
            ServiceHolder.getModelStoreManager().clear();
        }

        ServiceHolder.setModelManager(null);
        ServiceHolder.setModelStoreManager(null);
        ServiceHolder.setDataSourceService(null);
    }

    @Test
    public void testSingleInstanceGraph() throws Exception {
        initializeSiddhiAppRuntime();
        String runtime = "test-runtime";
        String namespace = "test-namespace";
        String instance = "single-instance";
        Node helloGatewayNode = new Node(namespace, instance, "gateway");
        Node componentANode = new Node(namespace, instance, "component-a");
        Node componentBNode = new Node(namespace, instance, "component-b");
        Node componentCNode = new Node(namespace, instance, "component-c");
        Node componentDNode = new Node(namespace, instance, "component-d");
        Node componentENode = new Node(namespace, instance, "component-e");

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, runtime, helloGatewayNode, componentANode);
        publishEvent(inputHandler, runtime, helloGatewayNode, componentANode);
        publishEvent(inputHandler, runtime, helloGatewayNode, componentBNode);
        publishEvent(inputHandler, runtime, componentBNode, componentCNode);
        publishEvent(inputHandler, runtime, componentCNode, componentDNode);
        publishEvent(inputHandler, runtime, componentCNode, componentDNode);
        publishEvent(inputHandler, runtime, componentCNode, componentDNode);
        publishEvent(inputHandler, runtime, componentCNode, componentENode);
        publishEvent(inputHandler, runtime, componentCNode, componentENode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Map<String, Model> currentModels = ServiceHolder.getModelManager().getCurrentRuntimeModels();
            Assert.assertNotNull(currentModels);
            Assert.assertEquals(currentModels.size(), 1);
            Model model = currentModels.get(runtime);
            Assert.assertNotNull(model);
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getRuntimeDependencyModel(startTime, endTime, runtime);
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, runtime,
                    namespace);
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtime,
                    helloGatewayNode.getNamespace(), helloGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    helloGatewayNode.getNamespace(), helloGatewayNode.getInstance(), helloGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    componentCNode.getNamespace(), componentCNode.getInstance(), componentCNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(componentCNode, componentDNode),
                    generateEdge(componentCNode, componentENode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtime,
                    helloGatewayNode.getNamespace(), helloGatewayNode.getInstance(), helloGatewayNode.getComponent());
            Assert.assertEquals(actualNode, helloGatewayNode);
        }
    }

    @Test
    public void testMultiInstanceGraph() throws Exception {
        initializeSiddhiAppRuntime();
        String runtime = "test-runtime";
        String namespace = "test-namespace";
        String instanceA = "multi-instance-a";
        String instanceB = "multi-instance-b";
        String instanceC = "multi-instance-c";
        String instanceD = "multi-instance-d";
        String gateway = "gateway";
        String componentA = "component-a";
        String componentB = "component-b";
        String componentC = "component-c";
        Node instanceAGatewayNode = new Node(namespace, instanceA, gateway);
        Node instanceAComponentANode = new Node(namespace, instanceA, componentA);
        Node instanceAComponentBNode = new Node(namespace, instanceA, componentB);
        Node instanceAComponentCNode = new Node(namespace, instanceA, componentC);
        Node instanceBGatewayNode = new Node(namespace, instanceB, gateway);
        Node instanceBComponentANode = new Node(namespace, instanceB, componentA);
        Node instanceBComponentBNode = new Node(namespace, instanceB, componentB);
        Node instanceCGatewayNode = new Node(namespace, instanceC, gateway);
        Node instanceCComponentANode = new Node(namespace, instanceC, componentA);
        Node instanceDGatewayNode = new Node(namespace, instanceD, gateway);
        Node instanceDComponentANode = new Node(namespace, instanceD, componentA);
        Node instanceDComponentBNode = new Node(namespace, instanceD, componentB);

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, runtime, instanceAGatewayNode, instanceAComponentANode);
        publishEvent(inputHandler, runtime, instanceAGatewayNode, instanceAComponentBNode);
        publishEvent(inputHandler, runtime, instanceAComponentANode, instanceAComponentCNode);
        publishEvent(inputHandler, runtime, instanceAComponentCNode, instanceBGatewayNode);
        publishEvent(inputHandler, runtime, instanceAComponentANode, instanceCGatewayNode);
        publishEvent(inputHandler, runtime, instanceBGatewayNode, instanceBComponentANode);
        publishEvent(inputHandler, runtime, instanceBComponentANode, instanceBComponentBNode);
        publishEvent(inputHandler, runtime, instanceCGatewayNode, instanceCComponentANode);
        publishEvent(inputHandler, runtime, instanceCComponentANode, instanceDGatewayNode);
        publishEvent(inputHandler, runtime, instanceDGatewayNode, instanceDComponentANode);
        publishEvent(inputHandler, runtime, instanceDComponentANode, instanceDComponentBNode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Map<String, Model> currentModels = ServiceHolder.getModelManager().getCurrentRuntimeModels();
            Assert.assertNotNull(currentModels);
            Assert.assertEquals(currentModels.size(), 1);
            Model model = currentModels.get(runtime);
            Assert.assertNotNull(model);
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceAComponentANode, instanceCGatewayNode),
                    generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode),
                    generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode),
                    generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getRuntimeDependencyModel(startTime, endTime, runtime);
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceAComponentANode, instanceCGatewayNode),
                    generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode),
                    generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode),
                    generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, runtime,
                    namespace);
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceAComponentANode, instanceCGatewayNode),
                    generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode),
                    generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode),
                    generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtime,
                    instanceAGatewayNode.getNamespace(), instanceAGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode,
                    instanceCGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceAComponentANode, instanceCGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtime,
                    instanceBGatewayNode.getNamespace(),
                    instanceBGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceAGatewayNode.getNamespace(), instanceAGatewayNode.getInstance(),
                    instanceAGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceCGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceAComponentANode, instanceCGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance(),
                    instanceCGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceCComponentANode.getNamespace(), instanceCComponentANode.getInstance(),
                    instanceCComponentANode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCComponentANode, instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceDGatewayNode.getNamespace(), instanceDGatewayNode.getInstance(),
                    instanceDGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceDGatewayNode, instanceDComponentANode,
                    instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtime,
                    instanceAComponentANode.getNamespace(), instanceAComponentANode.getInstance(),
                    instanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, instanceAComponentANode);
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtime,
                    instanceBComponentANode.getNamespace(), instanceBComponentANode.getInstance(),
                    instanceBComponentANode.getComponent());
            Assert.assertEquals(actualNode, instanceBComponentANode);
        }
    }

    @Test
    public void testDisjointMultiInstanceGraph() throws Exception {
        initializeSiddhiAppRuntime();
        String runtime = "test-runtime";
        String namespace = "test-namespace";
        String instanceA = "disjoint-multi-instance-a";
        String instanceB = "disjoint-multi-instance-b";
        String instanceC = "disjoint-multi-instance-c";
        String instanceD = "disjoint-multi-instance-d";
        String gateway = "gateway";
        String componentA = "component-a";
        String componentB = "component-b";
        String componentC = "component-c";
        Node instanceAGatewayNode = new Node(namespace, instanceA, gateway);
        Node instanceAComponentANode = new Node(namespace, instanceA, componentA);
        Node instanceAComponentBNode = new Node(namespace, instanceA, componentB);
        Node instanceAComponentCNode = new Node(namespace, instanceA, componentC);
        Node instanceBGatewayNode = new Node(namespace, instanceB, gateway);
        Node instanceBComponentANode = new Node(namespace, instanceB, componentA);
        Node instanceBComponentBNode = new Node(namespace, instanceB, componentB);
        Node instanceCGatewayNode = new Node(namespace, instanceC, gateway);
        Node instanceCComponentANode = new Node(namespace, instanceC, componentA);
        Node instanceDGatewayNode = new Node(namespace, instanceD, gateway);
        Node instanceDComponentANode = new Node(namespace, instanceD, componentA);
        Node instanceDComponentBNode = new Node(namespace, instanceD, componentB);

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, runtime, instanceAGatewayNode, instanceAComponentANode);
        publishEvent(inputHandler, runtime, instanceAGatewayNode, instanceAComponentBNode);
        publishEvent(inputHandler, runtime, instanceAComponentANode, instanceAComponentCNode);
        publishEvent(inputHandler, runtime, instanceAComponentCNode, instanceBGatewayNode);
        publishEvent(inputHandler, runtime, instanceBGatewayNode, instanceBComponentANode);
        publishEvent(inputHandler, runtime, instanceBComponentANode, instanceBComponentBNode);
        publishEvent(inputHandler, runtime, instanceCGatewayNode, instanceCComponentANode);
        publishEvent(inputHandler, runtime, instanceCComponentANode, instanceDGatewayNode);
        publishEvent(inputHandler, runtime, instanceDGatewayNode, instanceDComponentANode);
        publishEvent(inputHandler, runtime, instanceDComponentANode, instanceDComponentBNode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Map<String, Model> currentModels = ServiceHolder.getModelManager().getCurrentRuntimeModels();
            Assert.assertNotNull(currentModels);
            Assert.assertEquals(currentModels.size(), 1);
            Model model = currentModels.get(runtime);
            Assert.assertNotNull(model);
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode),
                    generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode),
                    generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getRuntimeDependencyModel(startTime, endTime, runtime);
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode),
                    generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode),
                    generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, runtime,
                    namespace);
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode),
                    generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode),
                    generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode),
                    generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtime,
                    instanceAGatewayNode.getNamespace(), instanceAGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtime,
                    instanceBGatewayNode.getNamespace(), instanceBGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceAGatewayNode.getNamespace(), instanceAGatewayNode.getInstance(),
                    instanceAGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance(),
                    instanceCGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceCComponentANode.getNamespace(), instanceCComponentANode.getInstance(),
                    instanceCComponentANode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCComponentANode, instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtime,
                    instanceDGatewayNode.getNamespace(), instanceDGatewayNode.getInstance(),
                    instanceDGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceDGatewayNode, instanceDComponentANode,
                    instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtime,
                    instanceCComponentANode.getNamespace(), instanceCComponentANode.getInstance(),
                    instanceCComponentANode.getComponent());
            Assert.assertEquals(actualNode, instanceCComponentANode);
        }
    }

    @Test
    public void testRuntimeSeparation() throws Exception {
        initializeSiddhiAppRuntime();
        String runtimeA = "test-runtime-a";
        String runtimeB = "test-runtime-b";
        String namespaceA = "namespace-a";
        String namespaceB = "namespace-b";
        String instanceA = "instance-a";
        String instanceB = "instance-b";
        String gateway = "gateway";
        String componentA = "component-a";
        String componentB = "component-b";
        Node runtimeANamespaceAInstanceAGatewayNode = new Node(namespaceA, instanceA, gateway);
        Node runtimeANamespaceAInstanceAComponentANode = new Node(namespaceA, instanceA, componentA);
        Node runtimeANamespaceAInstanceBGatewayNode = new Node(namespaceA, instanceB, gateway);
        Node runtimeANamespaceAInstanceBComponentANode = new Node(namespaceA, instanceB, componentA);
        Node runtimeANamespaceBInstanceAGatewayNode = new Node(namespaceB, instanceA, gateway);
        Node runtimeANamespaceBInstanceAComponentANode = new Node(namespaceB, instanceA, componentA);
        Node runtimeANamespaceBInstanceAComponentBNode = new Node(namespaceB, instanceA, componentB);
        Node runtimeBNamespaceAInstanceAGatewayNode = new Node(namespaceA, instanceA, gateway);
        Node runtimeBNamespaceAInstanceAComponentANode = new Node(namespaceA, instanceA, componentA);
        Node runtimeBNamespaceAInstanceAComponentBNode = new Node(namespaceA, instanceA, componentB);

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, runtimeA, runtimeANamespaceAInstanceAGatewayNode,
                runtimeANamespaceAInstanceAComponentANode);
        publishEvent(inputHandler, runtimeA, runtimeANamespaceAInstanceAComponentANode,
                runtimeANamespaceAInstanceBGatewayNode);
        publishEvent(inputHandler, runtimeA, runtimeANamespaceAInstanceBGatewayNode,
                runtimeANamespaceAInstanceBComponentANode);
        publishEvent(inputHandler, runtimeA, runtimeANamespaceAInstanceBComponentANode,
                runtimeANamespaceBInstanceAGatewayNode);
        publishEvent(inputHandler, runtimeA, runtimeANamespaceBInstanceAGatewayNode,
                runtimeANamespaceBInstanceAComponentANode);
        publishEvent(inputHandler, runtimeA, runtimeANamespaceBInstanceAGatewayNode,
                runtimeANamespaceBInstanceAComponentBNode);
        publishEvent(inputHandler, runtimeB, runtimeBNamespaceAInstanceAGatewayNode,
                runtimeBNamespaceAInstanceAComponentANode);
        publishEvent(inputHandler, runtimeB, runtimeBNamespaceAInstanceAComponentANode,
                runtimeBNamespaceAInstanceAComponentBNode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Map<String, Model> currentModels = ServiceHolder.getModelManager().getCurrentRuntimeModels();
            Assert.assertNotNull(currentModels);
            Assert.assertEquals(currentModels.size(), 2);
            {
                Model model = currentModels.get(runtimeA);
                Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceAInstanceAGatewayNode,
                        runtimeANamespaceAInstanceAComponentANode, runtimeANamespaceAInstanceBGatewayNode,
                        runtimeANamespaceAInstanceBComponentANode, runtimeANamespaceBInstanceAGatewayNode,
                        runtimeANamespaceBInstanceAComponentANode, runtimeANamespaceBInstanceAComponentBNode));
                Assert.assertEquals(model.getEdges(), asSet(
                        generateEdge(runtimeANamespaceAInstanceAGatewayNode, runtimeANamespaceAInstanceAComponentANode),
                        generateEdge(runtimeANamespaceAInstanceAComponentANode, runtimeANamespaceAInstanceBGatewayNode),
                        generateEdge(runtimeANamespaceAInstanceBGatewayNode, runtimeANamespaceAInstanceBComponentANode),
                        generateEdge(runtimeANamespaceAInstanceBComponentANode, runtimeANamespaceBInstanceAGatewayNode),
                        generateEdge(runtimeANamespaceBInstanceAGatewayNode, runtimeANamespaceBInstanceAComponentANode),
                        generateEdge(runtimeANamespaceBInstanceAGatewayNode, runtimeANamespaceBInstanceAComponentBNode))
                );
            }
            {
                Model model = currentModels.get(runtimeB);
                Assert.assertEquals(model.getNodes(), asSet(runtimeBNamespaceAInstanceAGatewayNode,
                        runtimeBNamespaceAInstanceAComponentANode, runtimeBNamespaceAInstanceAComponentBNode));
                Assert.assertEquals(model.getEdges(), asSet(
                        generateEdge(runtimeBNamespaceAInstanceAGatewayNode, runtimeBNamespaceAInstanceAComponentANode),
                        generateEdge(runtimeBNamespaceAInstanceAComponentANode,
                                runtimeBNamespaceAInstanceAComponentBNode))
                );
            }
        }
        {
            Model model = ServiceHolder.getModelManager().getRuntimeDependencyModel(startTime, endTime, runtimeA);
            Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceAInstanceAGatewayNode,
                    runtimeANamespaceAInstanceAComponentANode,
                    runtimeANamespaceAInstanceBGatewayNode, runtimeANamespaceAInstanceBComponentANode,
                    runtimeANamespaceBInstanceAGatewayNode, runtimeANamespaceBInstanceAComponentANode,
                    runtimeANamespaceBInstanceAComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(runtimeANamespaceAInstanceAGatewayNode, runtimeANamespaceAInstanceAComponentANode),
                    generateEdge(runtimeANamespaceAInstanceAComponentANode, runtimeANamespaceAInstanceBGatewayNode),
                    generateEdge(runtimeANamespaceAInstanceBGatewayNode, runtimeANamespaceAInstanceBComponentANode),
                    generateEdge(runtimeANamespaceAInstanceBComponentANode, runtimeANamespaceBInstanceAGatewayNode),
                    generateEdge(runtimeANamespaceBInstanceAGatewayNode, runtimeANamespaceBInstanceAComponentANode),
                    generateEdge(runtimeANamespaceBInstanceAGatewayNode, runtimeANamespaceBInstanceAComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, runtimeA,
                    runtimeANamespaceAInstanceAGatewayNode.getNamespace());
            Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceAInstanceAGatewayNode,
                    runtimeANamespaceAInstanceAComponentANode,
                    runtimeANamespaceAInstanceBGatewayNode, runtimeANamespaceAInstanceBComponentANode,
                    runtimeANamespaceBInstanceAGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(runtimeANamespaceAInstanceAGatewayNode, runtimeANamespaceAInstanceAComponentANode),
                    generateEdge(runtimeANamespaceAInstanceAComponentANode, runtimeANamespaceAInstanceBGatewayNode),
                    generateEdge(runtimeANamespaceAInstanceBGatewayNode, runtimeANamespaceAInstanceBComponentANode),
                    generateEdge(runtimeANamespaceAInstanceBComponentANode, runtimeANamespaceBInstanceAGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, runtimeA,
                    runtimeANamespaceBInstanceAGatewayNode.getNamespace());
            Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceBInstanceAGatewayNode,
                    runtimeANamespaceBInstanceAComponentANode, runtimeANamespaceBInstanceAComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(runtimeANamespaceBInstanceAGatewayNode, runtimeANamespaceBInstanceAComponentANode),
                    generateEdge(runtimeANamespaceBInstanceAGatewayNode, runtimeANamespaceBInstanceAComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtimeA,
                    runtimeANamespaceAInstanceAGatewayNode.getNamespace(),
                    runtimeANamespaceAInstanceAGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceAInstanceAGatewayNode,
                    runtimeANamespaceAInstanceAComponentANode, runtimeANamespaceAInstanceBGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(runtimeANamespaceAInstanceAGatewayNode, runtimeANamespaceAInstanceAComponentANode),
                    generateEdge(runtimeANamespaceAInstanceAComponentANode, runtimeANamespaceAInstanceBGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime, runtimeA,
                    runtimeANamespaceAInstanceBGatewayNode.getNamespace(),
                    runtimeANamespaceAInstanceBGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceAInstanceBGatewayNode,
                    runtimeANamespaceAInstanceBComponentANode, runtimeANamespaceBInstanceAGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(runtimeANamespaceAInstanceBGatewayNode, runtimeANamespaceAInstanceBComponentANode),
                    generateEdge(runtimeANamespaceAInstanceBComponentANode, runtimeANamespaceBInstanceAGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtimeA,
                    runtimeANamespaceAInstanceAComponentANode.getNamespace(),
                    runtimeANamespaceAInstanceAComponentANode.getInstance(),
                    runtimeANamespaceAInstanceAComponentANode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceAInstanceAComponentANode,
                    runtimeANamespaceAInstanceBGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(runtimeANamespaceAInstanceAComponentANode, runtimeANamespaceAInstanceBGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime, runtimeA,
                    runtimeANamespaceAInstanceBComponentANode.getNamespace(),
                    runtimeANamespaceAInstanceBComponentANode.getInstance(),
                    runtimeANamespaceAInstanceBComponentANode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(runtimeANamespaceAInstanceBComponentANode,
                    runtimeANamespaceBInstanceAGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(runtimeANamespaceAInstanceBComponentANode, runtimeANamespaceBInstanceAGatewayNode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtimeA,
                    runtimeANamespaceAInstanceAComponentANode.getNamespace(),
                    runtimeANamespaceAInstanceAComponentANode.getInstance(),
                    runtimeANamespaceAInstanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, runtimeANamespaceAInstanceAComponentANode);
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtimeA,
                    runtimeANamespaceBInstanceAComponentANode.getNamespace(),
                    runtimeANamespaceBInstanceAComponentANode.getInstance(),
                    runtimeANamespaceBInstanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, runtimeANamespaceBInstanceAComponentANode);
        }
    }

    @Test
    public void testNamespaceSeparation() throws Exception {
        initializeSiddhiAppRuntime();
        String runtime = "test-runtime";
        String namespaceA = "namespace-a";
        String namespaceB = "namespace-b";
        String instanceA = "instance-a";
        String instanceB = "instance-b";
        String instanceC = "instance-c";
        String instanceD = "instance-d";
        String gateway = "gateway";
        String componentA = "component-a";
        String componentB = "component-b";
        String componentC = "component-c";
        Node namespaceAInstanceAGatewayNode = new Node(namespaceA, instanceA, gateway);
        Node namespaceAInstanceAComponentANode = new Node(namespaceA, instanceA, componentA);
        Node namespaceAInstanceAComponentBNode = new Node(namespaceA, instanceA, componentB);
        Node namespaceAInstanceAComponentCNode = new Node(namespaceA, instanceA, componentC);
        Node namespaceAInstanceBGatewayNode = new Node(namespaceA, instanceB, gateway);
        Node namespaceAInstanceBComponentANode = new Node(namespaceA, instanceB, componentA);
        Node namespaceAInstanceBComponentBNode = new Node(namespaceA, instanceB, componentB);
        Node namespaceBInstanceAGatewayNode = new Node(namespaceB, instanceA, gateway);
        Node namespaceBInstanceAComponentANode = new Node(namespaceB, instanceA, componentA);
        Node namespaceBInstanceCGatewayNode = new Node(namespaceB, instanceC, gateway);
        Node namespaceBInstanceCComponentANode = new Node(namespaceB, instanceC, componentA);
        Node namespaceBInstanceCComponentBNode = new Node(namespaceB, instanceC, componentB);
        Node namespaceBInstanceDGatewayNode = new Node(namespaceB, instanceD, gateway);
        Node namespaceBInstanceDComponentANode = new Node(namespaceB, instanceD, componentA);
        Node namespaceBInstanceDComponentBNode = new Node(namespaceB, instanceD, componentB);

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, runtime, namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentANode);
        publishEvent(inputHandler, runtime, namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentBNode);
        publishEvent(inputHandler, runtime, namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode);
        publishEvent(inputHandler, runtime, namespaceAInstanceAComponentANode, namespaceAInstanceBGatewayNode);
        publishEvent(inputHandler, runtime, namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentANode);
        publishEvent(inputHandler, runtime, namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentBNode);
        publishEvent(inputHandler, runtime, namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode);
        publishEvent(inputHandler, runtime, namespaceBInstanceAGatewayNode, namespaceBInstanceAComponentANode);
        publishEvent(inputHandler, runtime, namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentANode);
        publishEvent(inputHandler, runtime, namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentBNode);
        publishEvent(inputHandler, runtime, namespaceBInstanceCComponentBNode, namespaceBInstanceDGatewayNode);
        publishEvent(inputHandler, runtime, namespaceBInstanceDGatewayNode, namespaceBInstanceDComponentANode);
        publishEvent(inputHandler, runtime, namespaceBInstanceDComponentANode, namespaceBInstanceDComponentBNode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Map<String, Model> currentModels = ServiceHolder.getModelManager().getCurrentRuntimeModels();
            Assert.assertNotNull(currentModels);
            Assert.assertEquals(currentModels.size(), 1);
            Model model = currentModels.get(runtime);
            Assert.assertNotNull(model);
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceAGatewayNode,
                    namespaceAInstanceAComponentANode, namespaceAInstanceAComponentBNode,
                    namespaceAInstanceAComponentCNode, namespaceAInstanceBGatewayNode,
                    namespaceAInstanceBComponentANode, namespaceAInstanceBComponentBNode,
                    namespaceBInstanceAGatewayNode, namespaceBInstanceAComponentANode,
                    namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentANode,
                    namespaceBInstanceCComponentBNode, namespaceBInstanceDGatewayNode,
                    namespaceBInstanceDComponentANode, namespaceBInstanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentANode),
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentBNode),
                    generateEdge(namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode),
                    generateEdge(namespaceAInstanceAComponentANode, namespaceAInstanceBGatewayNode),
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentANode),
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentBNode),
                    generateEdge(namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode),
                    generateEdge(namespaceBInstanceAGatewayNode, namespaceBInstanceAComponentANode),
                    generateEdge(namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentANode),
                    generateEdge(namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentBNode),
                    generateEdge(namespaceBInstanceCComponentBNode, namespaceBInstanceDGatewayNode),
                    generateEdge(namespaceBInstanceDGatewayNode, namespaceBInstanceDComponentANode),
                    generateEdge(namespaceBInstanceDComponentANode, namespaceBInstanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getRuntimeDependencyModel(startTime, endTime, runtime);
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceAGatewayNode,
                    namespaceAInstanceAComponentANode, namespaceAInstanceAComponentBNode,
                    namespaceAInstanceAComponentCNode, namespaceAInstanceBGatewayNode,
                    namespaceAInstanceBComponentANode, namespaceAInstanceBComponentBNode,
                    namespaceBInstanceAGatewayNode, namespaceBInstanceAComponentANode,
                    namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentANode,
                    namespaceBInstanceCComponentBNode, namespaceBInstanceDGatewayNode,
                    namespaceBInstanceDComponentANode, namespaceBInstanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentANode),
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentBNode),
                    generateEdge(namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode),
                    generateEdge(namespaceAInstanceAComponentANode, namespaceAInstanceBGatewayNode),
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentANode),
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentBNode),
                    generateEdge(namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode),
                    generateEdge(namespaceBInstanceAGatewayNode, namespaceBInstanceAComponentANode),
                    generateEdge(namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentANode),
                    generateEdge(namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentBNode),
                    generateEdge(namespaceBInstanceCComponentBNode, namespaceBInstanceDGatewayNode),
                    generateEdge(namespaceBInstanceDGatewayNode, namespaceBInstanceDComponentANode),
                    generateEdge(namespaceBInstanceDComponentANode, namespaceBInstanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime,
                    runtime, namespaceAInstanceAGatewayNode.getNamespace());
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceAGatewayNode,
                    namespaceAInstanceAComponentANode, namespaceAInstanceAComponentBNode,
                    namespaceAInstanceAComponentCNode, namespaceAInstanceBGatewayNode,
                    namespaceAInstanceBComponentANode, namespaceAInstanceBComponentBNode,
                    namespaceBInstanceAGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentANode),
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentBNode),
                    generateEdge(namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode),
                    generateEdge(namespaceAInstanceAComponentANode, namespaceAInstanceBGatewayNode),
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentANode),
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentBNode),
                    generateEdge(namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime,
                    runtime, namespaceBInstanceAGatewayNode.getNamespace());
            Assert.assertEquals(model.getNodes(), asSet(namespaceBInstanceAGatewayNode,
                    namespaceBInstanceAComponentANode, namespaceBInstanceCGatewayNode,
                    namespaceBInstanceCComponentANode, namespaceBInstanceCComponentBNode,
                    namespaceBInstanceDGatewayNode, namespaceBInstanceDComponentANode,
                    namespaceBInstanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceBInstanceAGatewayNode, namespaceBInstanceAComponentANode),
                    generateEdge(namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentANode),
                    generateEdge(namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentBNode),
                    generateEdge(namespaceBInstanceCComponentBNode, namespaceBInstanceDGatewayNode),
                    generateEdge(namespaceBInstanceDGatewayNode, namespaceBInstanceDComponentANode),
                    generateEdge(namespaceBInstanceDComponentANode, namespaceBInstanceDComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    runtime, namespaceAInstanceAGatewayNode.getNamespace(),
                    namespaceAInstanceAGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceAGatewayNode,
                    namespaceAInstanceAComponentANode, namespaceAInstanceAComponentBNode,
                    namespaceAInstanceAComponentCNode, namespaceAInstanceBGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentANode),
                    generateEdge(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentBNode),
                    generateEdge(namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode),
                    generateEdge(namespaceAInstanceAComponentANode, namespaceAInstanceBGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    runtime, namespaceAInstanceBGatewayNode.getNamespace(),
                    namespaceAInstanceBGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceBGatewayNode,
                    namespaceAInstanceBComponentANode, namespaceAInstanceBComponentBNode,
                    namespaceBInstanceAGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentANode),
                    generateEdge(namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentBNode),
                    generateEdge(namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    runtime, namespaceAInstanceAComponentBNode.getNamespace(),
                    namespaceAInstanceAComponentBNode.getInstance(), namespaceAInstanceAComponentBNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceAComponentBNode,
                    namespaceAInstanceAComponentCNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    runtime, namespaceAInstanceBComponentBNode.getNamespace(),
                    namespaceAInstanceBComponentBNode.getInstance(), namespaceAInstanceBComponentBNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceBComponentBNode,
                    namespaceBInstanceAGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtime,
                    namespaceAInstanceAComponentANode.getNamespace(), namespaceAInstanceAComponentANode.getInstance(),
                    namespaceAInstanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, namespaceAInstanceAComponentANode);
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(runtime,
                    namespaceBInstanceAComponentANode.getNamespace(), namespaceBInstanceAComponentANode.getInstance(),
                    namespaceBInstanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, namespaceBInstanceAComponentANode);
        }
    }

    @Test
    public void testReloadModelManager() throws Exception {
        Map<String, Model> initialRuntimeModels = ServiceHolder.getModelManager().getCurrentRuntimeModels();

        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());

        Assert.assertEquals(ServiceHolder.getModelManager().getCurrentRuntimeModels(), initialRuntimeModels);
    }

    @Test
    public void testLoadModelWithEmptyTimeRange() throws Exception {
        Model model = ServiceHolder.getModelManager().getRuntimeDependencyModel(System.currentTimeMillis(),
                System.currentTimeMillis() + 10000, "runtime-a");

        Assert.assertEquals(model.getNodes().size(), 0);
        Assert.assertEquals(model.getEdges().size(), 0);
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidParamCount() {
        testParams("sourceNamespace, sourceInstance, sourceComponent, sourceInstanceKind, " +
                "destinationNamespace, destinationInstance, destinationComponent");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidSourceNamespaceParamType() {
        testParams("invalidValue, sourceInstance, sourceComponent, sourceInstanceKind, " +
                "destinationNamespace, destinationInstance, destinationComponent, destinationInstanceKind");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidSourceInstanceParamType() {
        testParams("sourceNamespace, invalidValue, sourceComponent, sourceInstanceKind, " +
                "destinationNamespace, destinationInstance, destinationComponent, destinationInstanceKind");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidSourceComponentParamType() {
        testParams("sourceNamespace, sourceInstance, invalidValue, sourceInstanceKind, " +
                "destinationNamespace, destinationInstance, destinationComponent, destinationInstanceKind");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidSourceInstanceKindParamType() {
        testParams("sourceNamespace, sourceInstance, sourceComponent, invalidValue, " +
                "destinationNamespace, destinationInstance, destinationComponent, destinationInstanceKind");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidDestinationNamespaceParamType() {
        testParams("sourceNamespace, sourceInstance, sourceComponent, sourceInstanceKind, " +
                "invalidValue, destinationInstance, destinationComponent, destinationInstanceKind");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidDestinationInstanceParamType() {
        testParams("sourceNamespace, sourceInstance, sourceComponent, sourceInstanceKind, " +
                "destinationNamespace, invalidValue, destinationComponent, destinationInstanceKind");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidDestinationComponentParamType() {
        testParams("sourceNamespace, sourceInstance, sourceComponent, sourceInstanceKind, " +
                "destinationNamespace, destinationInstance, invalidValue, destinationInstanceKind");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidDestinationIntanceKindParamType() {
        testParams("sourceNamespace, sourceInstance, sourceComponent, sourceInstanceKind, " +
                "destinationNamespace, destinationInstance, destinationComponent, invalidValue");
    }

    @Test
    public void testPersistence() throws Exception {
        initializeSiddhiAppRuntime();
        siddhiAppRuntime.persist();
        siddhiAppRuntime.restoreLastRevision();


    }

    /**
     * Run a test for params.
     *
     * @param params The parameters to be used in the test
     */
    private void testParams(String params) {
        String streamDefinitionAttributes = "sourceNamespace string, sourceInstance string, sourceComponent string, " +
                "sourceInstanceKind string, destinationNamespace string, destinationInstance string, " +
                "destinationComponent string, destinationInstanceKind string, invalidValue int";
        String inStreamDefinition = "define stream inputStream(" + streamDefinitionAttributes + ");";
        String outStreamDefinition = "define stream outputStream(" + streamDefinitionAttributes + ");";
        String query = "@info(name = \"query\")\n" +
                "from inputStream#observe:modelGenerator(" + params + ")\n" +
                "select *\n" +
                "insert into outputStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setPersistenceStore(new InMemoryPersistenceStore());
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime("@App:name(\"test-siddhi-app\")\n"
                + inStreamDefinition + "\n" + outStreamDefinition + "\n" + query);
    }

    /**
     * Publish an event from a source node to a destination node.
     *
     * @param inputHandler Siddhi input handler to use
     * @param runtime The runtime the source and destination nodes belongs to
     * @param sourceNode The source node of the request
     * @param destinationNode The destintaion node of the request
     * @throws Exception If input handler throws an exception
     */
    private void publishEvent(InputHandler inputHandler, String runtime, Node sourceNode, Node destinationNode)
            throws Exception {
        inputHandler.send(new Object[]{runtime, sourceNode.getNamespace(), sourceNode.getInstance(),
                sourceNode.getComponent(), sourceNode.getInstanceKind(), destinationNode.getNamespace(),
                destinationNode.getInstance(), destinationNode.getComponent(), destinationNode.getInstanceKind()});
    }

    /**
     * Generate set from items.
     *
     * @param items The items to add to the generated set
     * @param <T> The Set type
     * @return The generated set
     */
    private <T> Set<T> asSet(T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }

    /**
     * Generate an Edge object using a source and target node.
     *
     * @param sourceNode The source node of the edge to be created
     * @param targetNode The target node of the edge to be created
     * @return The generated edge
     */
    private Edge generateEdge(Node sourceNode, Node targetNode) {
        return new Edge(
                new EdgeNode(sourceNode.getNamespace(), sourceNode.getInstance(), sourceNode.getComponent()),
                new EdgeNode(targetNode.getNamespace(), targetNode.getInstance(), targetNode.getComponent())
        );
    }

    /**
     * Initialize the Siddhi App Runtime.
     */
    private void initializeSiddhiAppRuntime() {
        String streamDefinitionAttributes = "runtime string, sourceNamespace string, sourceInstance string, " +
                "sourceComponent string, sourceInstanceKind string, destinationNamespace string, " +
                "destinationInstance string, destinationComponent string, destinationInstanceKind string";
        String inStreamDefinition = "define stream inputStream(" + streamDefinitionAttributes + ");";
        String outStreamDefinition = "define stream outputStream(" + streamDefinitionAttributes + ");";
        String query = "@info(name = \"query\")\n" +
                "from inputStream#observe:modelGenerator(runtime, sourceNamespace, sourceInstance, sourceComponent, " +
                "sourceInstanceKind, destinationNamespace, destinationInstance, destinationComponent, " +
                "destinationInstanceKind)\n" +
                "select *\n" +
                "insert into outputStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setPersistenceStore(new InMemoryPersistenceStore());
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime("@App:name(\"test-siddhi-app\")\n"
                + inStreamDefinition + "\n" + outStreamDefinition + "\n" + query);
        siddhiAppRuntime.addCallback("inputStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                for (Event event : events) {
                    synchronized (this) {
                        sentEvents.add(event);
                    }
                }
            }
        });
        siddhiAppRuntime.addCallback("outputStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    synchronized (this) {
                        receivedEvents.add(event);
                    }
                }
            }
        });
        siddhiAppRuntime.start();
        if (logger.isDebugEnabled()) {
            logger.debug("Initialized Siddhi App Runtime");
        }
    }
}
