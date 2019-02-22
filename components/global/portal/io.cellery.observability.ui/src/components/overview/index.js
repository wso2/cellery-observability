/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

/* eslint max-lines: ["off"] */
/* eslint max-len: ["off"] */

import ArrowRightAltSharp from "@material-ui/icons/ArrowRightAltSharp";
import Button from "@material-ui/core/Button";
import CellIcon from "../../icons/CellIcon";
import ChevronLeftIcon from "@material-ui/icons/ChevronLeft";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import Constants from "../../utils/constants";
import DependencyGraph from "../common/DependencyGraph";
import Divider from "@material-ui/core/Divider";
import Drawer from "@material-ui/core/Drawer";
import Error from "@material-ui/icons/Error";
import Fade from "@material-ui/core/Fade";
import Grey from "@material-ui/core/colors/grey";
import HttpUtils from "../../utils/api/httpUtils";
import IconButton from "@material-ui/core/IconButton";
import MoreIcon from "@material-ui/icons/MoreHoriz";
import NotFound from "../common/error/NotFound";
import NotificationUtils from "../../utils/common/notificationUtils";
import Paper from "@material-ui/core/Paper";
import Popper from "@material-ui/core/Popper";
import QueryUtils from "../../utils/common/queryUtils";
import React from "react";
import SidePanelContent from "./SidePanelContent";
import StateHolder from "../common/state/stateHolder";
import TopToolbar from "../common/toptoolbar";
import Typography from "@material-ui/core/Typography";
import classNames from "classnames";
import withGlobalState from "../common/state";
import {withStyles} from "@material-ui/core/styles";
import withColor, {ColorGenerator} from "../common/color";
import * as PropTypes from "prop-types";

const drawerWidth = 300;

const styles = (theme) => ({
    root: {
        display: "flex"
    },
    moreDetails: {
        position: "absolute",
        right: 25,
        transition: theme.transitions.create(["margin", "width"], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen
        })
    },
    moreDetailsShift: {
        transition: theme.transitions.create(["margin", "width"], {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen
        })
    },
    menuButton: {
        marginTop: 8,
        marginRight: 8
    },
    hide: {
        display: "none"
    },
    drawer: {
        width: drawerWidth,
        flexShrink: 0
    },
    drawerPaper: {
        top: 135,
        width: drawerWidth,
        borderTopWidth: 1,
        borderTopStyle: "solid",
        borderTopColor: Grey[200]
    },
    drawerHeader: {
        display: "flex",
        alignItems: "center",
        padding: 5,
        justifyContent: "flex-start",
        textTransform: "uppercase",
        minHeight: "fit-content"
    },
    content: {
        flexGrow: 1,
        padding: theme.spacing.unit * 3,
        transition: theme.transitions.create("margin", {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen
        }),
        marginLeft: Number(theme.spacing.unit),
        marginRight: -drawerWidth + theme.spacing.unit
    },
    contentShift: {
        transition: theme.transitions.create("margin", {
            easing: theme.transitions.easing.easeOut,
            duration: theme.transitions.duration.enteringScreen
        }),
        marginRight: theme.spacing.unit
    },
    sideBarHeading: {
        letterSpacing: 1,
        fontSize: 12,
        marginLeft: 4
    },
    btnLegend: {
        float: "right",
        position: "sticky",
        bottom: 20,
        marginTop: 10,
        fontSize: 12
    },
    legendContent: {
        padding: theme.spacing.unit * 2
    },
    legendText: {
        display: "inline-flex",
        marginLeft: 5,
        fontSize: 12
    },
    legendIcon: {
        verticalAlign: "middle",
        marginLeft: 20
    },
    legendFirstEl: {
        verticalAlign: "middle"
    },
    graphContainer: {
        display: "flex"
    },
    diagram: {
        padding: theme.spacing.unit * 3,
        flexGrow: 1
    }
});

class Overview extends React.Component {

