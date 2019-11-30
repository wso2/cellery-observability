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

import java.util.Set;

/**
 * Represents a dependency model between components in the system.
 */
public class Model {
    private Set<Node> nodes;
    private Set<Edge> edges;

    private static final String NODE_FQN_SEPARATOR = "#";

    public Model(Set<Node> nodes, Set<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public Set<Edge> getEdges() {
        return edges;
    }

    public static String getNodeFQN(EdgeNode node) {
        return Model.getNodeFQN(node.getNamespace(), node.getInstance(), node.getComponent());
    }

    public static String getNodeFQN(String namespace, String instance, String component) {
        return namespace + NODE_FQN_SEPARATOR + instance + NODE_FQN_SEPARATOR + component;
    }
}
