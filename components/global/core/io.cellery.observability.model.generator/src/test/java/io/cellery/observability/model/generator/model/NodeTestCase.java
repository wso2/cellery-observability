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

/**
 * Node related test cases.
 */
public class NodeTestCase {

    @Test
    public void testNodeEquality() {
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("test-namespace", "test-instance", "test-component");
        nodeB.setInstanceKind("Cell");
        Assert.assertEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeEqualityWithMismatchedInstanceKind() {
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("test-namespace", "test-instance", "test-component");
        nodeB.setInstanceKind("Composite");
        Assert.assertEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeInEqualityWithMismatchedNamespace() {
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("different-namespace", "test-instance", "test-component");
        nodeB.setInstanceKind("Cell");
        Assert.assertNotEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeInEqualityWithMismatchedInstance() {
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("test-namespace", "different-instance", "test-component");
        nodeB.setInstanceKind("Cell");
        Assert.assertNotEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeInEqualityWithMismatchedComponent() {
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("test-namespace", "test-instance", "different-component");
        nodeB.setInstanceKind("Cell");
        Assert.assertNotEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeFQN() {
        Node nodeA = new Node("test-namespace", "test-instance", "test-component");
        nodeA.setInstanceKind("Cell");
        Assert.assertEquals(Model.getNodeFQN(nodeA), nodeA.getFQN());

        Node nodeB = new Node("test-namespace", "test-instance", "test-component");
        nodeB.setInstanceKind("Composite");
        Assert.assertEquals(Model.getNodeFQN(nodeB), nodeB.getFQN());
    }
}
