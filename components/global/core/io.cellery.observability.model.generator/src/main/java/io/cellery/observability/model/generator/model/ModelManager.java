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

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import io.cellery.observability.model.generator.exception.GraphStoreException;
import io.cellery.observability.model.generator.exception.ModelException;
import io.cellery.observability.model.generator.internal.ServiceHolder;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This is the Manager, singleton class which performs the operations in the in memory dependency tree.
 */
public class ModelManager {
    private MutableNetwork<Node, Edge> dependencyGraph;
    private Map<String, Node> nodeCache = new HashMap<>();

    public ModelManager() throws ModelException {
        try {
            this.dependencyGraph = NetworkBuilder.directed()
                    .allowsParallelEdges(true)
                    .expectedNodeCount(100000)
                    .expectedEdgeCount(1000000)
                    .build();

            Model model = ServiceHolder.getModelStoreManager().loadLastModel();
            if (model != null) {
                addNodes(model.getNodes());
                addEdges(model.getEdges());
            }
        } catch (GraphStoreException e) {
            throw new ModelException("Unable to load already persisted model", e);
        }
    }

    /**
     * Add a set of nodes to the dependency model.
     *
     * @param nodes The set of nodes to be added
     */
    private void addNodes(Set<Node> nodes) {
        for (Node node : nodes) {
            this.addNode(node);
        }
    }

    /**
     * Add a set of edges to the dependency model.
     * Expects that the relevant nodes had been already added.
     *
     * @param edges The set of edges to be added.
     * @throws ModelException If source or target node was not added before adding a particular edge
     */
    private void addEdges(Set<Edge> edges) throws ModelException {
        for (Edge edge : edges) {
            Node sourceNode = getNode(edge.getSource().getNamespace(), edge.getSource().getInstance(),
                    edge.getSource().getComponent());
            Node targetNode = getNode(edge.getTarget().getNamespace(), edge.getTarget().getInstance(),
                    edge.getTarget().getComponent());
            if (sourceNode != null && targetNode != null) {
                this.dependencyGraph.addEdge(sourceNode, targetNode, edge);
            } else {
                String msg = "";
                if (sourceNode == null) {
                    msg += "Source node doesn't exist in the graph for edge :" + edge;
                }
                if (targetNode == null) {
                    msg += ("".equals(msg) ? "" : ". ") + "Target node doesn't exist in the graph for edge :" + edge;
                }
                throw new ModelException(msg);
            }
        }
    }

    /**
     * Get a node representing a component belonging to an instance in a particular namespace.
     *
     * @param namespace The namespace the instance belongs to
     * @param instance The name of the instance the component belongs to
     * @param component The name of the component
     * @return The node in the dependency graph or null if not present
     */
    public Node getNode(String namespace, String instance, String component) {
        String nodeFQN = Model.getNodeFQN(namespace, instance, component);
        Node cachedNode = this.nodeCache.get(nodeFQN);
        if (cachedNode == null) {
            Optional<Node> requiredNode = this.dependencyGraph.nodes()
                    .stream()
                    .filter(node -> Objects.equals(node.getFQN(), nodeFQN))
                    .findAny();
            if (requiredNode.isPresent()) {
                this.nodeCache.put(requiredNode.get().getFQN(), requiredNode.get());
                return requiredNode.get();
            } else {
                return null;
            }
        }
        return cachedNode;
    }

    /**
     * Get the existing node if it exists or generate a new node in the dependency graph.
     *
     * @param namespace The namespace the instance belongs to
     * @param instance The name of the instance the component belongs to
     * @param component The name of the component
     * @return The node in the dependency graph or the generated node
     */
    public Node getOrGenerateNode(String namespace, String instance, String component) {
        Node node = this.getNode(namespace, instance, component);
        if (node == null) {
            node = new Node(namespace, instance, component);
            this.addNode(node);
        }
        return node;
    }

    /**
     * Add a node to the dependency graph.
     *
     * @param node The node to be added.
     */
    public void addNode(Node node) {
        this.dependencyGraph.addNode(node);
        this.nodeCache.put(node.getFQN(), node);
    }

    /**
     * Add an edge to the dependency graph.
     *
     * @param source The source node of the edge
     * @param target The target node of the edge
     */
    public void addEdge(Node source, Node target) {
        EdgeNode sourceEdgeNode = new EdgeNode(source.getNamespace(), source.getInstance(), source.getComponent());
        EdgeNode targetEdgeNode = new EdgeNode(target.getNamespace(), target.getInstance(), target.getComponent());
        this.dependencyGraph.addEdge(source, target, new Edge(sourceEdgeNode, targetEdgeNode));
    }

    /**
     * Get the edges in the current dependency model.
     *
     * @return The nodes in the current dependency model used by the Model Manager.
     */
    public Set<Edge> getCurrentEdges() {
        return dependencyGraph.edges();
    }

    /**
     * Get the nodes in the current dependency model.
     *
     * @return The edges in the current dependency model used by the Model Manager.
     */
    public Set<Node> getCurrentNodes() {
        return dependencyGraph.nodes();
    }

