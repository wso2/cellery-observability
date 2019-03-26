/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* eslint max-len: ["off"] */

import ComponentDependencyGraph from "./ComponentDependencyGraph";
import Divider from "@material-ui/core/Divider";
import ErrorBoundary from "../../common/error/ErrorBoundary";
import HttpUtils from "../../../utils/api/httpUtils";
import InfoOutlined from "@material-ui/icons/InfoOutlined";
import NotificationUtils from "../../../utils/common/notificationUtils";
import QueryUtils from "../../../utils/common/queryUtils";
import React from "react";
import StateHolder from "../../common/state/stateHolder";
import Typography from "@material-ui/core/Typography/Typography";
import withGlobalState from "../../common/state";
import {withRouter} from "react-router-dom";
import {withStyles} from "@material-ui/core";
import withColor, {ColorGenerator} from "../../common/color";
import * as PropTypes from "prop-types";

const styles = (theme) => ({
    subtitle: {
        fontWeight: 400,
        fontSize: "1rem"
    },
    graph: {
        width: "100%",
        height: "100%"
    },
    dependencies: {
        marginTop: theme.spacing.unit * 3
    },
    graphContainer: {
        display: "flex"
    },
    dependencyGraph: {
        height: "75vh",
        width: "100%"
    },
    diagram: {
        padding: theme.spacing.unit * 3,
        flexGrow: 1
    },
    info: {
        display: "inline-flex"
    },
    infoIcon: {
        verticalAlign: "middle",
        display: "inline-flex",
        fontSize: 18,
        marginRight: 4
    },
    divider: {
        marginTop: theme.spacing.unit
    }
});

