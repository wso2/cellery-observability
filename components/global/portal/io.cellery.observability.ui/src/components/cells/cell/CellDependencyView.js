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

import DependencyGraph from "../../common/DependencyGraph";
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
        fontSize: "1rem",
        display: "block"
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
    graph: {
        width: "100%",
        height: "100%"
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

class CellDependencyView extends React.Component {

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
            || this.props.cell !== nextProps.cell;

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
        const {globalState, cell} = this.props;
        const self = this;

        const search = {
            queryStartTime: queryStartTime.valueOf(),
            queryEndTime: queryEndTime.valueOf()
        };

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Cell Dependency Graph", globalState);
        }
        HttpUtils.callObservabilityAPI(
            {
                url: `/dependency-model/cells/${cell}${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            globalState
        ).then((data) => {
            self.setState({
                data: {
                    nodes: data.nodes,
                    edges: data.edges
                }
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
            }
        }).catch(() => {
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                NotificationUtils.showNotification(
                    "Failed to load cell dependency view",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    onClickCell = (nodeId) => {
        const {history} = this.props;
        const cell = nodeId.split(":")[0];
        history.push(`/cells/${cell}`);
    };

    render = () => {
        const {classes, cell, colorGenerator} = this.props;
        const dependedNodeCount = this.state.data.nodes.length;

        const viewGenerator = (nodeId, opacity) => {
            const color = ColorGenerator.shadeColor(colorGenerator.getColor(nodeId), opacity);
            const outlineColor = ColorGenerator.shadeColor(color, -0.08);

            const cellView = '<svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="14px" height="14px" viewBox="0 0 14 14" style="enable-background:new 0 0 14 14" xmlSpace="preserve">'
                + `<path fill="${color}"  stroke="${(cell === nodeId) ? "#444" : outlineColor}" stroke-opacity="${1 - opacity}" `
                + ' stroke-width="0.5px" d="M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,1V9a1.43,1.43,0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,.41H8.92a1.4,1.4,0,0,0,1-.41L12.72,10a1.46,1.46,0,0,0,.41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,0,0,8.92.84Z" transform="translate(-0.54 -0.37)"/>'
                + "</svg>";

            return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(cellView)}`;
        };

        const dataNodes = this.state.data.nodes;
        const dataEdges = this.state.data.edges;

        let view;
        if (dependedNodeCount > 1) {
            view = (
                <ErrorBoundary title={"Unable to Render"} description={"Unable to Render due to Invalid Data"}>
                    <div className={classes.dependencyGraph}>
                        <DependencyGraph id="graph-id" nodeData={dataNodes} edgeData={dataEdges} selectedCell={cell}
                            onClickNode={this.onClickCell} viewGenerator={viewGenerator} graphType="dependency" />
                    </div>
                </ErrorBoundary>
            );
        } else {
            view = (
                <div>
                    <InfoOutlined className={classes.infoIcon} color="action"/>
                    <Typography variant="subtitle2" color="textSecondary" className={classes.info}>
                        {`"${cell}"`} cell does not depend on any other Cell
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

CellDependencyView.propTypes = {
    classes: PropTypes.object.isRequired,
    cell: PropTypes.string.isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired,
    colorGenerator: PropTypes.instanceOf(ColorGenerator).isRequired,
    history: PropTypes.shape({
        push: PropTypes.func.isRequired
    }).isRequired
};

export default withStyles(styles, {withTheme: true})(withColor(withGlobalState(withRouter(CellDependencyView))));
