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

import "vis/dist/vis-network.min.css";
import ErrorBoundary from "../../common/error/ErrorBoundary";
import React from "react";
import UnknownError from "../../common/error/UnknownError";
import vis from "vis";
import {withStyles} from "@material-ui/core/styles/index";
import * as PropTypes from "prop-types";

const styles = (theme) => ({
    graph: {
        width: "100%",
        height: "100%"
    }
});

class ComponentDependencyGraph extends React.Component {

    static NodeType = {
        CELL: "Cell",
        COMPOSITE: "Composite",
        COMPONENT: "component",
        GATEWAY: "gateway"
    };

    static INSTANCE_COMPONENT_SEPARATOR = ":";

    static GRAPH_OPTIONS = {
        nodes: {
            size: 40,
            font: {
                size: 15,
                color: "#000000"
            }
        },
        edges: {
            width: 2,
            smooth: false,
            color: {
                inherit: false,
                color: "#ccc7c7"
            },
            arrows: {
                to: {
                    enabled: true,
                    scaleFactor: 0.5
                }
            }
        },
        layout: {
            randomSeed: 1,
            improvedLayout: true
        },
        autoResize: true,
        physics: {
            enabled: true,
            forceAtlas2Based: {
                gravitationalConstant: -800,
                centralGravity: 0.1,
                avoidOverlap: 1
            },
            solver: "forceAtlas2Based",
            stabilization: {
                enabled: true,
                iterations: 25,
                fit: true
            }
        },
        interaction: {
            selectConnectedEdges: false,
            hover: true
        }
    };

    constructor(props) {
        super(props);
        this.dependencyGraph = React.createRef();
        this.isInitializationDone = false;
    }

    componentDidMount = () => {
        if (this.dependencyGraph.current) {
            this.draw();
        }
    };

    componentDidUpdate = () => {
        if (this.dependencyGraph.current) {
            this.draw();
        }
    };

