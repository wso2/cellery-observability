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

package io.cellery.observability.model.generator.model;

import io.cellery.observability.model.generator.exception.GraphStoreException;
import io.cellery.observability.model.generator.exception.ModelException;
import io.cellery.observability.model.generator.internal.ModelStoreManager;
import io.cellery.observability.model.generator.internal.ServiceHolder;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model Manager related test cases.
 */
public class ModelManagerTestCase {

    @AfterMethod
    public void cleanUp() {
        ServiceHolder.setModelStoreManager(null);
    }

    @Test
    public void testInitialization() throws Exception {
        String runtimeA = "runtime-a";
        String runtimeB = "runtime-b";
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");
        Node nodeC = new Node("namespace-b", "instance-a", "component-a");
        nodeC.setInstanceKind("Cell");
        Node nodeD = new Node("namespace-b", "instance-a", "component-a");
        nodeD.setInstanceKind("Cell");

        Edge edgeA = new Edge(nodeA, nodeB);
        Edge edgeB = new Edge(nodeA, nodeC);

        Set<Node> runtimeANodes = new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC));
        Set<Edge> runtimeAEdges = new HashSet<>(Arrays.asList(edgeA, edgeB));
        Model runtimeAModel = new Model(runtimeANodes, runtimeAEdges);

        Set<Node> runtimeBNodes = new HashSet<>(Collections.singletonList(nodeD));
        Set<Edge> runtimeBEdges = new HashSet<>(Collections.emptyList());
        Model runtimeBModel = new Model(runtimeBNodes, runtimeBEdges);

        Map<String, Model> runtimeModels = new HashMap<>();
        runtimeModels.put(runtimeA, runtimeAModel);
        runtimeModels.put(runtimeB, runtimeBModel);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModels()).thenReturn(runtimeModels);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        ModelManager modelManager = new ModelManager();
        HashMap<String, HashMap<String, Node>> nodeCache
                = Whitebox.getInternalState(modelManager, "nodeCache");

        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), runtimeModels);
        Assert.assertNotNull(nodeCache);
        Assert.assertEquals(nodeCache.size(), 2);
        {
            HashMap<String, Node> nodes = nodeCache.get(runtimeA);
            Assert.assertNotNull(nodes);
            Assert.assertEquals(nodes.size(), 3);
            Assert.assertEquals(nodes.get(nodeA.getFQN()), nodeA);
            Assert.assertEquals(nodes.get(nodeB.getFQN()), nodeB);
            Assert.assertEquals(nodes.get(nodeC.getFQN()), nodeC);
        }
        {
            HashMap<String, Node> nodes = nodeCache.get(runtimeB);
            Assert.assertNotNull(nodes);
            Assert.assertEquals(nodes.size(), 1);
            Assert.assertEquals(nodes.get(nodeD.getFQN()), nodeD);
        }
    }

    @Test
    public void testModelManagerInitializationWithNoModel() throws Exception {
        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModels()).thenReturn(null);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        ModelManager modelManager = new ModelManager();
        HashMap<String, Node> nodeCache = Whitebox.getInternalState(modelManager, "nodeCache");
        Assert.assertEquals(nodeCache.size(), 0);
    }

    @Test(expectedExceptions = ModelException.class)
    public void testInitializationWithUnexpectedInvalidEdge() throws Exception {
        String runtime = "runtime-a";
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");

        Edge edgeA = new Edge(nodeA, nodeB);

        Set<Node> nodes = Collections.emptySet();
        Set<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
        Model model = new Model(nodes, edges);
        Map<String, Model> runtimeModels = Collections.singletonMap(runtime, model);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModels()).thenReturn(runtimeModels);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test(expectedExceptions = ModelException.class)
    public void testInitializationWithUnexpectedInvalidEdgeWithNoSource() throws Exception {
        String runtime = "runtime-a";
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");

        Edge edgeA = new Edge(nodeA, nodeB);

        Set<Node> nodes = new HashSet<>(Collections.singletonList(nodeB));
        Set<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
        Model model = new Model(nodes, edges);
        Map<String, Model> runtimeModels = Collections.singletonMap(runtime, model);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModels()).thenReturn(runtimeModels);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test(expectedExceptions = ModelException.class)
    public void testInitializationWithUnexpectedInvalidEdgeWithNoTarget() throws Exception {
        String runtime = "runtime-a";
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");

        Edge edgeA = new Edge(nodeA, nodeB);

        Set<Node> nodes = new HashSet<>(Collections.singletonList(nodeA));
        Set<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
        Model model = new Model(nodes, edges);
        Map<String, Model> runtimeModels = Collections.singletonMap(runtime, model);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModels()).thenReturn(runtimeModels);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test(expectedExceptions = ModelException.class)
    public void testModelManagerInitializationWithGraphStoreException() throws Exception {
        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModels()).thenThrow(new GraphStoreException("Test Exception"));
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test
    public void testGetNonExistentNode() throws Exception {
        String runtime = "runtime-a";
        ModelManager modelManager = initEmptyModelManager();
        Node node = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(runtime, node);

        Node retrievedNode = modelManager.getNode(runtime, node.getNamespace(), node.getInstance(),
                "different-component");
        Assert.assertNull(retrievedNode);
    }

    @Test
    public void testGetNodeFromCache() throws Exception {
        String runtime = "test-runtime";
        ModelManager modelManager = initEmptyModelManager();
        HashMap<String, HashMap<String, Node>> nodeCache = Mockito.spy(
                Whitebox.<HashMap<String, HashMap<String, Node>>>getInternalState(modelManager, "nodeCache"));
        Whitebox.setInternalState(modelManager, "nodeCache", nodeCache);

        Node node = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(runtime, node);

        Node retrievedNode = modelManager.getNode(runtime, node.getNamespace(), node.getInstance(),
                node.getComponent());
        Mockito.verify(nodeCache, Mockito.times(2))
                .computeIfAbsent(Mockito.eq(runtime), Mockito.any());
        Assert.assertEquals(retrievedNode, node);
    }

    @Test
    public void testGetOrGenerateNodeWithExistingNode() throws Exception {
        String runtime = "test-runtime";
        ModelManager modelManager = initEmptyModelManager();
        Node node = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(runtime, node);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Collections.singletonList(node)), Collections.emptySet())));

        Node retrievedNode = modelManager.getOrGenerateNode(runtime, node.getNamespace(), node.getInstance(),
                node.getComponent());
        Assert.assertEquals(retrievedNode, node);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Collections.singletonList(node)), Collections.emptySet())));
    }

    @Test
    public void testGetOrGenerateNodeWithNonExistingNode() throws Exception {
        String runtime = "test-runtime";
        ModelManager modelManager = initEmptyModelManager();
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(runtime, nodeA);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Collections.singletonList(nodeA)), Collections.emptySet())));

        Node nodeB = new Node("test-namespace", "test-instance", "different-component");
        Node retrievedNode = modelManager.getOrGenerateNode(runtime, nodeB.getNamespace(),
                nodeB.getInstance(), nodeB.getComponent());
        Assert.assertEquals(retrievedNode, nodeB);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Arrays.asList(nodeA, nodeB)), Collections.emptySet())));
    }

    @Test
    public void testAddNode() throws Exception {
        String runtime = "test-runtime";
        ModelManager modelManager = initEmptyModelManager();

        Node nodeA = new Node("test-namespace", "test-instance-a", "test-component");
        modelManager.addNode(runtime, nodeA);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Collections.singletonList(nodeA)), Collections.emptySet())));

        Node nodeB = new Node("test-namespace", "test-instance-b", "test-component");
        modelManager.addNode(runtime, nodeB);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Arrays.asList(nodeA, nodeB)), Collections.emptySet())));

        // Duplicate node
        Node nodeC = new Node("test-namespace", "test-instance-a", "test-component");
        modelManager.addNode(runtime, nodeC);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Arrays.asList(nodeA, nodeB)), Collections.emptySet())));
    }

    @Test
    public void testAddEdge() throws Exception {
        String runtime = "test-runtime";
        ModelManager modelManager = initEmptyModelManager();

        Node nodeA = new Node("test-namespace", "test-instance-a", "test-component");
        Node nodeB = new Node("test-namespace", "test-instance-b", "test-component");
        modelManager.addEdge(runtime, nodeA, nodeB);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Arrays.asList(nodeA, nodeB)),
                        new HashSet<>(Collections.singletonList(generateEdge(nodeA, nodeB))))));

        Node nodeC = new Node("test-namespace", "test-instance-c", "test-component");
        Node nodeD = new Node("test-namespace", "test-instance-d", "test-component");
        modelManager.addEdge(runtime, nodeC, nodeD);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)),
                        new HashSet<>(Arrays.asList(generateEdge(nodeA, nodeB),
                                generateEdge(nodeC, nodeD))))));

        // Duplicate Edge
        Node nodeE = new Node("test-namespace", "test-instance-a", "test-component");
        Node nodeF = new Node("test-namespace", "test-instance-b", "test-component");
        modelManager.addEdge(runtime, nodeE, nodeF);
        Assert.assertEquals(modelManager.getCurrentRuntimeModels(), Collections.singletonMap(runtime,
                new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)),
                        new HashSet<>(Arrays.asList(generateEdge(nodeA, nodeB), generateEdge(nodeC, nodeD))))));
    }

    @Test
    public void testGetDependencyModel() throws Exception {
        String runtime = "test-runtime";
        long startTime = 12312312;
        long endTime = 12315312;

        Node nodeA = new Node("test-namespace", "test-instance-a", "test-component");
        Node nodeB = new Node("test-namespace", "test-instance-b", "test-component");
        Node nodeC = new Node("test-namespace", "test-instance-c", "test-component");
        Node nodeD = new Node("test-namespace", "test-instance-d", "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeC, nodeD);

        List<Model> models = new ArrayList<>();
        models.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)),
                new HashSet<>(Arrays.asList(edgeA, edgeB))));
        models.add(new Model(new HashSet<>(Arrays.asList(nodeC, nodeD)),
                new HashSet<>(Collections.singletonList(edgeC))));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtime)).thenReturn(models);

        Model retrievedModel = modelManager.getRuntimeDependencyModel(startTime, endTime, runtime);
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB, edgeC)));
    }

    @Test
    public void testGetRuntimeDependencyModel() throws Exception {
        long startTime = 12312335;
        long endTime = 12315335;

        String runtimeA = "test-runtime-a";
        String runtimeB = "test-runtime-B";
        String namespaceA = "test-namespace-a";
        String namespaceB = "test-namespace-b";
        Node nodeA = new Node(namespaceA, "test-instance-a", "test-component");
        Node nodeB = new Node(namespaceA, "test-instance-b", "test-component");
        Node nodeC = new Node(namespaceB, "test-instance-a", "test-component");
        Node nodeD = new Node(namespaceB, "test-instance-b", "test-component");
        Node nodeE = new Node(namespaceB, "test-instance-c", "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeD, nodeE);

        List<Model> runtimeAModels = new ArrayList<>();
        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB)),
                new HashSet<>(Collections.singletonList(edgeA))));
        runtimeAModels.add(new Model(new HashSet<>(Collections.singletonList(nodeC)),
                new HashSet<>(Collections.singletonList(edgeB))));

        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeC)),
                new HashSet<>(Collections.singletonList(edgeB))));

        List<Model> runtimeBModels = new ArrayList<>();
        runtimeBModels.add(new Model(new HashSet<>(Arrays.asList(nodeD, nodeE)),
                new HashSet<>(Collections.singletonList(edgeC))));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeA))
                .thenReturn(runtimeAModels);
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeB))
                .thenReturn(runtimeBModels);

        Model retrievedModel = modelManager.getNamespaceDependencyModel(startTime, endTime, runtimeA,
                nodeA.getNamespace());
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB)));
    }

    @Test
    public void testGetNamespaceDependencyModel() throws Exception {
        long startTime = 12312335;
        long endTime = 12315335;

        String runtimeA = "test-runtime-a";
        String runtimeB = "test-runtime-b";
        String namespaceA = "test-namespace-a";
        String namespaceB = "test-namespace-b";
        Node nodeA = new Node(namespaceA, "test-instance-a", "test-component");
        Node nodeB = new Node(namespaceA, "test-instance-b", "test-component");
        Node nodeC = new Node(namespaceB, "test-instance-a", "test-component");
        Node nodeD = new Node(namespaceB, "test-instance-b", "test-component");
        Node nodeE = new Node(namespaceB, "test-instance-c", "test-component");
        Node nodeF = new Node(namespaceA, "test-instance-a", "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeC, nodeD);
        Edge edgeD = generateEdge(nodeD, nodeE);

        List<Model> runtimeAModels = new ArrayList<>();
        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)),
                new HashSet<>(Arrays.asList(edgeA, edgeB))));
        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeC, nodeD, nodeE)),
                new HashSet<>(Arrays.asList(edgeC, edgeD))));

        List<Model> runtimeBModels = new ArrayList<>();
        runtimeBModels.add(new Model(new HashSet<>(Collections.singletonList(nodeF)),
                new HashSet<>(Collections.emptyList())));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeA))
                .thenReturn(runtimeAModels);
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeB))
                .thenReturn(runtimeBModels);

        Model retrievedModel = modelManager.getNamespaceDependencyModel(startTime, endTime, runtimeA,
                nodeA.getNamespace());
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB)));
    }

    @Test
    public void testGetInstanceDependencyModel() throws Exception {
        long startTime = 12312324;
        long endTime = 12315324;

        String runtimeA = "test-runtime-a";
        String runtimeB = "test-runtime-b";
        String namespaceA = "test-namespace-a";
        String namespaceB = "test-namespace-b";
        Node nodeA = new Node(namespaceA, "test-instance-a", "test-gateway");
        Node nodeB = new Node(namespaceA, "test-instance-a", "test-component-a");
        Node nodeC = new Node(namespaceA, "test-instance-b", "test-component");
        Node nodeD = new Node(namespaceA, "test-instance-c", "test-component");
        Node nodeE = new Node(namespaceA, "test-instance-d", "test-component");
        Node nodeF = new Node(namespaceB, "test-instance-a", "test-component");
        Node nodeG = new Node(namespaceA, "test-instance-a", "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeC, nodeD);
        Edge edgeD = generateEdge(nodeD, nodeE);
        Edge edgeE = generateEdge(nodeB, nodeF);

        List<Model> runtimeAModels = new ArrayList<>();
        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeF)),
                new HashSet<>(Arrays.asList(edgeA, edgeB, edgeE))));
        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeC, nodeD, nodeE)),
                new HashSet<>(Arrays.asList(edgeC, edgeD))));

        List<Model> runtimeBModels = new ArrayList<>();
        runtimeBModels.add(new Model(new HashSet<>(Collections.singletonList(nodeG)),
                new HashSet<>(Collections.emptyList())));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeA))
                .thenReturn(runtimeAModels);
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeB))
                .thenReturn(runtimeBModels);

        Model retrievedModel = modelManager.getInstanceDependencyModel(startTime, endTime, runtimeA,
                nodeA.getNamespace(), nodeA.getInstance());
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeF)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB, edgeE)));
    }

    @Test
    public void testGetComponentDependencyModel() throws Exception {
        long startTime = 12312312;
        long endTime = 12315312;

        String runtimeA = "test-runtime-a";
        String runtimeB = "test-runtime-b";
        String namespaceA = "test-namespace-a";
        String namespaceB = "test-namespace-b";
        String instanceA = "test-instance-a";
        Node nodeA = new Node(namespaceA, instanceA, "test-gateway");
        Node nodeB = new Node(namespaceA, instanceA, "test-component-a");
        Node nodeC = new Node(namespaceA, "test-instance-b", "test-component");
        Node nodeD = new Node(namespaceA, "test-instance-c", "test-component");
        Node nodeE = new Node(namespaceA, "test-instance-d", "test-component");
        Node nodeF = new Node(namespaceB, instanceA, "test-component");
        Node nodeG = new Node(namespaceA, instanceA, "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeC, nodeD);
        Edge edgeD = generateEdge(nodeD, nodeE);
        Edge edgeE = generateEdge(nodeA, nodeF);

        List<Model> runtimeAModels = new ArrayList<>();
        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeF)),
                new HashSet<>(Arrays.asList(edgeA, edgeB, edgeE))));
        runtimeAModels.add(new Model(new HashSet<>(Arrays.asList(nodeC, nodeD, nodeE)),
                new HashSet<>(Arrays.asList(edgeC, edgeD))));

        List<Model> runtimeBModels = new ArrayList<>();
        runtimeBModels.add(new Model(new HashSet<>(Collections.singletonList(nodeG)),
                new HashSet<>(Collections.emptyList())));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeA))
                .thenReturn(runtimeAModels);
        Mockito.when(ServiceHolder.getModelStoreManager().loadModels(startTime, endTime, runtimeB))
                .thenReturn(runtimeBModels);

        Model retrievedModel = modelManager.getComponentDependencyModel(startTime, endTime, runtimeA,
                nodeA.getNamespace(), nodeA.getInstance(), nodeA.getComponent());
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeF)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB, edgeE)));
    }

    /**
     * Generate an edge using source and target node.
     *
     * @param sourceNode The source node of the edge
     * @param targetNode The target node of the edge
     * @return The generated edge
     */
    private Edge generateEdge(Node sourceNode, Node targetNode) {
        return new Edge(
                new EdgeNode(sourceNode.getNamespace(), sourceNode.getInstance(),
                        sourceNode.getComponent()),
                new EdgeNode(targetNode.getNamespace(), targetNode.getInstance(),
                        targetNode.getComponent())
        );
    }

    /**
     * Initialize an empty model manager.
     *
     * @return The initialized model manager
     * @throws Exception if initializing fails
     */
    private ModelManager initEmptyModelManager() throws Exception {
        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModels()).thenReturn(Collections.emptyMap());
        ServiceHolder.setModelStoreManager(modelStoreManager);
        return new ModelManager();
    }
}
