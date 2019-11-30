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
        ServiceHolder.getModelStoreManager().clear();

        ServiceHolder.setModelManager(null);
        ServiceHolder.setModelStoreManager(null);
        ServiceHolder.setDataSourceService(null);
    }

    @Test
    public void testSingleInstanceGraph() throws Exception {
        initializeSiddhiAppRuntime();
        String namespace = "default";
        String instance = "single-instance";
        Node helloGatewayNode = new Node(namespace, instance, "gateway");
        Node componentANode = new Node(namespace, instance, "component-a");
        Node componentBNode = new Node(namespace, instance, "component-b");
        Node componentCNode = new Node(namespace, instance, "component-c");
        Node componentDNode = new Node(namespace, instance, "component-d");
        Node componentENode = new Node(namespace, instance, "component-e");

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, helloGatewayNode, componentANode);
        publishEvent(inputHandler, helloGatewayNode, componentANode);
        publishEvent(inputHandler, helloGatewayNode, componentBNode);
        publishEvent(inputHandler, componentBNode, componentCNode);
        publishEvent(inputHandler, componentCNode, componentDNode);
        publishEvent(inputHandler, componentCNode, componentDNode);
        publishEvent(inputHandler, componentCNode, componentDNode);
        publishEvent(inputHandler, componentCNode, componentENode);
        publishEvent(inputHandler, componentCNode, componentENode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Set<Node> actualNodes = ServiceHolder.getModelManager().getCurrentNodes();
            Set<Edge> actualEdges = ServiceHolder.getModelManager().getCurrentEdges();
            Assert.assertEquals(actualNodes, asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(actualEdges, asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getDependencyModel(startTime, endTime);
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, namespace);
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    helloGatewayNode.getNamespace(), helloGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    helloGatewayNode.getNamespace(), helloGatewayNode.getInstance(), helloGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(helloGatewayNode, componentANode, componentBNode,
                    componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(helloGatewayNode, componentANode),
                    generateEdge(helloGatewayNode, componentBNode), generateEdge(componentBNode, componentCNode),
                    generateEdge(componentCNode, componentDNode), generateEdge(componentCNode, componentENode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    componentCNode.getNamespace(), componentCNode.getInstance(), componentCNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(componentCNode, componentDNode, componentENode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(componentCNode, componentDNode),
                    generateEdge(componentCNode, componentENode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(helloGatewayNode.getNamespace(),
                    helloGatewayNode.getInstance(), helloGatewayNode.getComponent());
            Assert.assertEquals(actualNode, helloGatewayNode);
        }
    }

    @Test
    public void testMultiInstanceGraph() throws Exception {
        initializeSiddhiAppRuntime();
        String namespace = "default";
        String instanceA = "multi-instance-a";
        String instanceB = "multi-instance-b";
        String instanceC = "multi-instance-c";
        String instanceD = "multi-instance-d";
        Node instanceAGatewayNode = new Node(namespace, instanceA, "gateway");
        Node instanceAComponentANode = new Node(namespace, instanceA, "component-a");
        Node instanceAComponentBNode = new Node(namespace, instanceA, "component-b");
        Node instanceAComponentCNode = new Node(namespace, instanceA, "component-c");
        Node instanceBGatewayNode = new Node(namespace, instanceB, "gateway");
        Node instanceBComponentANode = new Node(namespace, instanceB, "component-a");
        Node instanceBComponentBNode = new Node(namespace, instanceB, "component-b");
        Node instanceCGatewayNode = new Node(namespace, instanceC, "gateway");
        Node instanceCComponentANode = new Node(namespace, instanceC, "component-a");
        Node instanceDGatewayNode = new Node(namespace, instanceD, "gateway");
        Node instanceDComponentANode = new Node(namespace, instanceD, "component-a");
        Node instanceDComponentBNode = new Node(namespace, instanceD, "component-b");

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, instanceAGatewayNode, instanceAComponentANode);
        publishEvent(inputHandler, instanceAGatewayNode, instanceAComponentBNode);
        publishEvent(inputHandler, instanceAComponentANode, instanceAComponentCNode);
        publishEvent(inputHandler, instanceAComponentCNode, instanceBGatewayNode);
        publishEvent(inputHandler, instanceAComponentANode, instanceCGatewayNode);
        publishEvent(inputHandler, instanceBGatewayNode, instanceBComponentANode);
        publishEvent(inputHandler, instanceBComponentANode, instanceBComponentBNode);
        publishEvent(inputHandler, instanceCGatewayNode, instanceCComponentANode);
        publishEvent(inputHandler, instanceCComponentANode, instanceDGatewayNode);
        publishEvent(inputHandler, instanceDGatewayNode, instanceDComponentANode);
        publishEvent(inputHandler, instanceDComponentANode, instanceDComponentBNode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Set<Node> actualNodes = ServiceHolder.getModelManager().getCurrentNodes();
            Set<Edge> actualEdges = ServiceHolder.getModelManager().getCurrentEdges();
            Assert.assertEquals(actualNodes, asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(actualEdges, asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
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
            Model model = ServiceHolder.getModelManager().getDependencyModel(startTime, endTime);
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
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, namespace);
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
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
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
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    instanceBGatewayNode.getNamespace(), instanceBGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
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
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance(),
                    instanceCGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    instanceCComponentANode.getNamespace(), instanceCComponentANode.getInstance(),
                    instanceCComponentANode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCComponentANode, instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    instanceDGatewayNode.getNamespace(), instanceDGatewayNode.getInstance(),
                    instanceDGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceDGatewayNode, instanceDComponentANode,
                    instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(instanceAComponentANode.getNamespace(),
                    instanceAComponentANode.getInstance(), instanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, instanceAComponentANode);
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(instanceBComponentANode.getNamespace(),
                    instanceBComponentANode.getInstance(), instanceBComponentANode.getComponent());
            Assert.assertEquals(actualNode, instanceBComponentANode);
        }
    }

    @Test
    public void testDisjointMultiInstanceGraph() throws Exception {
        initializeSiddhiAppRuntime();
        String namespace = "default";
        String instanceA = "disjoint-multi-instance-a";
        String instanceB = "disjoint-multi-instance-b";
        String instanceC = "disjoint-multi-instance-c";
        String instanceD = "disjoint-multi-instance-d";
        Node instanceAGatewayNode = new Node(namespace, instanceA, "gateway");
        Node instanceAComponentANode = new Node(namespace, instanceA, "component-a");
        Node instanceAComponentBNode = new Node(namespace, instanceA, "component-b");
        Node instanceAComponentCNode = new Node(namespace, instanceA, "component-c");
        Node instanceBGatewayNode = new Node(namespace, instanceB, "gateway");
        Node instanceBComponentANode = new Node(namespace, instanceB, "component-a");
        Node instanceBComponentBNode = new Node(namespace, instanceB, "component-b");
        Node instanceCGatewayNode = new Node(namespace, instanceC, "gateway");
        Node instanceCComponentANode = new Node(namespace, instanceC, "component-a");
        Node instanceDGatewayNode = new Node(namespace, instanceD, "gateway");
        Node instanceDComponentANode = new Node(namespace, instanceD, "component-a");
        Node instanceDComponentBNode = new Node(namespace, instanceD, "component-b");

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, instanceAGatewayNode, instanceAComponentANode);
        publishEvent(inputHandler, instanceAGatewayNode, instanceAComponentBNode);
        publishEvent(inputHandler, instanceAComponentANode, instanceAComponentCNode);
        publishEvent(inputHandler, instanceAComponentCNode, instanceBGatewayNode);
        publishEvent(inputHandler, instanceBGatewayNode, instanceBComponentANode);
        publishEvent(inputHandler, instanceBComponentANode, instanceBComponentBNode);
        publishEvent(inputHandler, instanceCGatewayNode, instanceCComponentANode);
        publishEvent(inputHandler, instanceCComponentANode, instanceDGatewayNode);
        publishEvent(inputHandler, instanceDGatewayNode, instanceDComponentANode);
        publishEvent(inputHandler, instanceDComponentANode, instanceDComponentBNode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Set<Node> actualNodes = ServiceHolder.getModelManager().getCurrentNodes();
            Set<Edge> actualEdges = ServiceHolder.getModelManager().getCurrentEdges();
            Assert.assertEquals(actualNodes, asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode, instanceCGatewayNode, instanceCComponentANode, instanceDGatewayNode,
                    instanceDComponentANode, instanceDComponentBNode));
            Assert.assertEquals(actualEdges, asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
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
            Model model = ServiceHolder.getModelManager().getDependencyModel(startTime, endTime);
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
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(startTime, endTime, namespace);
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
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    instanceAGatewayNode.getNamespace(), instanceAGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceAGatewayNode, instanceAComponentANode,
                    instanceAComponentBNode, instanceAComponentCNode, instanceBGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceAGatewayNode, instanceAComponentANode),
                    generateEdge(instanceAGatewayNode, instanceAComponentBNode),
                    generateEdge(instanceAComponentANode, instanceAComponentCNode),
                    generateEdge(instanceAComponentCNode, instanceBGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    instanceBGatewayNode.getNamespace(), instanceBGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceBGatewayNode, instanceBComponentANode,
                    instanceBComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceBGatewayNode, instanceBComponentANode),
                    generateEdge(instanceBComponentANode, instanceBComponentBNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(startTime, endTime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
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
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    instanceCGatewayNode.getNamespace(), instanceCGatewayNode.getInstance(),
                    instanceCGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCGatewayNode, instanceCComponentANode,
                    instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCGatewayNode, instanceCComponentANode),
                    generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    instanceCComponentANode.getNamespace(), instanceCComponentANode.getInstance(),
                    instanceCComponentANode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceCComponentANode, instanceDGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceCComponentANode, instanceDGatewayNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    instanceDGatewayNode.getNamespace(), instanceDGatewayNode.getInstance(),
                    instanceDGatewayNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(instanceDGatewayNode, instanceDComponentANode,
                    instanceDComponentBNode));
            Assert.assertEquals(model.getEdges(), asSet(generateEdge(instanceDGatewayNode, instanceDComponentANode),
                    generateEdge(instanceDComponentANode, instanceDComponentBNode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(instanceCComponentANode.getNamespace(),
                    instanceCComponentANode.getInstance(), instanceCComponentANode.getComponent());
            Assert.assertEquals(actualNode, instanceCComponentANode);
        }
    }

    @Test
    public void testNamespaceSeparation() throws Exception {
        initializeSiddhiAppRuntime();
        String namespaceA = "namespace-a";
        String namespaceB = "namespace-b";
        String namespaceAInstanceA = "instance-a";
        String namespaceAInstanceB = "instance-b";
        String namespaceBInstanceA = "instance-a";
        String namespaceBInstanceC = "instance-c";
        String namespaceBInstanceD = "instance-d";
        Node namespaceAInstanceAGatewayNode = new Node(namespaceA, namespaceAInstanceA, "gateway");
        Node namespaceAInstanceAComponentANode = new Node(namespaceA, namespaceAInstanceA, "component-a");
        Node namespaceAInstanceAComponentBNode = new Node(namespaceA, namespaceAInstanceA, "component-b");
        Node namespaceAInstanceAComponentCNode = new Node(namespaceA, namespaceAInstanceA, "component-c");
        Node namespaceAInstanceBGatewayNode = new Node(namespaceA, namespaceAInstanceB, "gateway");
        Node namespaceAInstanceBComponentANode = new Node(namespaceA, namespaceAInstanceB, "component-a");
        Node namespaceAInstanceBComponentBNode = new Node(namespaceA, namespaceAInstanceB, "component-b");
        Node namespaceBInstanceAGatewayNode = new Node(namespaceB, namespaceBInstanceA, "gateway");
        Node namespaceBInstanceAComponentANode = new Node(namespaceB, namespaceBInstanceA, "component-a");
        Node namespaceBInstanceCGatewayNode = new Node(namespaceB, namespaceBInstanceC, "gateway");
        Node namespaceBInstanceCComponentANode = new Node(namespaceB, namespaceBInstanceC, "component-a");
        Node namespaceBInstanceCComponentBNode = new Node(namespaceB, namespaceBInstanceC, "component-b");
        Node namespaceBInstanceDGatewayNode = new Node(namespaceB, namespaceBInstanceD, "gateway");
        Node namespaceBInstanceDComponentANode = new Node(namespaceB, namespaceBInstanceD, "component-a");
        Node namespaceBInstanceDComponentBNode = new Node(namespaceB, namespaceBInstanceD, "component-b");

        long startTime = System.currentTimeMillis() - 100;
        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        publishEvent(inputHandler, namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentANode);
        publishEvent(inputHandler, namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentBNode);
        publishEvent(inputHandler, namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode);
        publishEvent(inputHandler, namespaceAInstanceAComponentANode, namespaceAInstanceBGatewayNode);
        publishEvent(inputHandler, namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentANode);
        publishEvent(inputHandler, namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentBNode);
        publishEvent(inputHandler, namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode);
        publishEvent(inputHandler, namespaceBInstanceAGatewayNode, namespaceBInstanceAComponentANode);
        publishEvent(inputHandler, namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentANode);
        publishEvent(inputHandler, namespaceBInstanceCGatewayNode, namespaceBInstanceCComponentBNode);
        publishEvent(inputHandler, namespaceBInstanceCComponentBNode, namespaceBInstanceDGatewayNode);
        publishEvent(inputHandler, namespaceBInstanceDGatewayNode, namespaceBInstanceDComponentANode);
        publishEvent(inputHandler, namespaceBInstanceDComponentANode, namespaceBInstanceDComponentBNode);
        long endTime = System.currentTimeMillis() + 100;

        {
            Set<Node> actualNodes = ServiceHolder.getModelManager().getCurrentNodes();
            Set<Edge> actualEdges = ServiceHolder.getModelManager().getCurrentEdges();
            Assert.assertEquals(actualNodes, asSet(namespaceAInstanceAGatewayNode, namespaceAInstanceAComponentANode,
                    namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode,
                    namespaceAInstanceBGatewayNode, namespaceAInstanceBComponentANode,
                    namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode,
                    namespaceBInstanceAComponentANode, namespaceBInstanceCGatewayNode,
                    namespaceBInstanceCComponentANode, namespaceBInstanceCComponentBNode,
                    namespaceBInstanceDGatewayNode, namespaceBInstanceDComponentANode,
                    namespaceBInstanceDComponentBNode));
            Assert.assertEquals(actualEdges, asSet(
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
            Model model = ServiceHolder.getModelManager().getDependencyModel(startTime, endTime);
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
                    namespaceAInstanceAGatewayNode.getNamespace());
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
                    namespaceBInstanceAGatewayNode.getNamespace());
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
                    namespaceAInstanceAGatewayNode.getNamespace(), namespaceAInstanceAGatewayNode.getInstance());
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
                    namespaceAInstanceBGatewayNode.getNamespace(), namespaceAInstanceBGatewayNode.getInstance());
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
                    namespaceAInstanceAComponentBNode.getNamespace(), namespaceAInstanceAComponentBNode.getInstance(),
                    namespaceAInstanceAComponentBNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceAComponentBNode,
                    namespaceAInstanceAComponentCNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceAComponentBNode, namespaceAInstanceAComponentCNode)));
        }
        {
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(startTime, endTime,
                    namespaceAInstanceBComponentBNode.getNamespace(), namespaceAInstanceBComponentBNode.getInstance(),
                    namespaceAInstanceBComponentBNode.getComponent());
            Assert.assertEquals(model.getNodes(), asSet(namespaceAInstanceBComponentBNode,
                    namespaceBInstanceAGatewayNode));
            Assert.assertEquals(model.getEdges(), asSet(
                    generateEdge(namespaceAInstanceBComponentBNode, namespaceBInstanceAGatewayNode)));
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(namespaceAInstanceAComponentANode.getNamespace(),
                    namespaceAInstanceAComponentANode.getInstance(), namespaceAInstanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, namespaceAInstanceAComponentANode);
        }
        {
            Node actualNode = ServiceHolder.getModelManager().getNode(namespaceBInstanceAComponentANode.getNamespace(),
                    namespaceBInstanceAComponentANode.getInstance(), namespaceBInstanceAComponentANode.getComponent());
            Assert.assertEquals(actualNode, namespaceBInstanceAComponentANode);
        }
    }

    @Test
    public void testReloadModelManager() throws Exception {
        Set<Node> initialNodes = ServiceHolder.getModelManager().getCurrentNodes();
        Set<Edge> initialEdges = ServiceHolder.getModelManager().getCurrentEdges();

        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());

        Assert.assertEquals(ServiceHolder.getModelManager().getCurrentNodes(), initialNodes);
        Assert.assertEquals(ServiceHolder.getModelManager().getCurrentEdges(), initialEdges);
    }

    @Test
    public void testLoadModelWithEmptyTimeRange() throws Exception {
        Model model = ServiceHolder.getModelManager().getDependencyModel(System.currentTimeMillis(),
                System.currentTimeMillis() + 10000);

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
     * @param sourceNode The source node of the request
     * @param destinationNode The destintaion node of the request
     * @throws Exception If input handler throws an exception
     */
    private void publishEvent(InputHandler inputHandler, Node sourceNode, Node destinationNode) throws Exception {
        inputHandler.send(new Object[]{sourceNode.getNamespace(), sourceNode.getInstance(),
                sourceNode.getComponent(), sourceNode.getInstanceKind(),
                destinationNode.getNamespace(), destinationNode.getInstance(),
                destinationNode.getComponent(), destinationNode.getInstanceKind()});
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
        String streamDefinitionAttributes = "sourceNamespace string, sourceInstance string, sourceComponent string, " +
                "sourceInstanceKind string, destinationNamespace string, destinationInstance string, " +
                "destinationComponent string, destinationInstanceKind string";
        String inStreamDefinition = "define stream inputStream(" + streamDefinitionAttributes + ");";
        String outStreamDefinition = "define stream outputStream(" + streamDefinitionAttributes + ");";
        String query = "@info(name = \"query\")\n" +
                "from inputStream#observe:modelGenerator(sourceNamespace, sourceInstance, sourceComponent, " +
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