class ComponentDependencyView extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            data: {
                nodes: [],
                edges: []
            },
            currentTimeRange: {
                startTime: props.globalState.get(StateHolder.GLOBAL_FILTER).startTime,
                endTime: props.globalState.get(StateHolder.GLOBAL_FILTER).endTime
            }
        };
    }

    componentDidMount = () => {
        const {globalState} = this.props;

        this.update(
            true,
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).startTime).valueOf(),
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).endTime).valueOf()
        );
    };

    componentDidUpdate = () => {
        const {globalState} = this.props;

        this.update(
            true,
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).startTime).valueOf(),
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).endTime).valueOf()
        );
    };

    /**
     * React lifecycle method to change the next state based on the props and state.
     *
     * @param {Object} props The current props
     * @param {Object} state The current state
     * @returns {Object} The next state to be used
     */
    static getDerivedStateFromProps = (props, state) => ({
        ...state,
        currentTimeRange: {
            startTime: props.globalState.get(StateHolder.GLOBAL_FILTER).startTime,
            endTime: props.globalState.get(StateHolder.GLOBAL_FILTER).endTime
        }
    });

    /**
     * React lifecycle method to indicate whether the update should be done or not.
     * This checks for the props and state changes and decides whether the update should happen.
     *
     * @param {Object} nextProps The next props that will be used
     * @param {Object} nextState The next state that will be used
     * @returns {boolean} True if component should update
     */
    shouldComponentUpdate = (nextProps, nextState) => {
        const {data, currentTimeRange} = this.state;
        const startTime = currentTimeRange.startTime;
        const endTime = currentTimeRange.endTime;
        const newStartTime = nextProps.globalState.get(StateHolder.GLOBAL_FILTER).startTime;
        const newEndTime = nextProps.globalState.get(StateHolder.GLOBAL_FILTER).endTime;

        // Check if user inputs (cell, component, time range) hand changed
        let shouldComponentUpdate = startTime !== newStartTime || endTime !== newEndTime
            || this.props.cell !== nextProps.cell || this.props.component !== nextProps.component;

        if (!shouldComponentUpdate) {
            // Check if the number of items in the data had changed
            shouldComponentUpdate = data.nodes.length !== nextState.data.nodes.length
                || data.edges.length !== nextState.data.edges.length;

            // Check if the actual data items had changed
            if (!shouldComponentUpdate) {
                for (let i = 0; i < data.nodes.length; i++) {
                    shouldComponentUpdate
                        = nextState.data.nodes.filter((nextNode) => nextNode.id === data.nodes[i].id).length === 0;
                    if (shouldComponentUpdate) {
                        break;
                    }
                }
            }
            if (!shouldComponentUpdate) {
                for (let i = 0; i < data.edges.length; i++) {
                    shouldComponentUpdate = nextState.data.edges.filter(
                        (nextEdges) => nextEdges.id === data.edges[i].id).length === 0;
                    if (shouldComponentUpdate) {
                        break;
                    }
                }
            }
        }
        return shouldComponentUpdate;
    };

    update = (isUserAction, queryStartTime, queryEndTime) => {
        const {globalState, cell, component} = this.props;
        const self = this;

        const search = {
            queryStartTime: queryStartTime.valueOf(),
            queryEndTime: queryEndTime.valueOf()
        };

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Component Dependency Graph", globalState);
        }
        let url = `/dependency-model/cells/${cell}/components/${component}`;
        url += `${HttpUtils.generateQueryParamString(search)}`;
        HttpUtils.callObservabilityAPI(
            {
                url: url,
                method: "GET"
            },
            globalState
        ).then((data) => {
            // Update node,edge data to show external cell dependencies
            const nodes = [];
            const edges = [];

            data.nodes.forEach((node) => {
                if (cell === node.id.split(":")[0]) {
                    if (node.id.split(":")[1] === ComponentDependencyGraph.NodeType.GATEWAY) {
                        nodes.push({
                            ...node,
                            label: node.id.split(":")[1],
                            group: "gateway"
                        });
                    } else {
                        nodes.push({
                            ...node,
                            label: node.id.split(":")[1],
                            group: ComponentDependencyGraph.NodeType.COMPONENT
                        });
                    }
                } else if (node.id.split(":")[1] === ComponentDependencyGraph.NodeType.GATEWAY) {
                    nodes.push({
                        ...node,
                        label: node.id.split(":")[0],
                        group: ComponentDependencyGraph.NodeType.CELL
                    });
                }
            });

            data.edges.forEach((edge) => {
                if ((cell === edge.source.split(":")[0] && cell === edge.target.split(":")[0])
                    || (cell === edge.source.split(":")[0] && edge.target.split(":")[1] === "gateway")) {
                    edges.push({
                        ...edge
                    });
                }
            });

            self.setState({
                data: {
                    nodes: nodes,
                    edges: edges
                }
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
            }
        }).catch(() => {
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                NotificationUtils.showNotification(
                    "Failed to load component dependency view",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    onClickNode = (nodeId, nodeType) => {
        const {history} = this.props;
        const cell = nodeId.split(":")[0];
        const component = nodeId.split(":")[1];
        if (nodeType === ComponentDependencyGraph.NodeType.CELL) {
            history.push(`/cells/${cell}`);
        } else {
            history.push(`/cells/${cell}/components/${component}`);
        }
    };

    render = () => {
        const {classes, cell, component, colorGenerator} = this.props;
        const dependedNodeCount = this.state.data.nodes.length;
        const selectedNode = `${cell}:${component}`;


        const viewGenerator = (group, nodeId, opacity) => {
            const color = ColorGenerator.shadeColor(colorGenerator.getColor(nodeId.split(":")[0]), opacity);
            const outlineColor = ColorGenerator.shadeColor(color, -0.08);
            const componentColor = ColorGenerator.shadeColor("#999999", opacity);

            let cellView;

            if (group === ComponentDependencyGraph.NodeType.COMPONENT) {
                cellView = '<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="14px" height="14px" viewBox="0 0 14 14" style="enable-background:new 0 0 14 14" xmlSpace="preserve">'
                    + `<path fill="${color}"  stroke="${(selectedNode === nodeId) ? "#444" : outlineColor}" stroke-opacity="${1 - opacity}" `
                    + 'stroke-width="0.5px" d="M13,7a6,6,0,0,1-6,6.06A6,6,0,0,1,1,7,6,6,0,0,1,7,.94,6,6,0,0,1,13,7Z" transform="translate(-0.79 -0.69)"/>'
                    + `<path fill="${componentColor}" stroke="#fff" stroke-width="0.1px" d="M4.37,5c-.19.11-.19.28,0,.39L6.76,6.82a.76.76,0,0,0,.69,0L9.64,5.45a.23.23,0,0,0,0-.42L7.45,3.7a.76.76,0,0,0-.69,0Z" transform="translate(-0.79 -0.69)"/>`
                    + `<path fill="${componentColor}" stroke="#fff" stroke-width="0.1px" d="M10,5.93c0-.22-.15-.31-.34-.19L7.45,7.1a.73.73,0,0,1-.69,0L4.37,5.73c-.19-.11-.35,0-.35.2V8a.88.88,0,0,0,.33.63l2.43,1.68a.61.61,0,0,0,.65,0L9.66,8.63A.9.9,0,0,0,10,8Z" transform="translate(-0.79 -0.69)"/>`
                    + '<text fill="#fff" font-size="1.63px" font-family="ArialMT, Arial" transform="translate(5.76 5.1) scale(0.98 1)">μ</text>'
                    + "</svg>";
            } else if (group === ComponentDependencyGraph.NodeType.GATEWAY) {
                cellView = '<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="14px" height="14px" viewBox="0 0 14 14" style="enable-background:new 0 0 14 14" xmlSpace="preserve">'
                    + `<path fill="${color}"  stroke="${(selectedNode === nodeId) ? "#444" : outlineColor}" stroke-opacity="${1 - opacity}" `
                    + 'stroke-width="0.5px" d="M13,7a6,6,0,0,1-6,6.06A6,6,0,0,1,1,7,6,6,0,0,1,7,.94,6,6,0,0,1,13,7Z" transform="translate(-0.79 -0.69)"/>'
                    + `<path fill="${componentColor}" stroke="#fff" stroke-width="0.1px" d="M6.39,6.29v1L4.6,5.47a.14.14,0,0,1,0-.21L6.39,3.47v1a.15.15,0,0,0,.15.14H9.3a.15.15,0,0,1,.14.15V6a.15.15,0,0,1-.14.15H6.54A.15.15,0,0,0,6.39,6.29ZM7.46,7.85H4.7A.15.15,0,0,0,4.56,8V9.27a.15.15,0,0,0,.14.15H7.46a.15.15,0,0,1,.15.14v1L9.4,8.74a.14.14,0,0,0,0-.21L7.61,6.74v1A.15.15,0,0,1,7.46,7.85Z" transform="translate(-0.79 -0.69)"/>`
                    + "</svg>";
            } else {
                cellView = '<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="14px" height="14px" viewBox="0 0 14 14" style="enable-background:new 0 0 14 14" xmlSpace="preserve">'
                    + `<path fill="${color}"  stroke="${outlineColor}" stroke-opacity="${1 - opacity}" `
                    + ' stroke-width="0.5px" d="M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,1V9a1.43,1.43,0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,.41H8.92a1.4,1.4,0,0,0,1-.41L12.72,10a1.46,1.46,0,0,0,.41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,0,0,8.92.84Z" transform="translate(-0.54 -0.37)"/>'
                    + "</svg>";
            }

            return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(cellView)}`;
        };

        const dataNodes = this.state.data.nodes;
        const dataEdges = this.state.data.edges;
        let view;

        if (dependedNodeCount > 1) {
            view = (
                <ErrorBoundary title={"Unable to Render"} description={"Unable to Render due to Invalid Data"}>
                    <div className={classes.dependencyGraph}>
                        <ComponentDependencyGraph
                            id="component-dependency-graph"
                            nodeData={dataNodes} edgeData={dataEdges} selectedComponent={selectedNode}
                            onClickNode={this.onClickNode} viewGenerator={viewGenerator}
                            graphType="dependency" cellColor={colorGenerator.getColor(cell)}/>
                    </div>
                </ErrorBoundary>
            );
        } else {
            view = (
                <div>
                    <InfoOutlined className={classes.infoIcon} color="action"/>
                    <Typography variant="subtitle2" color="textSecondary" className={classes.info}>
                        {`"${component}"`} component in {`"${cell}"`} cell does not depend on any other Component
                    </Typography>
                </div>
            );
        }
        return (
            <div className={classes.dependencies}>
                <Typography color="textSecondary" className={classes.subtitle}>
                    Dependencies
                </Typography>
                <Divider className={classes.divider}/>
                <div className={classes.graphContainer}>
                    <div className={classes.diagram}>
                        {view}
                    </div>
                </div>
            </div>
        );
    }

}

ComponentDependencyView.propTypes = {
    classes: PropTypes.object.isRequired,
    cell: PropTypes.string.isRequired,
    component: PropTypes.string.isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired,
    colorGenerator: PropTypes.instanceOf(ColorGenerator).isRequired,
    history: PropTypes.shape({
        push: PropTypes.func.isRequired
    }).isRequired
};

export default withStyles(styles, {withTheme: true})(withColor(withGlobalState(withRouter(ComponentDependencyView))));
