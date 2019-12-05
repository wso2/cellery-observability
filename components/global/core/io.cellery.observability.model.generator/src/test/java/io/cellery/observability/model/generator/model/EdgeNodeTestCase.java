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
 * Edge Note related test cases.
 */
public class EdgeNodeTestCase {

    @Test
    public void testNodeEquality() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        Assert.assertEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeInEqualityWithMismatchedRuntime() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("different-runtime", "test-namespace", "test-instance", "test-component");
        Assert.assertNotEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeInEqualityWithMismatchedNamespace() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "different-namespace", "test-instance", "test-component");
        Assert.assertNotEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeInEqualityWithMismatchedInstance() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "test-namespace", "different-instance", "test-component");
        Assert.assertNotEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeInEqualityWithMismatchedComponent() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        EdgeNode nodeB = new EdgeNode("test-runtime", "test-namespace", "test-instance", "different-component");
        Assert.assertNotEquals(nodeA, nodeB);
    }

    @Test
    public void testNodeFQN() {
        EdgeNode nodeA = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        Assert.assertEquals(Model.getNodeFQN(nodeA), nodeA.getFQN());

        EdgeNode nodeB = new EdgeNode("test-runtime", "test-namespace", "test-instance", "test-component");
        Assert.assertEquals(Model.getNodeFQN(nodeB), nodeB.getFQN());
    }
}