    draw = () => {
        const {nodeData, edgeData, selectedComponent, viewGenerator, onClickNode, selectedInstanceKind,
            instance} = this.props;
        const dataNodes = [];
        const dataEdges = [];

        const getGroupNodesIds = (group) => {
            const output = [];
            nodes.get({
                filter: function(item) {
                    if (item.group === group) {
                        output.push(item.id);
                    }
                }
            });
            return output;
        };

        const getDistance = (pts, centroid) => {
            const cenX = centroid.x;
            const cenY = centroid.y;
            const distance = [];
            let dist = 0;
            for (let i = 0; i < pts.length; i++) {
                distance.push(Math.hypot(pts[i].x - cenX, pts[i].y - cenY));
            }
            dist = Math.max(...distance);
            return dist;
        };

        const getPolygonCentroid = (pts) => {
            let maxX;
            let maxY;
            let minX;
            let minY;
            for (let i = 0; i < pts.length; i++) {
                minX = (pts[i].x < minX || minX === undefined) ? pts[i].x : minX;
                maxX = (pts[i].x > maxX || maxX === undefined) ? pts[i].x : maxX;
                minY = (pts[i].y < minY || minY === undefined) ? pts[i].y : minY;
                maxY = (pts[i].y > maxY || maxY === undefined) ? pts[i].y : maxY;
            }
            return {x: (minX + maxX) / 2, y: (minY + maxY) / 2};
        };

        const getGroupNodePositions = (groupId) => {
            const groupNodes = getGroupNodesIds(groupId);
            const nodePositions = network.getPositions(groupNodes);
            return Object.values(nodePositions);
        };

        const findPoint = (x, y, angle, distance) => {
            const result = {};
            result.x = Math.round(Math.cos(angle * Math.PI / 180) * distance + x);
            result.y = Math.round(Math.sin(angle * Math.PI / 180) * distance + y);
            return result;
        };

        if (nodeData) {
            nodeData.forEach((node, index) => {
                dataNodes.push({
                    id: node.id,
                    label: node.label,
                    shape: "image",
                    image: viewGenerator(node.group, node.id, 0),
                    group: node.group
                });
            });
        }

        if (edgeData) {
            edgeData.forEach((edge, index) => {
                // Finding distinct links
                const linkMatches = dataEdges.find(
                    (existingEdge) => existingEdge.from === edge.source && existingEdge.to === edge.target);

                if (!linkMatches) {
                    dataEdges.push({
                        id: index,
                        from: edge.source,
                        to: edge.target
                    });
                }
            });
        }

        const getGroupNodes = (nodes, group) => {
            const output = [];
            nodes.forEach((node) => {
                if (node.group === group) {
                    output.push(node);
                }
            });
            return output;
        };

        const groupNodesComponents = getGroupNodes(dataNodes, ComponentDependencyGraph.NodeType.COMPONENT);
        const groupNodesCells = getGroupNodes(dataNodes, ComponentDependencyGraph.NodeType.CELL);
        const groupNodesGateway = getGroupNodes(dataNodes, ComponentDependencyGraph.NodeType.GATEWAY);
        const groupNodesComposites = getGroupNodes(dataNodes, ComponentDependencyGraph.NodeType.COMPOSITE);

        let nodes = new vis.DataSet(groupNodesComponents);
        nodes.add({
            id: instance,
            label: instance,
            shape: "image",
            image: viewGenerator(selectedInstanceKind, instance, 0),
            group: selectedInstanceKind
        });
        nodes.add(groupNodesGateway);
        nodes.add(groupNodesComposites);
        nodes.add(groupNodesCells);
        const edges = new vis.DataSet(dataEdges);

        const graphData = {
            nodes: nodes,
            edges: edges
        };

        const network
            = new vis.Network(this.dependencyGraph.current, graphData, ComponentDependencyGraph.GRAPH_OPTIONS);
        let allNodes;
        const spacing = 150;
        const updatedNodes = [];

        if (this.dependencyGraph !== null) {
            this.dependencyGraph.current.style.visibility = "hidden";
        }

        if (selectedComponent) {
            network.selectNodes([selectedComponent], false);
        }

        network.on("beforeDrawing", (ctx) => {
            ctx.fillStyle = "#ffffff";
            ctx.fillRect(-ctx.canvas.offsetWidth, -(ctx.canvas.offsetHeight + 20),
                ctx.canvas.width, ctx.canvas.height);
        });

        network.on("stabilized", () => {
            const nodeIds = nodes.getIds();
            if (!this.isInitializationDone) {
                network.fit({
                    nodes: nodeIds
                });
                this.isInitializationDone = true;
            }

            window.onresize = () => {
                network.fit({
                    nodes: nodeIds
                });
            };

            if (this.dependencyGraph !== null) {
                this.dependencyGraph.current.style.visibility = "visible";
            }
        });

        network.on("stabilizationIterationsDone", () => {
            allNodes = nodes.get({returnType: "Object"});

            let centerPoint = getPolygonCentroid(getGroupNodePositions(ComponentDependencyGraph.NodeType.COMPONENT));
            const polygonRadius = getDistance(getGroupNodePositions(ComponentDependencyGraph.NodeType.COMPONENT),
                centerPoint);
            const size = polygonRadius + spacing;

            for (const nodeId in allNodes) {
                if (allNodes[nodeId].group === ComponentDependencyGraph.NodeType.COMPONENT) {
                    allNodes[nodeId].fixed = true;
                    if (allNodes.hasOwnProperty(nodeId)) {
                        updatedNodes.push(allNodes[nodeId]);
                    }
                }
            }
            centerPoint = getPolygonCentroid(getGroupNodePositions(ComponentDependencyGraph.NodeType.COMPONENT));

            const focused = nodes.get(instance);
            focused.size = size;
            focused.fixed = true;
            if (groupNodesComponents.length === 1) {
                focused.mass = 5;
            } else {
                focused.mass = polygonRadius / 10;
            }
            network.moveNode(instance, centerPoint.x, centerPoint.y);
            updatedNodes.push(focused);

            // Placing gateway node
            if (groupNodesGateway.length > 0) {
                const gatewayNode = nodes.get(selectedComponent);
                gatewayNode.fixed = true;
                const gatewayPoint = findPoint(centerPoint.x, centerPoint.y, 90, size * Math.cos(180 / 8));

                if (gatewayNode) {
                    const x = gatewayPoint.x;
                    const y = gatewayPoint.y;
                    network.moveNode(gatewayNode.id, x, y);
                    updatedNodes.push(gatewayNode);
                }
            }
            nodes.update(updatedNodes);
        });

        const neighbourhoodHighlight = (params) => {
            const connectedNodes = network.getConnectedNodes(params.node);

            if (connectedNodes.length > 0) {
                const selectedNode = params.node;

                // Gray out all nodes
                for (const nodeId in allNodes) {
                    if (allNodes.hasOwnProperty(nodeId)) {
                        allNodes[nodeId].image = viewGenerator(allNodes[nodeId].group, nodeId, 0.8);
                        if (allNodes[nodeId].hiddenLabel === undefined) {
                            allNodes[nodeId].hiddenLabel = allNodes[nodeId].label;
                            allNodes[nodeId].label = undefined;
                        }
                    }
                }

                // Set first degree nodes their color and label
                for (let i = 0; i < connectedNodes.length; i++) {
                    allNodes[connectedNodes[i]].image
                        = viewGenerator(allNodes[connectedNodes[i]].group, connectedNodes[i], 0);
                    if (allNodes[connectedNodes[i]].hiddenLabel !== undefined) {
                        allNodes[connectedNodes[i]].label = allNodes[connectedNodes[i]].hiddenLabel;
                        allNodes[connectedNodes[i]].hiddenLabel = undefined;
                    }
                }

                // Set main node color and label
                allNodes[selectedNode].image = viewGenerator(allNodes[selectedNode].group, selectedNode, 0);
                if (allNodes[selectedNode].hiddenLabel !== undefined) {
                    allNodes[selectedNode].label = allNodes[selectedNode].hiddenLabel;
                    allNodes[selectedNode].hiddenLabel = undefined;
                }
            }

            const updateArray = [];
            for (const nodeId in allNodes) {
                if (allNodes.hasOwnProperty(nodeId)) {
                    updateArray.push(allNodes[nodeId]);
                }
            }
            nodes.update(updateArray);
        };

        const blur = () => {
            for (const nodeId in allNodes) {
                if (allNodes.hasOwnProperty(nodeId)) {
                    allNodes[nodeId].image = viewGenerator(allNodes[nodeId].group, nodeId, 0);
                    if (allNodes[nodeId].hiddenLabel !== undefined) {
                        allNodes[nodeId].label = allNodes[nodeId].hiddenLabel;
                        allNodes[nodeId].hiddenLabel = undefined;
                    }
                }
            }
            const updateArray = [];
            for (const nodeId in allNodes) {
                if (allNodes.hasOwnProperty(nodeId)) {
                    updateArray.push(allNodes[nodeId]);
                }
            }
            nodes.update(updateArray);
        };
        network.on("hoverNode", neighbourhoodHighlight);
        network.on("blurNode", blur);
        network.on("selectNode", (event) => {
            onClickNode(event.nodes[0], allNodes[event.nodes[0]].group);
        });
    };

    render = () => {
        const {nodeData, classes} = this.props;
        let view;

        if (nodeData && nodeData.length > 0) {
            view = (
                <ErrorBoundary title={"Unable to Render"} description={"Unable to Render due to Invalid Data"}>
                    <div className={classes.graph} ref={this.dependencyGraph}/>
                </ErrorBoundary>
            );
        } else {
            view = (
                <UnknownError title={"No Data Available"} description={"No Data Available to render Dependency Graph"}/>
            );
        }
        return view;
    };

}

ComponentDependencyGraph.propTypes = {
    classes: PropTypes.object.isRequired,
    id: PropTypes.string.isRequired,
    nodeData: PropTypes.arrayOf(PropTypes.object),
    edgeData: PropTypes.arrayOf(PropTypes.object),
    selectedComponent: PropTypes.string,
    config: PropTypes.object,
    reloadGraph: PropTypes.bool,
    onClickNode: PropTypes.func,
    onClickGraph: PropTypes.func,
    viewGenerator: PropTypes.func,
    selectedInstanceKind: PropTypes.string,
    instance: PropTypes.string
};

export default withStyles(styles)(ComponentDependencyGraph);

