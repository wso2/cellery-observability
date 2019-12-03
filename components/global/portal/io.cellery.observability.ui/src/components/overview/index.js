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

/* eslint max-lines: ["error", 800] */

import ArrowRightAltSharp from "@material-ui/icons/ArrowRightAltSharp";
import Button from "@material-ui/core/Button";
import CellIcon from "../../icons/CellIcon";
import ChevronLeftIcon from "@material-ui/icons/ChevronLeft";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import CompositeIcon from "../../icons/CompositeIcon";
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
import ReactDOMServer from "react-dom/server";
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
        display: "flex",
        height: "75vh",
        width: "100%"
    },
    diagram: {
        padding: theme.spacing.unit * 3,
        flexGrow: 1
    }
});

class Overview extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            dependencyDiagram: {
                nodes: [],
                edges: []
            },
            metrics: {
                overall: {},
                selectedInstance: {}
            },
            selectedInstance: null,
            isLoading: true,
            legend: {
                isOpen: false,
                target: null
            },
            isSidePanelOpen: true
        };
    }

    static getDerivedStateFromProps = (props, state) => {
        const queryParams = HttpUtils.parseQueryParams(props.location.search);
        return {
            ...state,
            selectedInstance: queryParams ? queryParams.cell : null
        };
    };

    handleOnUpdate = (isUserAction, startTime, endTime) => {
        const self = this;
        const {globalState} = self.props;
        const {selectedInstance} = self.state;
        const dependencyDiagramFilter = {
            queryStartTime: startTime.valueOf(),
            queryEndTime: endTime.valueOf()
        };
        const metricsFilter = {
            queryStartTime: startTime.valueOf(),
            queryEndTime: endTime.valueOf(),
            timeGranularity: QueryUtils.getTimeGranularity(startTime, endTime)
        };

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Instance Dependencies", globalState);
        }
        self.setState({
            isLoading: true
        });
        const apiCalls = [
            HttpUtils.callObservabilityAPI(
                {
                    url: `/dependency-model/instances${HttpUtils.generateQueryParamString(dependencyDiagramFilter)}`,
                    method: "GET"
                },
                globalState
            ),
            HttpUtils.callObservabilityAPI(
                {
                    url: `/k8s/instances${HttpUtils.generateQueryParamString(dependencyDiagramFilter)}`,
                    method: "GET"
                },
                globalState
            ),
            HttpUtils.callObservabilityAPI(
                {
                    url: `/http-requests/instances${HttpUtils.generateQueryParamString(metricsFilter)}`,
                    method: "GET"
                },
                this.props.globalState
            )
        ];
        if (selectedInstance) {
            const instanceMetricsFilter = {
                queryStartTime: startTime.valueOf(),
                queryEndTime: endTime.valueOf(),
                timeGranularity: QueryUtils.getTimeGranularity(startTime, endTime),
                instanceName: selectedInstance
            };
            const queryParams = HttpUtils.generateQueryParamString(instanceMetricsFilter);
            apiCalls.push(HttpUtils.callObservabilityAPI(
                {
                    url: `/http-requests/instances/${selectedInstance}/components${queryParams}`,
                    method: "GET"
                },
                self.props.globalState
            ));
        }
        Promise.all(apiCalls).then((data) => {
            // Extracting unique node data map
            const ingressTypes = data[1].map((datum) => ({
                instance: datum[0],
                component: datum[1],
                ingressTypes: datum[3]
            }));
            const nodeData = data[0].nodes.filter((nodeDatum) => Boolean(nodeDatum.instance))
                .map((nodeDatum) => {
                    // Extracting the ingress type for the relevant node
                    nodeDatum.ingressTypes = ingressTypes
                        .filter((ingressTypesDatum) => nodeDatum.instance === ingressTypesDatum.instance
                            && nodeDatum.component === ingressTypesDatum.component)
                        .reduce((ingressTypesSet, ingressTypesDatum) => {
                            if (ingressTypesDatum.ingressTypes) {
                                ingressTypesSet.add(ingressTypesDatum.ingressTypes);
                            }
                            return new Set(ingressTypesSet);
                        }, new Set());
                    return nodeDatum;
                });
            const nodeDataMap = {};
            nodeData.forEach((nodeDatum) => {
                if (!nodeDataMap[nodeDatum.instance]) {
                    nodeDataMap[nodeDatum.instance] = {
                        id: nodeDatum.instance,
                        instanceKind: nodeDatum.instanceKind,
                        ingressTypes: Array.from(nodeDatum.ingressTypes)
                    };
                }
            });

            // Extracting unique edge data map
            const edgeDataMap = {};
            data[0].edges.filter((edgeDatum) => edgeDatum.source.instance !== edgeDatum.target.instance)
                .forEach((edgeDatum) => {
                    edgeDataMap[`${edgeDatum.source.instance} --> ${edgeDatum.target.instance}`] = {
                        source: edgeDatum.source.instance,
                        target: edgeDatum.target.instance
                    };
                });

            const dependencyDiagram = {
                nodes: Object.values(nodeDataMap),
                edges: Object.values(edgeDataMap)
            };

            // Calculating metrics
            const metrics = {
                overall: self.calculateOverallMetrics(data[2].map((datum) => ({
                    sourceInstance: datum[0],
                    sourceInstanceKind: datum[1],
                    destinationInstance: datum[2],
                    destinationInstanceKind: datum[3],
                    httpResponseGroup: datum[4],
                    totalResponseTimeMilliSec: datum[5],
                    requestCount: datum[6]
                })))
            };
            if (selectedInstance && data.length === 4) { // An instance had been selected
                metrics.selectedInstance = self.calculateInstanceMetrics(data[3].map((datum) => ({
                    sourceInstance: datum[0],
                    sourceComponent: datum[1],
                    destinationInstance: datum[2],
                    destinationComponent: datum[3],
                    httpResponseGroup: datum[4],
                    totalResponseTimeMilliSec: datum[5],
                    requestCount: datum[6]
                })));
            } else {
                metrics.selectedInstance = {};
            }
            // Adding empty metrics entries for nodes without incoming metrics
            nodeData.forEach((nodeDatum) => {
                if (!metrics.overall[nodeDatum.instance]) {
                    metrics.overall[nodeDatum.instance] = this.generateEmptyMetricsEntry();
                }
                if (selectedInstance === nodeDatum.instance && !metrics.selectedInstance[nodeDatum.component]) {
                    metrics.selectedInstance[nodeDatum.component] = this.generateEmptyMetricsEntry();
                }
            });

            self.setState({
                dependencyDiagram: dependencyDiagram,
                metrics: metrics,
                isLoading: false
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
            }
        }).catch(() => {
            self.setState({
                isLoading: false
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                NotificationUtils.showNotification(
                    "Failed to load Overview",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    calculateOverallMetrics = (metricsEntries) => metricsEntries
        .filter((metricsEntry) => metricsEntry.destinationInstance)
        .reduce((aggregatedMetrics, currentEntry) => {
            let aggregatedMetricsEntry = aggregatedMetrics[currentEntry.destinationInstance];
            if (!aggregatedMetricsEntry) {
                aggregatedMetricsEntry = this.generateEmptyMetricsEntry();
                aggregatedMetrics[currentEntry.destinationInstance] = aggregatedMetricsEntry;
            }
            aggregatedMetricsEntry.totalIncomingRequests += currentEntry.requestCount;
            aggregatedMetricsEntry.responseCounts[currentEntry.httpResponseGroup] += currentEntry.requestCount;
            return aggregatedMetrics;
        }, {});

    calculateInstanceMetrics = (metricsEntries) => {
        const {selectedInstance} = this.state;
        return metricsEntries.filter((metricsDatum) => metricsDatum.destinationInstance === selectedInstance)
            .reduce((aggregatedMetrics, currentEntry) => {
                let aggregatedMetricsEntry = aggregatedMetrics[currentEntry.destinationComponent];
                if (!aggregatedMetricsEntry) {
                    aggregatedMetricsEntry = this.generateEmptyMetricsEntry();
                    aggregatedMetrics[currentEntry.destinationComponent] = aggregatedMetricsEntry;
                }
                aggregatedMetricsEntry.totalIncomingRequests += currentEntry.requestCount;
                aggregatedMetricsEntry.responseCounts[currentEntry.httpResponseGroup] += currentEntry.requestCount;
                return aggregatedMetrics;
            }, {});
    };

    generateEmptyMetricsEntry = () => ({
        totalIncomingRequests: 0,
        responseCounts: {
            "0xx": 0,
            "2xx": 0,
            "3xx": 0,
            "4xx": 0,
            "5xx": 0
        }
    });

    onSelectedInstanceChange = (nodeId) => {
        const {history, match, location} = this.props;

        // Updating the Browser URL
        const queryParamsString = HttpUtils.generateQueryParamString({
            ...HttpUtils.parseQueryParams(location.search),
            cell: nodeId
        });
        history.replace(match.url + queryParamsString, {
            ...location.state
        });
    };

    handleSidePanelOpen = () => {
        this.setState({
            isSidePanelOpen: true
        });
    };

    handleSidePanelClose = () => {
        this.setState({
            isSidePanelOpen: false
        });
    };

    handleLegendButtonClick = (event) => {
        const currentTarget = event.currentTarget;
        this.setState((prevState) => ({
            legend: {
                isOpen: !prevState.legend.isOpen,
                target: currentTarget
            }
        }));
    };

    componentDidUpdate(prevProps, prevState, snapshot) {
        const {globalState} = this.props;
        const {selectedInstance} = this.state;
        const fromTime = QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).startTime);
        const toTime = QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).endTime);

        if (selectedInstance !== prevState.selectedInstance) {
            this.handleOnUpdate(true, fromTime, toTime);
        }
    }

    render = () => {
        const {classes, theme, colorGenerator, globalState} = this.props;
        const {
            selectedInstance, selectedInstanceKind, dependencyDiagram, legend, isSidePanelOpen, isLoading, metrics
        } = this.state;

        const legendId = legend.isOpen ? "legend-popper" : null;
        const percentageRangeMinValue = this.props.globalState.get(StateHolder.CONFIG).percentageRangeMinValue;
        const isDataAvailable = dependencyDiagram.nodes.length && dependencyDiagram.nodes.length > 0;

        const getStatusForPercentage = (percentage) => {
            let status = Constants.Status.Success;
            if (percentage < globalState.get(StateHolder.CONFIG).percentageRangeMinValue.warningThreshold) {
                status = Constants.Status.Warning;
            }
            if (percentage < globalState.get(StateHolder.CONFIG).percentageRangeMinValue.errorThreshold) {
                status = Constants.Status.Error;
            }
            if (percentage < 0 || percentage > 1) {
                status = Constants.Status.Unknown;
            }
            return status;
        };

        const viewGenerator = (nodeId, opacity, instanceKind) => {
            const color = ColorGenerator.shadeColor(colorGenerator.getColor(nodeId), opacity);
            const outlineColor = ColorGenerator.shadeColor(color, -0.08);
            const errorColor = ColorGenerator.shadeColor(colorGenerator.getColor(ColorGenerator.ERROR), opacity);
            const warningColor = ColorGenerator.shadeColor(colorGenerator.getColor(ColorGenerator.WARNING), opacity);

            const totalErrorRequests = metrics.overall[nodeId].responseCounts["5xx"];
            const successPercentage = 1 - (totalErrorRequests / metrics.overall[nodeId].requestCount);
            const state = getStatusForPercentage(successPercentage);

            let instanceView;
            if (instanceKind === Constants.InstanceKind.CELL) {
                const successCell = (
                    <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink"
                        x="0px" y="0px" width="14px" height="14px" viewBox="0 0 14 14"
                        style={{enableBackground: "new 0 0 14 14"}} xmlSpace="preserve">
                        <path fill={color} stroke={(selectedInstance === nodeId) ? "#444" : outlineColor}
                            strokeOpacity={1 - opacity} strokeWidth="0.5px"
                            d={"M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,1V9a1.43,"
                                + "1.43,0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,.41H8.92a1.4,1.4,0,0,0,1-.41L12.72,"
                                + "10a1.46,1.46,0,0,0,.41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,0,0,8.92.84Z"}
                            transform="translate(-0.54 -0.37)"/>
                    </svg>
                );

                const errorCell = (
                    <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink"
                        x="0px" y="0px" width="14px" height="14px" viewBox="0 0 14 14"
                        style={{enableBackground: "new 0 0 14 14"}} xmlSpace="preserve">
                        <g>
                            <path fill={color} stroke={(selectedInstance === nodeId) ? "#444" : outlineColor}
                                strokeOpacity={1 - opacity} strokeWidth="0.5px"
                                d={"M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,1V9a1.43,1.43,"
                                    + "0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,.41H8.92a1.4,1.4,0,0,0,1-.41L12.72,"
                                    + "10a1.46,1.46,0,0,0,.41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,"
                                    + "0,0,8.92.84Z"}
                                transform="translate(-0.54 -0.37)"/>
                        </g>
                        <path fill={errorColor} d="M11.17.5a2.27,2.27,0,1,0,2.26,2.26A2.27,2.27,0,0,0,11.17.5Z"
                            transform="translate(-0.54 -0.37)"/>
                        <path fill="#fff"
                            d={"M11.17,5.15a2.39,2.39,0,1,1,2.38-2.39A2.39,2.39,0,0,1,11.17,5.15Zm0-4.53A2.14,"
                                + "2.14,0,1,0,13.3,2.76,2.14,2.14,0,0,0,11.17.62Z"} transform="translate(-0.54 -0.37)"/>
                        <path fill="#fff" d="M10.86,3.64h.61v.61h-.61Zm0-2.44h.61V3h-.61Z"
                            transform="translate(-0.54 -0.37)"/>
                    </svg>
                );

                const warningCell = (
                    <svg version="1.1" xmlns="http://www.w3.org/2000/svg"
                        xmlnsXlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="14px" height="14px"
                        viewBox="0 0 14 14" style={{enableBackground: "new 0 0 14 14"}} xmlSpace="preserve">
                        <g>
                            <path fill={color}
                                stroke={(selectedInstance === nodeId) ? "#444" : outlineColor}
                                strokeOpacity={1 - opacity} strokeWidth="0.5px"
                                d={"M8.92.84H5a1.45,1.45,0,0,0-1,.42L1.22,4a1.43,1.43,0,0,0-.43,"
                                    + "1V9a1.43,1.43,0,0,0,.43,1L4,12.75a1.4,1.4,0,0,0,1,"
                                    + ".41H8.92a1.4,1.4,0,0,0,1-.41L12.72,10a1.46,1.46,0,0,0,"
                                    + ".41-1V5a1.46,1.46,0,0,0-.41-1L9.94,1.25A1.44,1.44,0,0,0,8.92.84Z"}
                                transform="translate(-0.54 -0.37)"/>
                        </g>
                        <path fill={warningColor} d="M11.17.5a2.27,2.27,0,1,0,2.26,2.26A2.27,2.27,0,0,0,11.17.5Z"
                            transform="translate(-0.54 -0.37)"/>
                        <path fill="#fff" transform="translate(-0.54 -0.37)"
                            d={"M11.17,5.15a2.39,2.39,0,1,1,2.38-2.39A2.39,2.39,0,0,1,11.17,"
                                + "5.15Zm0-4.53A2.14,2.14,0,1,0,13.3,2.76,2.14,2.14,0,0,0,11.17.62Z"}/>
                        <path fill="#fff" d="M10.86,3.64h.61v.61h-.61Zm0-2.44h.61V3h-.61Z"
                            transform="translate(-0.54 -0.37)"/>
                    </svg>
                );

                if (state === Constants.Status.Success || state === Constants.Status.Unknown) {
                    // Not to show any indicators if it is success threshold or status is unknown
                    instanceView = successCell;
                } else if (state === Constants.Status.Warning) {
                    instanceView = warningCell;
                } else {
                    instanceView = errorCell;
                }
            } else if (instanceKind === Constants.InstanceKind.COMPOSITE) {
                const successComposite = (
                    <svg version="1.1" xmlns="http://www.w3.org/2000/svg"
                        xmlnsXlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="14px" height="14px"
                        viewBox="0 0 14 14" style={{enableBackground: "new 0 0 14 14"}} xmlSpace="preserve">
                        <circle cx="6.4" cy="6.7" r="6.1" fill={color}
                            stroke={(selectedInstance === nodeId) ? "#444" : outlineColor}
                            strokeOpacity={1 - opacity} strokeDasharray="1.9772,0.9886" strokeWidth="0.5px"/>
                    </svg>
                );

                const errorComposite = (
                    <svg version="1.1" xmlns="http://www.w3.org/2000/svg"
                        xmlnsXlink="http://www.w3.org/1999/xlink" x="0px" y="0px" width="14px" height="14px"
                        viewBox="0 0 14 14" style={{enableBackground: "new 0 0 14 14"}} xmlSpace="preserve">
                        <g>
                            <circle cx="6.4" cy="6.7" r="6.1" fill={color}
                                stroke={(selectedInstance === nodeId) ? "#444" : outlineColor}
                                strokeOpacity={1 - opacity}
                                strokeWidth="0.5px" strokeDasharray="1.9772,0.9886"/>
                        </g>
                        <path fill={errorColor}
                            d="M11.17.5a2.27,2.27,0,1,0,2.26,2.26A2.27,2.27,0,0,0,11.17.5Z"
                            transform="translate(-0.54 -0.37)"/>
                        <path fill="#fff"
                            d={"M11.17,5.15a2.39,2.39,0,1,1,2.38-2.39A2.39,2.39,0,0,"
                                + "1,11.17,5.15Zm0-4.53A2.14,2.14,0,1,0,13.3,2.76,2.14,2.14,0,0,0,11.17.62Z"}
                            transform="translate(-0.54 -0.37)"/>
                        <path fill="#fff" d="M10.86,3.64h.61v.61h-.61Zm0-2.44h.61V3h-.61Z"
                            transform="translate(-0.54 -0.37)"/>
                    </svg>
                );

                const warningComposite = (
                    <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlnsXlink="http://www.w3.org/1999/xlink"
                        x="0px" y="0px" width="14px" height="14px" viewBox="0 0 14 14"
                        style={{enableBackground: "new 0 0 14 14"}} xmlSpace="preserve">
                        <g>
                            <circle cx="6.4" cy="6.7" r="6.1" fill={color}
                                stroke={(selectedInstance === nodeId) ? "#444" : outlineColor}
                                strokeOpacity={1 - opacity}
                                strokeWidth="0.5px" strokeDasharray="1.9772,0.9886"/>
                        </g>
                        <path fill={warningColor}
                            d="M11.17.5a2.27,2.27,0,1,0,2.26,2.26A2.27,2.27,0,0,0,11.17.5Z"
                            transform="translate(-0.54 -0.37)"/>
                        <path fill="#fff"
                            d={"M11.17,5.15a2.39,2.39,0,1,1,2.38-2.39A2.39,2.39,0,0,1,"
                                + "11.17,5.15Zm0-4.53A2.14,2.14,0,1,0,13.3,2.76,2.14,2.14,0,0,0,11.17.62Z"}
                            transform="translate(-0.54 -0.37)"/>
                        <path fill="#fff" d="M10.86,3.64h.61v.61h-.61Zm0-2.44h.61V3h-.61Z"
                            transform="translate(-0.54 -0.37)"/>
                    </svg>
                );

                if (state === Constants.Status.Success || state === Constants.Status.Unknown) {
                    // Not to show any indicators if it is success threshold or status is unknown
                    instanceView = successComposite;
                } else if (state === Constants.Status.Warning) {
                    instanceView = warningComposite;
                } else {
                    instanceView = errorComposite;
                }
            }
            const instanceViewHtml = ReactDOMServer.renderToStaticMarkup(instanceView);
            return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(instanceViewHtml)}`;
        };

        const renderNodeLabel = (nodeId) => {
            const node = dependencyDiagram.nodes.find((nodeDatum) => nodeDatum.id === nodeId);
            let nodeLabel;
            if (node && node.ingressTypes && node.ingressTypes.size > 0) {
                nodeLabel = `${nodeId}\n<b>(${node.ingressTypes.join(", ")})</b>`;
            } else {
                nodeLabel = nodeId;
            }
            return nodeLabel;
        };

        return (
            <React.Fragment>
                <TopToolbar title={"Overview"} onUpdate={this.handleOnUpdate}/>
                {
                    isLoading
                        ? null
                        : (
                            <div className={classes.root}>
                                <Paper className={classNames(classes.content, {
                                    [classes.contentShift]: isSidePanelOpen
                                })}>
                                    {
                                        isDataAvailable
                                            ? (
                                                <React.Fragment>
                                                    <div className={classes.graphContainer}>
                                                        <div className={classes.diagram}>
                                                            <DependencyGraph id="graph-id" graphType="overview"
                                                                nodeData={dependencyDiagram.nodes}
                                                                edgeData={dependencyDiagram.edges}
                                                                selectedInstance={selectedInstance}
                                                                onClickNode={
                                                                    (nodeId) => this.onSelectedInstanceChange(nodeId)}
                                                                onClickGraph={
                                                                    () => this.onSelectedInstanceChange(null)}
                                                                viewGenerator={viewGenerator}
                                                                renderNodeLabel={renderNodeLabel}/>
                                                        </div>
                                                    </div>
                                                    <Button aria-describedby={legendId} variant="outlined"
                                                        className={classes.btnLegend}
                                                        onClick={this.handleLegendButtonClick}>
                                                        Legend
                                                    </Button>
                                                </React.Fragment>
                                            )
                                            : (
                                                <NotFound title={"No Instances Found"}
                                                    description={"No Requests were sent within the selected time range"}
                                                />
                                            )
                                    }
                                    <Popper id={legendId} open={legend.isOpen} anchorEl={legend.target}
                                        placement={"top-end"} transition>
                                        {({TransitionProps}) => (
                                            <Fade {...TransitionProps} timeout={350}>
                                                <Paper>
                                                    <div className={classes.legendContent}>
                                                        <CellIcon className={classes.legendFirstEl} color="action"
                                                            fontSize="small"/>
                                                        <Typography color="inherit"
                                                            className={classes.legendText}> {
                                                                Constants.InstanceKind.CELL}
                                                        </Typography>
                                                        <CompositeIcon className={classes.legendIcon} color="action"
                                                            fontSize="small"/>
                                                        <Typography color="inherit"
                                                            className={classes.legendText}> {
                                                                Constants.InstanceKind.COMPOSITE}
                                                        </Typography>
                                                        <ArrowRightAltSharp className={classes.legendIcon}
                                                            color="action"/>
                                                        <Typography color="inherit"
                                                            className={classes.legendText}> Dependency</Typography>
                                                        <Error className={classes.legendIcon}
                                                            style={{
                                                                color: colorGenerator.getColor(ColorGenerator.WARNING)
                                                            }}/>
                                                        <Typography color="inherit" className={classes.legendText}>
                                                            {Math.round(
                                                                (1 - percentageRangeMinValue.warningThreshold) * 100)}%
                                                            - {Math.round(
                                                                (1 - percentageRangeMinValue.errorThreshold) * 100)}%
                                                            Errors </Typography>
                                                        <Error className={classes.legendIcon} color="error"/>
                                                        <Typography color="inherit" className={classes.legendText}>
                                                            &gt;
                                                            {Math.round(
                                                                (1 - percentageRangeMinValue.errorThreshold) * 100)}%
                                                            Errors
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
                                                    [classes.moreDetailsShift]: isSidePanelOpen
                                                })}>
                                                    <IconButton color="inherit" aria-label="Open drawer"
                                                        onClick={this.handleSidePanelOpen}
                                                        className={classNames(classes.menuButton,
                                                            isSidePanelOpen && classes.hide)}>
                                                        <MoreIcon/>
                                                    </IconButton>
                                                </div>

                                                <Drawer className={classes.drawer} variant="persistent" anchor="right"
                                                    open={isSidePanelOpen}
                                                    classes={{
                                                        paper: classes.drawerPaper
                                                    }}>
                                                    <div className={classes.drawerHeader}>
                                                        <IconButton onClick={this.handleSidePanelClose}>
                                                            {
                                                                theme.direction === "rtl"
                                                                    ? <ChevronLeftIcon/>
                                                                    : <ChevronRightIcon/>
                                                            }
                                                        </IconButton>
                                                        <Typography color="textSecondary"
                                                            className={classes.sideBarHeading}>
                                                            {selectedInstance ? "Cell Details" : "Overview"}
                                                        </Typography>
                                                    </div>
                                                    <Divider/>
                                                    <SidePanelContent selectedInstanceKind={selectedInstanceKind}
                                                        open={isSidePanelOpen} selectedInstance={selectedInstance}
                                                        metrics={
                                                            selectedInstance
                                                                ? metrics.selectedInstance
                                                                : metrics.overall
                                                        }/>
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
