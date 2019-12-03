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

/* eslint max-lines: ["error", 600] */

import "react-vis/dist/style.css";
import "./index.css";
import Avatar from "@material-ui/core/Avatar";
import CellIcon from "../../icons/CellIcon";
import CellsIcon from "../../icons/CellsIcon";
import CheckCircleOutline from "@material-ui/icons/CheckCircleOutline";
import ComponentIcon from "../../icons/ComponentIcon";
import CompositeIcon from "../../icons/CompositeIcon";
import Constants from "../../utils/constants";
import ErrorIcon from "@material-ui/icons/ErrorOutline";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import ExpansionPanelDetails from "@material-ui/core/ExpansionPanelDetails";
import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import Grey from "@material-ui/core/colors/grey";
import HealthIndicator from "../common/HealthIndicator";
import HelpOutlineIcon from "@material-ui/icons/HelpOutline";
import HttpTrafficIcon from "../../icons/HttpTrafficIcon";
import HttpUtils from "../../utils/api/httpUtils";
import IconButton from "@material-ui/core/IconButton";
import {Link} from "react-router-dom";
import MUIDataTable from "mui-datatables";
import React from "react";
import StateHolder from "../common/state/stateHolder";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import Timeline from "@material-ui/icons/Timeline";
import Tooltip from "@material-ui/core/Tooltip";
import Typography from "@material-ui/core/Typography";
import withGlobalState from "../common/state";
import {withStyles} from "@material-ui/core/styles";
import {
    ChartLabel, Hint, HorizontalBarSeries, HorizontalGridLines, VerticalGridLines, XAxis, XYPlot, YAxis
} from "react-vis";
import withColor, {ColorGenerator} from "../common/color";
import * as PropTypes from "prop-types";

const styles = () => ({
    drawerContent: {
        padding: 20
    },
    sideBarContentTitle: {
        fontSize: 14,
        fontWeight: 500,
        display: "inline-flex",
        paddingLeft: 10
    },
    titleIcon: {
        verticalAlign: "middle"
    },
    sidebarTableCell: {
        padding: 10
    },
    avatar: {
        width: 25,
        height: 25,
        fontSize: 10,
        fontWeight: 600,
        color: "#fff"
    },
    sidebarContainer: {
        marginBottom: 30
    },
    expansionSum: {
        padding: 0,
        borderBottomWidth: 1,
        borderBottomStyle: "solid",
        borderBottomColor: Grey[300]
    },
    cellIcon: {
        verticalAlign: "middle"
    },
    panel: {
        marginTop: 15,
        boxShadow: "none",
        borderTopWidth: 1,
        borderTopStyle: "solid",
        borderTopColor: Grey[200]
    },
    secondaryHeading: {
        paddingRight: 10
    },
    panelDetails: {
        padding: 0,
        marginBottom: 100
    },
    sidebarListTableText: {
        fontSize: 12

    },
    cellNameContainer: {
        marginTop: 10,
        marginBottom: 25
    },
    cellName: {
        display: "inline-flex",
        paddingLeft: 10
    },
    barChart: {
        marginTop: 20
    },
    titleDivider: {
        height: 1,
        border: "none",
        flexShrink: 0,
        backgroundColor: "#d1d1d1"
    }
});

