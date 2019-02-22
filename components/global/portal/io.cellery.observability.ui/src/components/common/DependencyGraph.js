/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ErrorBoundary from "./error/ErrorBoundary";
import React from "react";
import UnknownError from "./error/UnknownError";
import vis from "vis";
import {withStyles} from "@material-ui/core/styles/index";
import * as PropTypes from "prop-types";

const styles = (theme) => ({
    graph: {
        width: "100%",
        height: "100%"
    }
});

class DependencyGraph extends React.Component {

    static GraphType = {
        OVERVIEW: "overview",
        DEPENDENCY: "dependency"
    };

    static GRAPH_OPTIONS = {
        nodes: {
            shapeProperties: {
                borderRadius: 10
            },
            borderWidth: 1,
            size: 35,
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
            barnesHut: {
                gravitationalConstant: -1000,
                centralGravity: 0.3,
                springLength: 100,
                springConstant: 0.04,
                damping: 0.09,
                avoidOverlap: 1
            },
            forceAtlas2Based: {
                gravitationalConstant: -50,
                centralGravity: 0.01,
                springConstant: 0.08,
                springLength: 100,
                damping: 0.4,
                avoidOverlap: 1
            },
            repulsion: {
                centralGravity: 1,
                springLength: 0,
                springConstant: 0,
                nodeDistance: 0,
                damping: 0.09
            },
            hierarchicalRepulsion: {
                centralGravity: 0.0,
                springLength: 100,
                springConstant: 0.01,
                nodeDistance: 120,
                damping: 0.09
            },
            maxVelocity: 50,
            minVelocity: 0.1,
            solver: "forceAtlas2Based",
            stabilization: {
                enabled: true,
                iterations: 1000,
                updateInterval: 100,
                onlyDynamicEdges: false,
                fit: true
            },
            adaptiveTimestep: false
        },
        interaction: {
            hover: true
        }
    };

    constructor(props) {
        super(props);
        this.dependencyGraph = React.createRef();
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
        const {nodeData, edgeData, onClickNode, onClickGraph, selectedCell, viewGenerator, graphType} = this.props;
        const dataNodes = [];
        const dataEdges = [];

        if (nodeData) {
            nodeData.forEach((node, index) => {
                dataNodes.push({
                    id: node.id,
                    label: node.id,
                    shape: "image",
                    image: viewGenerator(node.id, 0)
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

        const nodes = new vis.DataSet(dataNodes);
        const edges = new vis.DataSet(dataEdges);

        const graphData = {
            nodes: nodes,
            edges: edges
        };

        const network = new vis.Network(this.dependencyGraph.current, graphData, DependencyGraph.GRAPH_OPTIONS);

        if (graphType === DependencyGraph.GraphType.OVERVIEW) {
            network.on("selectNode", (event) => {
                onClickNode(event.nodes[0], true);
            });

            network.on("deselectNode", (event) => {
                onClickGraph();
            });
        }

        if (selectedCell) {
            network.selectNodes([selectedCell], false);
        }

        const allNodes = nodes.get({returnType: "Object"});

        const neighbourhoodHighlight = (params) => {
            const connectedNodes = network.getConnectedNodes(params.node);

            if (connectedNodes.length > 0) {
                const selectedNode = params.node;

                // Gray out all nodes
                for (const nodeId in allNodes) {
                    if (allNodes.hasOwnProperty(nodeId)) {
                        allNodes[nodeId].image = viewGenerator(nodeId, 0.8);
                        if (allNodes[nodeId].hiddenLabel === undefined) {
                            allNodes[nodeId].hiddenLabel = allNodes[nodeId].label;
                            allNodes[nodeId].label = undefined;
                        }
                    }
                }

                // Set first degree nodes their color and label
                for (let i = 0; i < connectedNodes.length; i++) {
                    allNodes[connectedNodes[i]].image = viewGenerator(connectedNodes[i], 0);
                    if (allNodes[connectedNodes[i]].hiddenLabel !== undefined) {
                        allNodes[connectedNodes[i]].label = allNodes[connectedNodes[i]].hiddenLabel;
                        allNodes[connectedNodes[i]].hiddenLabel = undefined;
                    }
                }

                // Set main node color and label
                allNodes[selectedNode].image = viewGenerator(selectedNode, 0);
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

        network.on("hoverNode", neighbourhoodHighlight);

        const blur = () => {
            for (const nodeId in allNodes) {
                if (allNodes.hasOwnProperty(nodeId)) {
                    allNodes[nodeId].image = viewGenerator(nodeId, 0);
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

        network.on("blurNode", blur);
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

DependencyGraph.propTypes = {
    classes: PropTypes.object.isRequired,
    id: PropTypes.string.isRequired,
    nodeData: PropTypes.arrayOf(PropTypes.object),
    edgeData: PropTypes.arrayOf(PropTypes.object),
    selectedCell: PropTypes.string,
    config: PropTypes.object,
    reloadGraph: PropTypes.bool,
    onClickNode: PropTypes.func,
    onClickGraph: PropTypes.func,
    graphType: PropTypes.string,
    viewGenerator: PropTypes.func
};

export default withStyles(styles)(DependencyGraph);
