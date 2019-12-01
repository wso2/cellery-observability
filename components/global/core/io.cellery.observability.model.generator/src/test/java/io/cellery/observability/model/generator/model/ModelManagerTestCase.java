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
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");
        Node nodeC = new Node("namespace-b", "instance-a", "component-a");
        nodeC.setInstanceKind("Cell");

        Edge edgeA = new Edge(nodeA, nodeB);
        Edge edgeB = new Edge(nodeA, nodeC);

        HashSet<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC));
        HashSet<Edge> edges = new HashSet<>(Arrays.asList(edgeA, edgeB));
        Model model = new Model(nodes, edges);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModel()).thenReturn(model);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        ModelManager modelManager = new ModelManager();
        HashMap<String, Node> nodeCache = Whitebox.getInternalState(modelManager, "nodeCache");

        Assert.assertEquals(modelManager.getCurrentNodes(), model.getNodes());
        Assert.assertEquals(modelManager.getCurrentEdges(), model.getEdges());
        Assert.assertEquals(nodeCache.size(), 3);
        Assert.assertEquals(nodeCache.get(nodeA.getFQN()), nodeA);
        Assert.assertEquals(nodeCache.get(nodeB.getFQN()), nodeB);
        Assert.assertEquals(nodeCache.get(nodeC.getFQN()), nodeC);
    }

    @Test
    public void testModelManagerInitializationWithNoModel() throws Exception {
        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModel()).thenReturn(null);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        ModelManager modelManager = new ModelManager();
        HashMap<String, Node> nodeCache = Whitebox.getInternalState(modelManager, "nodeCache");
        Assert.assertEquals(nodeCache.size(), 0);
    }

    @Test(expectedExceptions = ModelException.class)
    public void testInitializationWithUnexpectedInvalidEdge() throws Exception {
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");

        Edge edgeA = new Edge(nodeA, nodeB);

        Set<Node> nodes = Collections.emptySet();
        Set<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
        Model model = new Model(nodes, edges);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModel()).thenReturn(model);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test(expectedExceptions = ModelException.class)
    public void testInitializationWithUnexpectedInvalidEdgeWithNoSource() throws Exception {
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");

        Edge edgeA = new Edge(nodeA, nodeB);

        HashSet<Node> nodes = new HashSet<>(Collections.singletonList(nodeB));
        HashSet<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
        Model model = new Model(nodes, edges);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModel()).thenReturn(model);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test(expectedExceptions = ModelException.class)
    public void testInitializationWithUnexpectedInvalidEdgeWithNoTarget() throws Exception {
        Node nodeA = new Node("namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");

        Edge edgeA = new Edge(nodeA, nodeB);

        HashSet<Node> nodes = new HashSet<>(Collections.singletonList(nodeA));
        HashSet<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
        Model model = new Model(nodes, edges);

        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModel()).thenReturn(model);
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test(expectedExceptions = ModelException.class)
    public void testModelManagerInitializationWithGraphStoreException() throws Exception {
        ModelStoreManager modelStoreManager = Mockito.mock(ModelStoreManager.class);
        Mockito.when(modelStoreManager.loadLastModel()).thenThrow(new GraphStoreException("Test Exception"));
        ServiceHolder.setModelStoreManager(modelStoreManager);

        new ModelManager();
    }

    @Test
    public void testGetNonExistentNode() throws Exception {
        ModelManager modelManager = initEmptyModelManager();
        Node node = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(node);

        Node retrievedNode = modelManager.getNode(node.getNamespace(), node.getInstance(), "different-component");
        Assert.assertNull(retrievedNode);
    }

    @Test
    public void testGetNodeFromCache() throws Exception {
        ModelManager modelManager = initEmptyModelManager();
        HashMap<String, Node> nodeCache = Mockito.spy(
                Whitebox.<HashMap<String, Node>>getInternalState(modelManager, "nodeCache"));
        Whitebox.setInternalState(modelManager, "nodeCache", nodeCache);

        Node node = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(node);

        Node retrievedNode = modelManager.getNode(node.getNamespace(), node.getInstance(), node.getComponent());
        Mockito.verify(nodeCache, Mockito.times(1)).get(Mockito.eq(Model.getNodeFQN(node)));
        Assert.assertEquals(retrievedNode, node);
    }

    @Test
    public void testGetOrGenerateNodeWithExistingNode() throws Exception {
        ModelManager modelManager = initEmptyModelManager();
        Node node = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(node);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Collections.singletonList(node)));
        Assert.assertEquals(modelManager.getCurrentEdges(), Collections.emptySet());

        Node retrievedNode = modelManager.getOrGenerateNode(node.getNamespace(), node.getInstance(),
                node.getComponent());
        Assert.assertEquals(retrievedNode, node);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Collections.singletonList(node)));
        Assert.assertEquals(modelManager.getCurrentEdges(), Collections.emptySet());
    }

    @Test
    public void testGetOrGenerateNodeWithNonExistingNode() throws Exception {
        ModelManager modelManager = initEmptyModelManager();
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        modelManager.addNode(nodeA);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Collections.singletonList(nodeA)));
        Assert.assertEquals(modelManager.getCurrentEdges(), Collections.emptySet());

        Node nodeB = new Node("test-namespace", "test-instance", "different-component");
        Node retrievedNode = modelManager.getOrGenerateNode(nodeB.getNamespace(), nodeB.getInstance(),
                nodeB.getComponent());
        Assert.assertEquals(retrievedNode, nodeB);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB)));
        Assert.assertEquals(modelManager.getCurrentEdges(), Collections.emptySet());
    }

    @Test
    public void testAddNode() throws Exception {
        ModelManager modelManager = initEmptyModelManager();

        Node nodeA = new Node("test-namespace", "test-instance-a", "test-component");
        modelManager.addNode(nodeA);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Collections.singletonList(nodeA)));
        Assert.assertEquals(modelManager.getCurrentEdges(), Collections.emptySet());

        Node nodeB = new Node("test-namespace", "test-instance-b", "test-component");
        modelManager.addNode(nodeB);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB)));
        Assert.assertEquals(modelManager.getCurrentEdges(), Collections.emptySet());

        // Duplicate node
        Node nodeC = new Node("test-namespace", "test-instance-a", "test-component");
        modelManager.addNode(nodeC);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB)));
        Assert.assertEquals(modelManager.getCurrentEdges(), Collections.emptySet());
    }

    @Test
    public void testAddEdge() throws Exception {
        ModelManager modelManager = initEmptyModelManager();

        Node nodeA = new Node("test-namespace", "test-instance-a", "test-component");
        Node nodeB = new Node("test-namespace", "test-instance-b", "test-component");
        modelManager.addEdge(nodeA, nodeB);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB)));
        Assert.assertEquals(modelManager.getCurrentEdges(), new HashSet<>(Collections.singletonList(
                new Edge(nodeA, nodeB))));

        Node nodeC = new Node("test-namespace", "test-instance-c", "test-component");
        Node nodeD = new Node("test-namespace", "test-instance-d", "test-component");
        modelManager.addEdge(nodeC, nodeD);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)));
        Assert.assertEquals(modelManager.getCurrentEdges(), new HashSet<>(Arrays.asList(new Edge(nodeA, nodeB),
                new Edge(nodeC, nodeD))));

        // Duplicate Edge
        Node nodeE = new Node("test-namespace", "test-instance-a", "test-component");
        Node nodeF = new Node("test-namespace", "test-instance-b", "test-component");
        modelManager.addEdge(nodeE, nodeF);
        Assert.assertEquals(modelManager.getCurrentNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)));
        Assert.assertEquals(modelManager.getCurrentEdges(), new HashSet<>(Arrays.asList(new Edge(nodeA, nodeB),
                new Edge(nodeC, nodeD))));
    }

    @Test
    public void testGetDependencyModel() throws Exception {
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
        Mockito.when(ServiceHolder.getModelStoreManager().loadModel(startTime, endTime)).thenReturn(models);

        Model retrievedModel = modelManager.getDependencyModel(startTime, endTime);
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB, edgeC)));
    }

    @Test
    public void testGetNamespaceDependencyModel() throws Exception {
        long startTime = 12312335;
        long endTime = 12315335;

        String namespaceA = "test-namespace-a";
        String namespaceB = "test-namespace-b";
        Node nodeA = new Node(namespaceA, "test-instance-a", "test-component");
        Node nodeB = new Node(namespaceA, "test-instance-b", "test-component");
        Node nodeC = new Node(namespaceB, "test-instance-a", "test-component");
        Node nodeD = new Node(namespaceB, "test-instance-b", "test-component");
        Node nodeE = new Node(namespaceB, "test-instance-c", "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeC, nodeD);
        Edge edgeD = generateEdge(nodeD, nodeE);

        List<Model> models = new ArrayList<>();
        models.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)),
                new HashSet<>(Arrays.asList(edgeA, edgeB))));
        models.add(new Model(new HashSet<>(Arrays.asList(nodeC, nodeD, nodeE)),
                new HashSet<>(Arrays.asList(edgeC, edgeD))));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModel(startTime, endTime)).thenReturn(models);

        Model retrievedModel = modelManager.getNamespaceDependencyModel(startTime, endTime, nodeA.getNamespace());
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB)));
    }

    @Test
    public void testGetInstanceDependencyModel() throws Exception {
        long startTime = 12312324;
        long endTime = 12315324;

        String namespaceA = "test-namespace-a";
        String namespaceB = "test-namespace-b";
        Node nodeA = new Node(namespaceA, "test-instance-a", "test-gateway");
        Node nodeB = new Node(namespaceA, "test-instance-a", "test-component-a");
        Node nodeC = new Node(namespaceA, "test-instance-b", "test-component");
        Node nodeD = new Node(namespaceA, "test-instance-c", "test-component");
        Node nodeE = new Node(namespaceA, "test-instance-d", "test-component");
        Node nodeF = new Node(namespaceB, "test-instance-a", "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeC, nodeD);
        Edge edgeD = generateEdge(nodeD, nodeE);
        Edge edgeE = generateEdge(nodeB, nodeF);

        List<Model> models = new ArrayList<>();
        models.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeF)),
                new HashSet<>(Arrays.asList(edgeA, edgeB, edgeE))));
        models.add(new Model(new HashSet<>(Arrays.asList(nodeC, nodeD, nodeE)),
                new HashSet<>(Arrays.asList(edgeC, edgeD))));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModel(startTime, endTime)).thenReturn(models);

        Model retrievedModel = modelManager.getInstanceDependencyModel(startTime, endTime, nodeA.getNamespace(),
                nodeA.getInstance());
        Assert.assertEquals(retrievedModel.getNodes(), new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeF)));
        Assert.assertEquals(retrievedModel.getEdges(), new HashSet<>(Arrays.asList(edgeA, edgeB, edgeE)));
    }

    @Test
    public void testGetComponentDependencyModel() throws Exception {
        long startTime = 12312312;
        long endTime = 12315312;

        String namespaceA = "test-namespace-a";
        String namespaceB = "test-namespace-b";
        String instanceA = "test-instance-a";
        Node nodeA = new Node(namespaceA, instanceA, "test-gateway");
        Node nodeB = new Node(namespaceA, instanceA, "test-component-a");
        Node nodeC = new Node(namespaceA, "test-instance-b", "test-component");
        Node nodeD = new Node(namespaceA, "test-instance-c", "test-component");
        Node nodeE = new Node(namespaceA, "test-instance-d", "test-component");
        Node nodeF = new Node(namespaceB, "test-instance-a", "test-component");

        Edge edgeA = generateEdge(nodeA, nodeB);
        Edge edgeB = generateEdge(nodeA, nodeC);
        Edge edgeC = generateEdge(nodeC, nodeD);
        Edge edgeD = generateEdge(nodeD, nodeE);
        Edge edgeE = generateEdge(nodeA, nodeF);

        List<Model> models = new ArrayList<>();
        models.add(new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeF)),
                new HashSet<>(Arrays.asList(edgeA, edgeB, edgeE))));
        models.add(new Model(new HashSet<>(Arrays.asList(nodeC, nodeD, nodeE)),
                new HashSet<>(Arrays.asList(edgeC, edgeD))));

        ModelManager modelManager = initEmptyModelManager();
        Mockito.when(ServiceHolder.getModelStoreManager().loadModel(startTime, endTime)).thenReturn(models);

        Model retrievedModel = modelManager.getComponentDependencyModel(startTime, endTime, nodeA.getNamespace(),
                nodeA.getInstance(), nodeA.getComponent());
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
                new EdgeNode(sourceNode.getNamespace(), sourceNode.getInstance(), sourceNode.getComponent()),
                new EdgeNode(targetNode.getNamespace(), targetNode.getInstance(), targetNode.getComponent())
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
        Mockito.when(modelStoreManager.loadLastModel()).thenReturn(new Model(Collections.emptySet(),
                Collections.emptySet()));
        ServiceHolder.setModelStoreManager(modelStoreManager);
        return new ModelManager();
    }
}