    /**
     * Get the union dependency model within a given time period.
     *
     * @param startTime The start time of the time period
     * @param endTime The end time of the time period
     * @return The union dependency mode
     * @throws GraphStoreException If loading the model failed
     */
    public Model getDependencyModel(long startTime, long endTime) throws GraphStoreException {
        List<Model> models = ServiceHolder.getModelStoreManager().loadModel(startTime, endTime);
        return getMergedModel(models);
    }

    /**
     * Get the dependency model for a particular namespace for a given time period.
     *
     * @param startTime The start time of the time period
     * @param endTime The end time of the time period
     * @param namespace The namespace the instance belongs to
     * @return The union dependency mode
     * @throws GraphStoreException If loading the model failed
     */
    public Model getNamespaceDependencyModel(long startTime, long endTime, String namespace)
            throws GraphStoreException {
        Model completeModel = this.getDependencyModel(startTime, endTime);
        Model partialModel = new Model(new HashSet<>(), new HashSet<>());
        List<Node> instanceNodes = completeModel.getNodes()
                .stream()
                .filter((node) -> Objects.equals(node.getNamespace(), namespace))
                .collect(Collectors.toList());
        for (Node instanceNode : instanceNodes) {
            extractPartialModel(completeModel, partialModel, instanceNode,
                    (edge) -> Objects.equals(edge.getSource().getNamespace(), namespace));
        }
        return partialModel;
    }

    /**
     * Get the dependency model for a particular instance in a namespace for a given time period.
     *
     * @param startTime The start time of the time period
     * @param endTime The end time of the time period
     * @param namespace The namespace the instance belongs to
     * @param instance The instance of which the dependency model should be fetched
     * @return The union dependency mode
     * @throws GraphStoreException If loading the model failed
     */
    public Model getInstanceDependencyModel(long startTime, long endTime, String namespace, String instance)
            throws GraphStoreException {
        Model completeModel = this.getDependencyModel(startTime, endTime);
        Model partialModel = new Model(new HashSet<>(), new HashSet<>());
        List<Node> instanceNodes = completeModel.getNodes()
                .stream()
                .filter((node) -> Objects.equals(node.getNamespace(), namespace)
                        && Objects.equals(node.getInstance(), instance))
                .collect(Collectors.toList());
        for (Node instanceNode : instanceNodes) {
            extractPartialModel(completeModel, partialModel, instanceNode,
                    (edge) -> Objects.equals(edge.getSource().getNamespace(), namespace)
                            && Objects.equals(edge.getSource().getInstance(), instance));
        }
        return partialModel;
    }

    /**
     * Get the dependency model for a particular component in a namespace for a given time period.
     *
     * @param startTime The start time of the time period
     * @param endTime The end time of the time period
     * @param namespace The namespace the instance belongs to
     * @param instance The instance the component belongs to
     * @param component The component of which the dependency model should be fetched
     * @return The union dependency mode
     * @throws GraphStoreException If loading the model failed
     */
    public Model getComponentDependencyModel(long startTime, long endTime, String namespace, String instance,
                                             String component)
            throws GraphStoreException {
        Model completeModel = this.getDependencyModel(startTime, endTime);
        Model partialModel = new Model(new HashSet<>(), new HashSet<>());
        Optional<Node> componentNode = completeModel.getNodes()
                .stream()
                .filter((node) -> Objects.equals(node.getNamespace(), namespace)
                        && Objects.equals(node.getInstance(), instance)
                        && Objects.equals(node.getComponent(), component))
                .findAny();
        componentNode.ifPresent(node -> extractPartialModel(
                completeModel, partialModel, node,
                (edge -> Objects.equals(edge.getSource().getNamespace(), namespace)
                        && Objects.equals(edge.getSource().getInstance(), instance))));
        return partialModel;
    }

    /**
     * Extract a partial model from the complete model.
     *
     * @param completeModel The complete model from which the partial model should be extracted
     * @param partialModel The partial model object which should be populated
     * @param startNode The node to start the DFS from
     * @param edgeTraversePredicate Predicate indicating a particular edge should be traversed in a search
     */
    private void extractPartialModel(Model completeModel, Model partialModel, EdgeNode startNode,
                                     Predicate<Edge> edgeTraversePredicate) {
        Optional<Node> actualStartNode = completeModel.getNodes()
                .stream()
                .filter((node) -> Objects.equals(node, startNode))
                .findAny();
        if (actualStartNode.isPresent() && !partialModel.getNodes().contains(actualStartNode.get())) {  // Avoid loops
            partialModel.getNodes().add(actualStartNode.get());
            for (Edge edge : completeModel.getEdges()) {
                if (Objects.equals(edge.getSource(), startNode)) {
                    if (edgeTraversePredicate.test(edge)) {
                        partialModel.getEdges().add(edge);
                        extractPartialModel(completeModel, partialModel, edge.getTarget(), edgeTraversePredicate);
                    }
                }
            }
        }
    }

    /**
     * Ge the merged model of multiple models.
     *
     * @param models The models to be merged
     * @return The merged model
     */
    private Model getMergedModel(Collection<Model> models) {
        Set<Node> mergedNodesSet = new HashSet<>();
        Set<Edge> mergedEdgesSet = new HashSet<>();
        for (Model model : models) {
            mergedNodesSet.addAll(model.getNodes());
            mergedEdgesSet.addAll(model.getEdges());
        }
        return new Model(mergedNodesSet, mergedEdgesSet);
    }
}
