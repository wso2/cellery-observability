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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/**
 * Model related test cases.
 */
public class ModelTestCase {

    @Test
    public void testNodeFQNEquality() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        Assert.assertEquals(nodeA.getFQN(), nodeB.getFQN());
    }

    @Test
    public void testNodeFQNInEqualityWithMismatchedNamespace() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "different-namespace", "test-instance", "test-component");
        Assert.assertNotEquals(nodeA.getFQN(), nodeB.getFQN());
    }

    @Test
    public void testNodeFQNInEqualityWithMismatchedInstance() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "test-namespace", "different-instance", "test-component");
        Assert.assertNotEquals(nodeA.getFQN(), nodeB.getFQN());
    }

    @Test
    public void testNodeFQNInEqualityWithMismatchedComponent() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "test-namespace", "test-instance", "different-component");
        Assert.assertNotEquals(nodeA.getFQN(), nodeB.getFQN());
    }

    @Test
    public void testGetNodeFQNWithSeparateParameters() {
        Node nodeA = new Node("test-runtime", "test-namespace", "test-instance", "test-component");
        nodeA.setInstanceKind("Cell");
        Assert.assertEquals(Model.getNodeFQN(nodeA.getRuntime(), nodeA.getNamespace(), nodeA.getInstance(),
                nodeA.getComponent()), Model.getNodeFQN(nodeA));
    }

    @Test
    public void testModelEquality() {
        Model modelA;
        {
            Node nodeA = new Node("test-runtime", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("test-runtime", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("test-runtime", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");
            Node nodeD = new Node("test-runtime", "namespace-b", "instance-a", "component-a");
            nodeD.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);
            Edge edgeB = new Edge(nodeA, nodeC);
            Edge edgeC = new Edge(nodeB, nodeD);

            modelA = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)),
                    new HashSet<>(Arrays.asList(edgeA, edgeB, edgeC)));
        }
        Model modelB;
        {
            Node nodeA = new Node("test-runtime", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("test-runtime", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("test-runtime", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");
            Node nodeD = new Node("test-runtime", "namespace-b", "instance-a", "component-a");
            nodeD.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);
            Edge edgeB = new Edge(nodeA, nodeC);
            Edge edgeC = new Edge(nodeB, nodeD);

            modelB = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC, nodeD)),
                    new HashSet<>(Arrays.asList(edgeA, edgeB, edgeC)));
        }
        Assert.assertEquals(modelA, modelB);
    }

    @Test
    public void testModelInEqualityWithMismatchedNodes() {
        Model modelA;
        {
            Node nodeA = new Node("test-runtime", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("test-runtime", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("test-runtime", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);

            modelA = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)),
                    new HashSet<>(Collections.singletonList(edgeA)));
        }
        Model modelB;
        {
            Node nodeA = new Node("test-runtime", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("test-runtime", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");

            Edge edgeA = new Edge(nodeA, nodeB);

            modelB = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB)),
                    new HashSet<>(Collections.singletonList(edgeA)));
        }
        Assert.assertNotEquals(modelA, modelB);
    }

    @Test
    public void testModelInEqualityWithMismatchedEdges() {
        Model modelA;
        {
            Node nodeA = new Node("test-runtime", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("test-runtime", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("test-runtime", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);
            Edge edgeB = new Edge(nodeA, nodeC);

            modelA = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)),
                    new HashSet<>(Arrays.asList(edgeA, edgeB)));
        }
        Model modelB;
        {
            Node nodeA = new Node("test-runtime", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("test-runtime", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("test-runtime", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);

            modelB = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)),
                    new HashSet<>(Collections.singletonList(edgeA)));
        }
        Assert.assertNotEquals(modelA, modelB);
    }
}