class SidePanelContent extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            trafficGraphTooltip: false,
            error: null
        };
    }

    calculateTotals = (metrics) => {
        const totalMetrics = {
            totalIncomingRequests: 0,
            responseCounts: {
                "0xx": 0,
                "2xx": 0,
                "3xx": 0,
                "4xx": 0,
                "5xx": 0
            }
        };
        Object.values(metrics).forEach((metricsDatum) => {
            totalMetrics.totalIncomingRequests += metricsDatum.totalIncomingRequests;
            Object.keys(totalMetrics.responseCounts).forEach((key) => {
                totalMetrics.responseCounts[key] += metricsDatum.responseCounts[key];
            });
        });
        return totalMetrics;
    };

    calculateNodeSummary = (metrics) => {
        const {globalState} = this.props;
        const nodeSummary = {
            [Constants.Status.Success]: 0,
            [Constants.Status.Warning]: 0,
            [Constants.Status.Error]: 0,
            [Constants.Status.Unknown]: 0
        };
        Object.keys(metrics).forEach((nodeName) => {
            if (metrics[nodeName].totalIncomingRequests > 0) {
                const totalSuccessRequests = metrics[nodeName].responseCounts["2xx"]
                    + metrics[nodeName].responseCounts["3xx"];
                const successPercentage = totalSuccessRequests / metrics[nodeName].totalIncomingRequests;

                let status = Constants.Status.Success;
                if (successPercentage < globalState.get(StateHolder.CONFIG).percentageRangeMinValue.warningThreshold) {
                    status = Constants.Status.Warning;
                }
                if (successPercentage < globalState.get(StateHolder.CONFIG).percentageRangeMinValue.errorThreshold) {
                    status = Constants.Status.Error;
                }
                if (successPercentage < 0 || successPercentage > 1) {
                    status = Constants.Status.Unknown;
                }
                nodeSummary[status] += 1;
            } else {
                nodeSummary[Constants.Status.Unknown] += 1;
            }
        });
        return nodeSummary;
    };

    render = () => {
        const {classes, selectedInstance, selectedInstanceKind, colorGenerator, metrics} = this.props;
        const {trafficGraphTooltip} = this.state;

        const columns = [
            {
                options: {
                    customBodyRender: (value) => <HealthIndicator value={value}/>
                }
            },
            {
                options: {
                    customBodyRender: (datum) => {
                        const {cell, component} = datum;
                        return (
                            <Typography component={Link} className={classes.sidebarListTableText}
                                to={`/instances/${cell}${component ? `/components/${component}` : ""}`}>
                                {component ? component : cell}
                            </Typography>
                        );
                    }
                }
            },
            {
                options: {
                    customBodyRender: (datum) => (
                        <Tooltip title="View Traces">
                            <IconButton size="small" color="inherit" component={Link}
                                to={`/tracing/search${HttpUtils.generateQueryParamString(datum)}`}>
                                <Timeline/>
                            </IconButton>
                        </Tooltip>
                    )
                }
            }
        ];

        const unknownColor = colorGenerator.getColor(ColorGenerator.UNKNOWN);
        const successColor = colorGenerator.getColor(ColorGenerator.SUCCESS);
        const redirectionColor = colorGenerator.getColor(ColorGenerator.REDIRECTION);
        const warningColor = colorGenerator.getColor(ColorGenerator.WARNING);
        const errorColor = colorGenerator.getColor(ColorGenerator.ERROR);

        const totalMetrics = this.calculateTotals(metrics);
        const nodeSummary = this.calculateNodeSummary(metrics);

        const generateHorizontalBarSeries = (title, statusCodeKey, color) => (
            totalMetrics.responseCounts[statusCodeKey]
                ? (
                    <HorizontalBarSeries color={color}
                        data={[{
                            y: "Total",
                            x: (totalMetrics.responseCounts[statusCodeKey] / totalMetrics.totalIncomingRequests) * 100,
                            percentage: ((totalMetrics.responseCounts[statusCodeKey]
                                / totalMetrics.totalIncomingRequests) * 100).toFixed(2),
                            count: totalMetrics.responseCounts[statusCodeKey],
                            title: title
                        }]}
                        onValueMouseOver={(v) => this.setState({trafficGraphTooltip: v})}
                        onSeriesMouseOut={() => this.setState({trafficGraphTooltip: false})}
                    />
                )
                : null
        );
        const generateBadgeTableCell = (title, statusCodeKey, color) => (
            totalMetrics.responseCounts[statusCodeKey] > 0
                ? (
                    <TableCell className={classes.sidebarTableCell}>
                        <Avatar className={classes.avatar} style={{backgroundColor: color}}>
                            {title}
                        </Avatar>
                    </TableCell>
                )
                : null
        );
        const generateBadgeCountsTableCell = (statusCodeKey) => (
            totalMetrics.responseCounts[statusCodeKey] > 0
                ? (
                    <TableCell className={classes.sidebarTableCell}>
                        {((totalMetrics.responseCounts[statusCodeKey]
                            / totalMetrics.totalIncomingRequests) * 100).toFixed(0)}%
                    </TableCell>
                )
                : null
        );
        const generateExpansionPanelSummaryItem = (status, Icon, color) => (
            nodeSummary[status] > 0
                ? (
                    <Typography className={classes.secondaryHeading}>
                        <Icon className={classes.cellIcon} style={{color: color}}/>
                        &nbsp;{nodeSummary[status]}
                    </Typography>
                )
                : null
        );
        return (
            <div className={classes.drawerContent}>
                <div className={classes.sidebarContainer}>
                    {
                        selectedInstance
                            ? (
                                <div className={classes.cellNameContainer}>
                                    {
                                        selectedInstanceKind === Constants.InstanceKind.CELL
                                            ? <CellIcon className={classes.titleIcon} fontSize="small"/>
                                            : <CompositeIcon className={classes.titleIcon} fontSize="small"/>
                                    }

                                    <Typography color="inherit" className={classes.sideBarContentTitle}>
                                        {selectedInstanceKind}
                                    </Typography>
                                    <Typography component={Link} to={`/instances/${selectedInstance}`}
                                        className={classes.cellName}>
                                        {selectedInstance}
                                    </Typography>
                                </div>
                            )
                            : null
                    }
                    <HttpTrafficIcon className={classes.titleIcon} fontSize="small"/>
                    <Typography color="inherit" className={classes.sideBarContentTitle}>HTTP Traffic</Typography>
                    <hr className={classes.titleDivider}/>
                    <Table className={classes.table}>
                        <TableHead>
                            <TableRow>
                                <TableCell className={classes.sidebarTableCell}>Requests/s</TableCell>
                                {generateBadgeTableCell("OK", "2xx", successColor)}
                                {generateBadgeTableCell("3xx", "3xx", redirectionColor)}
                                {generateBadgeTableCell("4xx", "4xx", warningColor)}
                                {generateBadgeTableCell("5xx", "5xx", errorColor)}
                                {generateBadgeTableCell("xxx", "0xx", unknownColor)}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            <TableRow>
                                <TableCell className={classes.sidebarTableCell}>
                                    {totalMetrics.totalIncomingRequests}
                                </TableCell>
                                {generateBadgeCountsTableCell("2xx")}
                                {generateBadgeCountsTableCell("3xx")}
                                {generateBadgeCountsTableCell("4xx")}
                                {generateBadgeCountsTableCell("5xx")}
                                {generateBadgeCountsTableCell("0xx")}
                            </TableRow>
                        </TableBody>
                    </Table>
                    <div className={classes.barChart}>
                        <XYPlot yType="ordinal" stackBy="x" width={250} height={90}>
                            <VerticalGridLines/>
                            <HorizontalGridLines/>
                            <XAxis />
                            <YAxis />
                            <ChartLabel text="%" className="alt-x-label" includeMargin={false} xPercent={-0.15}
                                yPercent={1.8}/>
                            {generateHorizontalBarSeries("OK", "2xx", successColor)}
                            {generateHorizontalBarSeries("3xx", "3xx", redirectionColor)}
                            {generateHorizontalBarSeries("4xx", "4xx", warningColor)}
                            {generateHorizontalBarSeries("5xx", "5xx", errorColor)}
                            {generateHorizontalBarSeries("0xx", "xxx", unknownColor)}
                            {
                                trafficGraphTooltip
                                    ? (
                                        <Hint value={trafficGraphTooltip}>
                                            <div className="rv-hint__content">
                                                {`${trafficGraphTooltip.title}:
                                                ${trafficGraphTooltip.percentage}% (${trafficGraphTooltip.count})`}
                                            </div>
                                        </Hint>
                                    )
                                    : null
                            }
                        </XYPlot>
                    </div>
                </div>
                <div className={classes.sidebarContainer}>
                    {
                        selectedInstance
                            ? <ComponentIcon className={classes.titleIcon} fontSize="small"/>
                            : <CellsIcon className={classes.titleIcon} fontSize="small"/>
                    }
                    <Typography color="inherit" className={classes.sideBarContentTitle}>
                        {selectedInstance ? "Components" : "Instances"} ({Object.keys(metrics).length})
                    </Typography>
                    <ExpansionPanel className={classes.panel}>
                        <ExpansionPanelSummary expandIcon={<ExpandMoreIcon/>} className={classes.expansionSum}>
                            {generateExpansionPanelSummaryItem(Constants.Status.Success, CheckCircleOutline,
                                successColor)}
                            {generateExpansionPanelSummaryItem(Constants.Status.Warning, ErrorIcon,
                                warningColor)}
                            {generateExpansionPanelSummaryItem(Constants.Status.Error, ErrorIcon,
                                errorColor)}
                            {generateExpansionPanelSummaryItem(Constants.Status.Unknown, HelpOutlineIcon,
                                unknownColor)}
                        </ExpansionPanelSummary>
                        <ExpansionPanelDetails className={classes.panelDetails}>
                            <div className="overviewSidebarListTable">
                                <MUIDataTable columns={columns}
                                    options={{
                                        download: false,
                                        selectableRows: false,
                                        print: false,
                                        filter: false,
                                        search: false,
                                        viewColumns: false,
                                        rowHover: false
                                    }}
                                    data={Object.keys(metrics).map((nodeName) => [
                                        (metrics[nodeName].totalIncomingRequests > 0
                                            ? ((metrics[nodeName].responseCounts["2xx"]
                                                + metrics[nodeName].responseCounts["3xx"])
                                                    / metrics[nodeName].totalIncomingRequests)
                                            : -1),
                                        {
                                            cell: selectedInstance ? selectedInstance : nodeName,
                                            component: selectedInstance ? nodeName : null

                                        },
                                        {
                                            cell: selectedInstance ? selectedInstance : nodeName,
                                            component: selectedInstance ? nodeName : null
                                        }
                                    ])}/>
                            </div>
                        </ExpansionPanelDetails>
                    </ExpansionPanel>
                </div>
            </div>
        );
    }

}

SidePanelContent.propTypes = {
    classes: PropTypes.object.isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired,
    colorGenerator: PropTypes.instanceOf(ColorGenerator).isRequired,
    selectedInstance: PropTypes.string,
    selectedInstanceKind: PropTypes.string,
    metrics: PropTypes.objectOf(PropTypes.shape({
        totalIncomingRequests: PropTypes.number.isRequired,
        responseCounts: PropTypes.shape({
            "0xx": PropTypes.number.isRequired,
            "2xx": PropTypes.number.isRequired,
            "3xx": PropTypes.number.isRequired,
            "4xx": PropTypes.number.isRequired,
            "5xx": PropTypes.number.isRequired
        })
    }))
};

export default withStyles(styles, {withTheme: true})(withGlobalState(withColor(SidePanelContent)));
