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
 * Edge related test cases.
 */
public class EdgeTestCase {

    @Test
    public void testEquality() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Assert.assertEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedSourceRuntime() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("different-runtime", "destination-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedSourceNamespace() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "different-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedSourceInstance() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "different-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedSourceComponent() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "different-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedDestinationRuntime() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("different-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedDestinationNamespace() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "different-namespace", "destination-instance",
                        "destination-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedDestinationInstance() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "different-instance",
                        "destination-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }

    @Test
    public void testInEqualityWithMismatchedDestinationComponent() {
        Edge edgeA = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "destination-component")
        );
        Edge edgeB = new Edge(
                new EdgeNode("source-runtime", "source-namespace", "source-instance", "source-component"),
                new EdgeNode("destination-runtime", "destination-namespace", "destination-instance",
                        "different-component")
        );
        Assert.assertNotEquals(edgeA, edgeB);
    }
}