    constructor(props) {
        super(props);

        this.defaultState = {
            summary: {
                topic: "Cellery Deployment",
                content: [
                    {
                        key: "Total",
                        value: 0
                    },
                    {
                        key: "Successful",
                        value: 0
                    },
                    {
                        key: "Failed",
                        value: 0
                    },
                    {
                        key: "Warning",
                        value: 0
                    }
                ]
            },
            request: {
                statusCodes: [
                    {
                        key: "Total",
                        value: 0
                    },
                    {
                        key: "OK",
                        value: 0
                    },
                    {
                        key: "3xx",
                        value: 0
                    },
                    {
                        key: "4xx",
                        value: 0
                    },
                    {
                        key: "5xx",
                        value: 0
                    },
                    {
                        key: "Unknown",
                        value: 0
                    }
                ],
                cellStats: []
            },
            healthInfo: [],
            selectedCell: null,
            data: {
                nodes: null,
                edges: null
            },
            error: null,
            open: true,
            legend: null,
            legendOpen: false,
            listData: [],
            page: 0,
            rowsPerPage: 5
        };

        const queryParams = HttpUtils.parseQueryParams(props.location.search);
        this.state = {
            ...JSON.parse(JSON.stringify(this.defaultState)),
            selectedCell: queryParams.cell ? queryParams.cell : null,
            isLoading: true
        };
    }

    getCellState = (nodeId) => {
        const healthInfo = this.defaultState.healthInfo.find((element) => element.nodeId === nodeId);
        return healthInfo.status;
    };

