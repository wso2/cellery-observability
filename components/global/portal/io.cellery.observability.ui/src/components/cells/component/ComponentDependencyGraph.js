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
        CELL: "cell",
        COMPONENT: "component"
    };

    static GRAPH_OPTIONS = {
        nodes: {
            shapeProperties: {
                borderRadius: 10
            },
            borderWidth: 1,
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
        const {nodeData, edgeData, selectedComponent, viewGenerator, cellColor} = this.props;
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

        const drawPolygon = (ctx, pts, radius) => {
            let points;
            if (radius > 0) {
                points = getRoundedPoints(pts, radius);
            }
            let i;
            let pt;
            const len = points.length;
            for (i = 0; i < len; i++) {
                pt = points[i];
                if (i === 0) {
                    ctx.beginPath();
                    ctx.moveTo(pt[0], pt[1]);
                } else {
                    ctx.lineTo(pt[0], pt[1]);
                }
                if (radius > 0) {
                    ctx.quadraticCurveTo(pt[2], pt[3], pt[4], pt[5]);
                }
            }
            ctx.closePath();
        };

        const getRoundedPoints = (pts, radius) => {
            let i1;
            let i2;
            let i3;
            let nextPt;
            let p1;
            let p2;
            let p3;
            let prevPt;
            const len = pts.length;

            const res = new Array(len);
            for (i2 = 0; i2 < len; i2++) {
                i1 = i2 - 1;
                i3 = i2 + 1;
                if (i1 < 0) {
                    i1 = len - 1;
                }
                if (i3 === len) {
                    i3 = 0;
                }
                p1 = pts[i1];
                p2 = pts[i2];
                p3 = pts[i3];
                prevPt = getRoundedPoint(p1[0], p1[1], p2[0], p2[1], radius, false);
                nextPt = getRoundedPoint(p2[0], p2[1], p3[0], p3[1], radius, true);
                res[i2] = [prevPt[0], prevPt[1], p2[0], p2[1], nextPt[0], nextPt[1]];
            }
            return res;
        };

        const getRoundedPoint = (x1, y1, x2, y2, radius, first) => {
            const total = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));


            const idx = first ? radius / total : (total - radius) / total;
            return [x1 + (idx * (x2 - x1)), y1 + (idx * (y2 - y1))];
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

        let nodes = new vis.DataSet(groupNodesComponents);
        const edges = new vis.DataSet(dataEdges);

        const graphData = {
            nodes: nodes,
            edges: edges
        };

        const network
            = new vis.Network(this.dependencyGraph.current, graphData, ComponentDependencyGraph.GRAPH_OPTIONS);
        let allNodes;

        if (selectedComponent) {
            network.selectNodes([selectedComponent], false);
        }

        network.on("afterDrawing", (ctx) => {
            const centerPoint = getPolygonCentroid(getGroupNodePositions(ComponentDependencyGraph.NodeType.COMPONENT));
            const polygonRadius = getDistance(getGroupNodePositions(ComponentDependencyGraph.NodeType.COMPONENT),
                centerPoint);
            const numberOfSides = 8;
            const size = polygonRadius + 70;
            const Xcenter = centerPoint.x;
            const Ycenter = centerPoint.y;
            let curve = 0;

            if (polygonRadius === 0) {
                curve = 7;
            } else {
                curve = 12;
            }

            ctx.translate(Xcenter, Ycenter); // Translate to center of shape
            ctx.rotate(22.5 * Math.PI / 180); // Rotate 22.5 degrees.
            ctx.translate(-Xcenter, -Ycenter);

            const cornerPoints = [];
            ctx.beginPath();
            ctx.moveTo(Xcenter + size, Ycenter + size);
            ctx.lineJoin = "round";
            ctx.lineWidth = 3;

            for (let i = 0; i <= numberOfSides; i += 1) {
                ctx.lineTo(Xcenter + size * Math.cos(i * 2 * Math.PI / numberOfSides), Ycenter + size
                    * Math.sin(i * 2 * Math.PI / numberOfSides));

                if (i < numberOfSides) {
                    cornerPoints.push([
                        Xcenter + size * Math.cos(i * 2 * Math.PI / numberOfSides),
                        Ycenter + size * Math.sin(i * 2 * Math.PI / numberOfSides)
                    ]);
                }
            }
            ctx.closePath();
            drawPolygon(ctx, cornerPoints, curve);
            ctx.strokeStyle = cellColor;
            ctx.stroke();
        });

        network.on("stabilizationIterationsDone", () => {
            network.setOptions({physics: false});
            const centerPoint = getPolygonCentroid(getGroupNodePositions(ComponentDependencyGraph.NodeType.COMPONENT));

            let polygonRadius = getDistance(getGroupNodePositions(ComponentDependencyGraph.NodeType.COMPONENT),
                centerPoint);
            polygonRadius += 200;
            const d = 2 * Math.PI / groupNodesCells.length;
            groupNodesCells.forEach((node, i) => {
                nodes.add(node);
                const x = polygonRadius * Math.cos(d * i);
                const y = polygonRadius * Math.sin(d * i);
                network.moveNode(node.id, x, y);
            });

            allNodes = nodes.get({returnType: "Object"});
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
    cellColor: PropTypes.string
};

export default withStyles(styles)(ComponentDependencyGraph);