    onClickCell = (nodeId, isUserAction) => {
        const {globalState, history, match, location} = this.props;
        const fromTime = QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).startTime);
        const toTime = QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).endTime);

        // Updating the Browser URL
        const queryParamsString = HttpUtils.generateQueryParamString({
            ...HttpUtils.parseQueryParams(location.search),
            cell: nodeId
        });
        history.replace(match.url + queryParamsString, {
            ...location.state
        });

        const search = {
            queryStartTime: fromTime.valueOf(),
            queryEndTime: toTime.valueOf(),
            timeGranularity: QueryUtils.getTimeGranularity(fromTime, toTime)
        };
        if (isUserAction) {
            NotificationUtils.showLoadingOverlay(`Loading ${nodeId} Cell Information`, globalState);
        }
        HttpUtils.callObservabilityAPI(
            {
                url: `/http-requests/cells/${nodeId}/components${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            this.props.globalState
        ).then((response) => {
            const cell = this.state.data.nodes.find((element) => element.id === nodeId);
            const componentHealth = this.getComponentHealth(cell.components, response);
            const componentHealthCount = this.getHealthCount(componentHealth);
            const statusCodeContent = this.getStatusCodeContent(nodeId, this.defaultState.request.cellStats);
            const componentInfo = this.loadComponentsInfo(cell.components, componentHealth);
            this.setState((prevState) => ({
                summary: {
                    ...prevState.summary,
                    topic: nodeId,
                    content: [
                        {
                            key: "Total",
                            value: componentInfo.length
                        },
                        {
                            key: "Successful",
                            value: componentHealthCount.success
                        },
                        {
                            key: "Failed",
                            value: componentHealthCount.error
                        },
                        {
                            key: "Warning",
                            value: componentHealthCount.warning
                        }
                    ]
                },
                data: {...prevState.data},
                listData: componentInfo,
                selectedCell: cell.id,
                request: {
                    ...prevState.request,
                    statusCodes: statusCodeContent
                }
            }));
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
            }
        }).catch((error) => {
            this.setState({error: error});
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                NotificationUtils.showNotification(
                    `Failed to load ${nodeId} request statistics`,
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    getComponentHealth = (components, responseCodeStats) => {
        const {globalState} = this.props;
        const config = globalState.get(StateHolder.CONFIG);
        const healthInfo = [];
        components.forEach((component) => {
            const total = this.getTotalComponentRequests(component, responseCodeStats, "*");
            if (total === 0) {
                healthInfo.push({nodeId: component, status: Constants.Status.Success, percentage: 1});
            } else {
                const error = this.getTotalComponentRequests(component, responseCodeStats, "5xx");
                const successPercentage = 1 - (error / total);

                if (successPercentage > config.percentageRangeMinValue.warningThreshold) {
                    healthInfo.push({
                        nodeId: component,
                        status: Constants.Status.Success,
                        percentage: successPercentage
                    });
                } else if (successPercentage > config.percentageRangeMinValue.errorThreshold) {
                    healthInfo.push({
                        nodeId: component,
                        status: Constants.Status.Warning,
                        percentage: successPercentage
                    });
                } else {
                    healthInfo.push({
                        nodeId: component,
                        status: Constants.Status.Error,
                        percentage: successPercentage
                    });
                }
            }
        });
        return healthInfo;
    };

    onClickGraph = () => {
        const {history, match, location} = this.props;

        // Updating the Browser URL
        const queryParamsString = HttpUtils.generateQueryParamString({
            ...HttpUtils.parseQueryParams(location.search),
            cell: undefined
        });
        history.replace(match.url + queryParamsString, {
            ...location.state
        });

        const defaultState = JSON.parse(JSON.stringify(this.defaultState));
        this.setState((prevState) => ({
            ...prevState,
            summary: defaultState.summary,
            listData: this.loadCellInfo(defaultState.data.nodes),
            selectedCell: null,
            request: defaultState.request
        }));
    };

    handleDrawerOpen = () => {
        this.setState({open: true});
    };

    handleDrawerClose = () => {
        this.setState({open: false});
    };

    loadCellInfo = (nodes) => {
        const nodeInfo = [];
        nodes.forEach((node) => {
            const healthInfo = this.defaultState.healthInfo.find((element) => element.nodeId === node.id);
            nodeInfo.push([healthInfo.percentage, node.id, node.id]);
        });
        return nodeInfo;
    };

    loadComponentsInfo = (components, healthInfo) => {
        const componentInfo = [];
        components.forEach((component) => {
            const healthElement = healthInfo.find((element) => element.nodeId === component);
            componentInfo.push([healthElement.percentage, component, component]);
        });
        return componentInfo;
    };

    callOverviewInfo = (isUserAction, fromTime, toTime) => {
        const {colorGenerator, globalState} = this.props;
        const {selectedCell} = this.state;
        const self = this;

        const search = {};
        if (fromTime && toTime) {
            search.queryStartTime = fromTime.valueOf();
            search.queryEndTime = toTime.valueOf();
        }
        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Cell Dependencies", globalState);
        }
        HttpUtils.callObservabilityAPI(
            {
                url: `/dependency-model/cells${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            globalState
        ).then((result) => {
            const {nodes, edges} = result;

            self.defaultState.healthInfo = self.getCellHealth(nodes);
            const healthCount = self.getHealthCount(self.defaultState.healthInfo);
            const summaryContent = [
                {
                    key: "Total",
                    value: nodes.length
                },
                {
                    key: "Successful",
                    value: healthCount.success
                },
                {
                    key: "Failed",
                    value: healthCount.error
                },
                {
                    key: "Warning",
                    value: healthCount.warning
                }
            ];
            self.defaultState.summary.content = summaryContent;
            self.defaultState.data.nodes = nodes;
            self.defaultState.data.edges = edges;
            colorGenerator.addKeys(nodes);
            const cellList = self.loadCellInfo(nodes);


            self.setState((prevState) => ({
                data: {
                    nodes: nodes,
                    edges: edges
                },
                summary: {
                    ...prevState.summary,
                    content: summaryContent
                },
                listData: cellList,
                isLoading: false // From this point onwards the underlying content is always shown in the overlay
            }));
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
            }
            if (selectedCell) {
                self.onClickCell(selectedCell, isUserAction);
            }
        }).catch((error) => {
            self.setState({error: error});
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                NotificationUtils.showNotification(
                    "Failed to load Cell Dependencies",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    getHealthCount = (healthInfo) => {
        let successCount = 0;
        let warningCount = 0;
        let errorCount = 0;
        healthInfo.forEach((info) => {
            if (info.status === Constants.Status.Success) {
                successCount += 1;
            } else if (info.status === Constants.Status.Warning) {
                warningCount += 1;
            } else {
                errorCount += 1;
            }
        });
        return {success: successCount, warning: warningCount, error: errorCount};
    };

    getCellHealth = (nodes) => {
        const {globalState} = this.props;
        const config = globalState.get(StateHolder.CONFIG);
        const healthInfo = [];
        nodes.forEach((node) => {
            const total = this.getTotalRequests(node.id, this.defaultState.request.cellStats, "*");
            if (total === 0) {
                healthInfo.push({nodeId: node.id, status: Constants.Status.Success, percentage: 1});
            } else {
                const error = this.getTotalRequests(node.id, this.defaultState.request.cellStats, "5xx");
                const successPercentage = 1 - (error / total);
                if (successPercentage > config.percentageRangeMinValue.warningThreshold) {
                    healthInfo.push({nodeId: node.id, status: Constants.Status.Success, percentage: successPercentage});
                } else if (successPercentage > config.percentageRangeMinValue.errorThreshold) {
                    healthInfo.push({nodeId: node.id, status: Constants.Status.Warning, percentage: successPercentage});
                } else {
                    healthInfo.push({nodeId: node.id, status: Constants.Status.Error, percentage: successPercentage});
                }
            }
        });
        return healthInfo;
    };

    loadOverviewOnTimeUpdate = (isUserAction, fromTime, toTime) => {
        const {globalState} = this.props;
        const search = {
            queryStartTime: fromTime.valueOf(),
            queryEndTime: toTime.valueOf(),
            timeGranularity: QueryUtils.getTimeGranularity(fromTime, toTime)
        };
        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Cell Metadata", globalState);
        }
        HttpUtils.callObservabilityAPI(
            {
                url: `/http-requests/cells${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            this.props.globalState
        ).then((response) => {
            const statusCodeContent = this.getStatusCodeContent(null, response);
            this.defaultState.request.statusCodes = statusCodeContent;
            this.defaultState.request.cellStats = response;
            this.setState((prevState) => ({
                stats: {
                    cellStats: response
                },
                request: {
                    ...prevState.request,
                    statusCodes: statusCodeContent
                }
            }));
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
            }
            this.callOverviewInfo(isUserAction, fromTime, toTime);
        }).catch((error) => {
            this.setState({error: error});
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                NotificationUtils.showNotification(
                    "Failed to load Cell Metadata",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    getStatusCodeContent = (cell, data) => {
        const total = this.getTotalRequests(cell, data, "*");
        const response2xx = this.getTotalRequests(cell, data, "2xx");
        const response3xx = this.getTotalRequests(cell, data, "3xx");
        const response4xx = this.getTotalRequests(cell, data, "4xx");
        const response5xx = this.getTotalRequests(cell, data, "5xx");
        const responseUnknown = total - (response2xx + response3xx + response4xx + response5xx);
        return [
            {
                key: "Total",
                value: total
            },
            {
                key: "OK",
                count: response2xx,
                value: this.getPercentage(response2xx, total)
            },
            {
                key: "3xx",
                count: response3xx,
                value: this.getPercentage(response3xx, total)
            },
            {
                key: "4xx",
                count: response4xx,
                value: this.getPercentage(response4xx, total)
            },
            {
                key: "5xx",
                count: response5xx,
                value: this.getPercentage(response5xx, total)
            },
            {
                key: "Unknown",
                count: responseUnknown,
                value: this.getPercentage(responseUnknown, total)
            }
        ];
    };

    getPercentage = (responseCode, total) => {
        if (total !== 0) {
            return Math.round((responseCode / total) * 100);
        }
        return 0;
    };

    getTotalRequests = (cell, stats, responseCode) => {
        let total = 0;
        stats.forEach((stat) => {
            if (stat[1] !== "") {
                if (!cell || cell === stat[1]) {
                    if (responseCode === "*") {
                        total += stat[4];
                    } else if (responseCode === stat[2]) {
                        total += stat[4];
                    }
                }
            }
        });
        return total;
    };

    getTotalComponentRequests = (cell, stats, responseCode) => {
        let total = 0;
        stats.forEach((stat) => {
            if (stat[2] !== "") {
                if (!cell || cell === stat[2]) {
                    if (responseCode === "*") {
                        total += stat[6];
                    } else if (responseCode === stat[4]) {
                        total += stat[6];
                    }
                }
            }
        });
        return total;
    };

    handleClick = (event) => {
        const {currentTarget} = event;
        this.setState((state) => ({
            legend: currentTarget,
            legendOpen: !state.legendOpen
        }));
    };

    render = () => {
        const {classes, theme, colorGenerator} = this.props;
        const {open, selectedCell, legend, legendOpen, isLoading} = this.state;
        const id = legendOpen ? "legend-popper" : null;
        const percentageVal = this.props.globalState.get(StateHolder.CONFIG).percentageRangeMinValue;
        const isDataAvailable = this.state.data.nodes && this.state.data.nodes.length > 0;

        const viewGenerator = (nodeId, opacity) => {
            const color = ColorGenerator.shadeColor(colorGenerator.getColor(nodeId), opacity);
            const outlineColor = ColorGenerator.shadeColor(color, -0.08);
            const errorColor = ColorGenerator.shadeColor(colorGenerator.getColor(ColorGenerator.ERROR), opacity);
            const warningColor = ColorGenerator.shadeColor(colorGenerator.getColor(ColorGenerator.WARNING), opacity);
            const state = this.getCellState(nodeId);

            const successCell = '<svg xmlns="http://www.w3.org/2000/svg" x="0px" y="0px" width="100%" height="100%" viewBox="0 0 14 14">'
                + `<path fill="${color}"  stroke="${(selectedCell === nodeId) ? "#444" : outlineColor}" stroke-opacity="${1 - opacity}" `
                + ' stroke-width="0.5px" d="M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,1V9a1.43,1.43,0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,.41H8.92a1.4,1.4,0,0,0,1-.41L12.72,10a1.46,1.46,0,0,0,.41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,0,0,8.92.84Z" transform="translate(-0.54 -0.37)"/>'
                + "</svg>";

            const errorCell = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14"><g>'
                + `<path fill="${color}" stroke="${(selectedCell === nodeId) ? "#444" : outlineColor}" stroke-opacity="${1 - opacity}" `
                + ' stroke-width="0.5px" d="M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,1V9a1.43,1.43,0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,.41H8.92a1.4,1.4,0,0,0,1-.41L12.72,10a1.46,1.46,0,0,0,.41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,0,0,8.92.84Z" transform="translate(-0.54 -0.37)"/></g>'
                + `<path fill="${errorColor}" d="M11.17.5a2.27,2.27,0,1,0,2.26,2.26A2.27,2.27,0,0,0,11.17.5Z" transform="translate(-0.54 -0.37)"/>`
                + '<path fill="#fff" d="M11.17,5.15a2.39,2.39,0,1,1,2.38-2.39A2.39,2.39,0,0,1,11.17,5.15Zm0-4.53A2.14,2.14,0,1,0,13.3,2.76,2.14,2.14,0,0,0,11.17.62Z" transform="translate(-0.54 -0.37)"/>'
                + '<path fill="#fff" d="M10.86,3.64h.61v.61h-.61Zm0-2.44h.61V3h-.61Z" transform="translate(-0.54 -0.37)"/>'
                + "</svg>";

            const warningCell = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14"><g>'
                + `<path fill="${color}" stroke="${(selectedCell === nodeId) ? "#444" : outlineColor}" stroke-opacity="${1 - opacity}" `
                + 'stroke-width="0.5px" d="M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,1V9a1.43,1.43,0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,.41H8.92a1.4,1.4,0,0,0,1-.41L12.72,10a1.46,1.46,0,0,0,.41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,0,0,8.92.84Z" transform="translate(-0.54 -0.37)"/></g>'
                + `<path fill="${warningColor}" d="M11.17.5a2.27,2.27,0,1,0,2.26,2.26A2.27,2.27,0,0,0,11.17.5Z" transform="translate(-0.54 -0.37)"/>`
                + '<path fill="#fff" d="M11.17,5.15a2.39,2.39,0,1,1,2.38-2.39A2.39,2.39,0,0,1,11.17,5.15Zm0-4.53A2.14,2.14,0,1,0,13.3,2.76,2.14,2.14,0,0,0,11.17.62Z" transform="translate(-0.54 -0.37)"/>'
                + '<path fill="#fff" d="M10.86,3.64h.61v.61h-.61Zm0-2.44h.61V3h-.61Z" transform="translate(-0.54 -0.37)"/>'
                + "</svg>";

            let cellView;
            if (state === Constants.Status.Success) {
                cellView = successCell;
            } else if (state === Constants.Status.Warning) {
                cellView = warningCell;
            } else {
                cellView = errorCell;
            }

            return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(cellView)}`;
        };

        const dataNodes = this.state.data.nodes;
        const dataEdges = this.state.data.edges;

        return (
            <React.Fragment>
                <TopToolbar title={"Overview"} onUpdate={this.loadOverviewOnTimeUpdate}/>
                {
                    isLoading
                        ? null
                        : (
                            <div className={classes.root}>
                                <Paper className={classNames(classes.content, {
                                    [classes.contentShift]: open
                                })}>
                                    {
                                        isDataAvailable
                                            ? (
                                                <React.Fragment>
                                                    <div className={classes.graphContainer}>
                                                        <div className={classes.diagram}>
                                                            <DependencyGraph id="graph-id" nodeData={dataNodes} edgeData={dataEdges}
                                                                onClickNode={(nodeId) => this.onClickCell(nodeId, true)} viewGenerator={viewGenerator}
                                                                onClickGraph={this.onClickGraph} selectedCell={selectedCell} graphType="overview"
                                                            />
                                                        </div>
                                                    </div>
                                                    <Button aria-describedby={id} variant="outlined"
                                                        className={classes.btnLegend} onClick={this.handleClick}>
                                                        Legend
                                                    </Button>
                                                </React.Fragment>
                                            )
                                            : (
                                                <NotFound title={"No Cells Found"}
                                                    description={"No Requests were sent within the selected time range"}
                                                />
                                            )
                                    }
                                    <Popper id={id} open={legendOpen} anchorEl={legend} placement="top-end" transition>
                                        {({TransitionProps}) => (
                                            <Fade {...TransitionProps} timeout={350}>
                                                <Paper>
                                                    <div className={classes.legendContent}>
                                                        <CellIcon className={classes.legendFirstEl} color="action"
                                                            fontSize="small"/>
                                                        <Typography color="inherit"
                                                            className={classes.legendText}> Cell</Typography>
                                                        <ArrowRightAltSharp className={classes.legendIcon}
                                                            color="action"/>
                                                        <Typography color="inherit"
                                                            className={classes.legendText}> Dependency</Typography>
                                                        <Error className={classes.legendIcon}
                                                            style={{
                                                                color: colorGenerator.getColor(ColorGenerator.WARNING)
                                                            }}/>
                                                        <Typography color="inherit" className={classes.legendText}>
                                                            {Math.round((1 - percentageVal.warningThreshold) * 100)}%
                                                            - {Math.round((1 - percentageVal.errorThreshold) * 100)}%
                                                            Error </Typography>
                                                        <Error className={classes.legendIcon} color="error"/>
                                                        <Typography color="inherit" className={classes.legendText}>
                                                            &gt;
                                                            {Math.round((1 - percentageVal.errorThreshold) * 100)}%
                                                            Error
                                                        </Typography>
                                                    </div>
                                                </Paper>
                                            </Fade>
                                        )}
                                    </Popper>
                                </Paper>
                                {
                                    isDataAvailable
                                        ? (
                                            <React.Fragment>
                                                <div className={classNames(classes.moreDetails, {
                                                    [classes.moreDetailsShift]: open
                                                })}>
                                                    <IconButton color="inherit" aria-label="Open drawer"
                                                        onClick={this.handleDrawerOpen}
                                                        className={classNames(classes.menuButton, open && classes.hide)}
                                                    >
                                                        <MoreIcon/>
                                                    </IconButton>
                                                </div>

                                                <Drawer className={classes.drawer} variant="persistent" anchor="right"
                                                    open={open}
                                                    classes={{
                                                        paper: classes.drawerPaper
                                                    }}>
                                                    <div className={classes.drawerHeader}>
                                                        <IconButton onClick={this.handleDrawerClose}>
                                                            {
                                                                theme.direction === "rtl"
                                                                    ? <ChevronLeftIcon/>
                                                                    : <ChevronRightIcon/>
                                                            }
                                                        </IconButton>
                                                        <Typography color="textSecondary"
                                                            className={classes.sideBarHeading}>
                                                            {selectedCell ? "Cell Details" : "Overview"}
                                                        </Typography>
                                                    </div>
                                                    <Divider/>
                                                    <SidePanelContent summary={this.state.summary}
                                                        request={this.state.request} selectedCell={selectedCell}
                                                        open={this.state.open} listData={this.state.listData}/>
                                                </Drawer>
                                            </React.Fragment>

                                        )
                                        : null
                                }
                            </div>
                        )
                }
            </React.Fragment>
        );
    };

}

Overview.propTypes = {
    classes: PropTypes.object.isRequired,
    theme: PropTypes.object.isRequired,
    history: PropTypes.shape({
        replace: PropTypes.func.isRequired
    }).isRequired,
    location: PropTypes.shape({
        search: PropTypes.string.isRequired
    }).isRequired,
    match: PropTypes.shape({
        url: PropTypes.string.isRequired
    }).isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired,
    colorGenerator: PropTypes.instanceOf(ColorGenerator)
};

export default withStyles(styles, {withTheme: true})(withGlobalState(withColor(Overview)));
