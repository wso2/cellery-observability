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

import DataTable from "../common/DataTable";
import HealthIndicator from "../common/HealthIndicator";
import HttpUtils from "../../utils/api/httpUtils";
import {Link} from "react-router-dom";
import NotFound from "../common/error/NotFound";
import NotificationUtils from "../../utils/common/notificationUtils";
import Paper from "@material-ui/core/Paper";
import React from "react";
import StateHolder from "../common/state/stateHolder";
import TopToolbar from "../common/toptoolbar";
import withGlobalState from "../common/state";
import {withStyles} from "@material-ui/core/styles";
import * as PropTypes from "prop-types";

const styles = (theme) => ({
    root: {
        margin: theme.spacing.unit,
        padding: theme.spacing.unit * 3
    }
});

class CellList extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            instanceInfo: [],
            isLoading: false
        };
    }

    loadCellInfo = (isUserAction, queryStartTime, queryEndTime) => {
        const {globalState} = this.props;
        const self = this;

        const search = {
            queryStartTime: queryStartTime.valueOf(),
            queryEndTime: queryEndTime.valueOf()
        };

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Instance Info", globalState);
            self.setState({
                isLoading: true
            });
        }
        HttpUtils.callObservabilityAPI(
            {
                url: `/http-requests/instances${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            globalState
        ).then((data) => {
            const instanceInfo = data.map((dataItem) => ({
                sourceInstance: dataItem[0],
                sourceInstanceKind: dataItem[1],
                destinationInstance: dataItem[2],
                destinationInstanceKind: dataItem[3],
                httpResponseGroup: dataItem[4],
                totalResponseTimeMilliSec: dataItem[5],
                requestCount: dataItem[6]
            }));

            self.setState({
                instanceInfo: instanceInfo
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState({
                    isLoading: false
                });
            }
        }).catch(() => {
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState({
                    isLoading: false
                });
                NotificationUtils.showNotification(
                    "Failed to load instance information",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    render = () => {
        const {classes, match} = this.props;
        const {instanceInfo, isLoading} = this.state;
        const columns = [
            {
                name: "Health",
                options: {
                    filter: false,
                    customBodyRender: (value) => <HealthIndicator value={value}/>
                }
            },
            {
                name: "Instance",
                options: {
                    filter: true,
                    customBodyRender: (value) => <Link to={`${match.url}/${value}`}>{value}</Link>
                }
            },
            {
                name: "Kind",
                options: {
                    filter: true
                }
            },
            {
                name: "Inbound Error Rate",
                options: {
                    filter: false,
                    headerNoWrap: false,
                    customBodyRender: (value) => `${Math.round(value * 100)} %`
                }
            },
            {
                name: "Outbound Error Rate",
                options: {
                    filter: false,
                    headerNoWrap: true,
                    customBodyRender: (value) => `${Math.round(value * 100)} %`
                }
            },
            {
                name: "Average Response Time (ms)",
                options: {
                    filter: false,
                    customBodyRender: (value) => (Math.round(value))
                }
            },
            {
                name: "Average Inbound Request Count (requests/s)",
                options: {
                    filter: false
                }
            }
        ];
        const options = {
            filter: true,
            responsive: "stacked"
        };

        // Processing data to find the required values
        const dataTableMap = {};
        const initializeDataTableMapEntryIfNotPresent = (instance, instanceKind) => {
            if (!dataTableMap[instance]) {
                dataTableMap[instance] = {
                    instanceKind: instanceKind,
                    inboundErrorCount: 0,
                    outboundErrorCount: 0,
                    requestCount: 0,
                    totalResponseTimeMilliSec: 0
                };
            }
        };
        for (const instanceDatum of instanceInfo) {
            initializeDataTableMapEntryIfNotPresent(instanceDatum.sourceInstance, instanceDatum.sourceInstanceKind);
            initializeDataTableMapEntryIfNotPresent(instanceDatum.destinationInstance,
                instanceDatum.destinationInstanceKind);

            if (instanceDatum.httpResponseGroup === "5xx") {
                dataTableMap[instanceDatum.destinationInstance].inboundErrorCount += instanceDatum.requestCount;
                dataTableMap[instanceDatum.sourceInstance].outboundErrorCount += instanceDatum.requestCount;
            }
            dataTableMap[instanceDatum.destinationInstance].requestCount += instanceDatum.requestCount;
            dataTableMap[instanceDatum.destinationInstance].totalResponseTimeMilliSec
                += instanceDatum.totalResponseTimeMilliSec;
        }

        // Transforming the objects into 2D array accepted by the Table library
        const tableData = [];
        for (const instance in dataTableMap) {
            if (dataTableMap.hasOwnProperty(instance) && Boolean(instance)) {
                const instanceData = dataTableMap[instance];
                tableData.push([
                    instanceData.requestCount === 0
                        ? -1
                        : 1 - instanceData.inboundErrorCount / instanceData.requestCount,
                    instance,
                    instanceData.instanceKind,
                    instanceData.requestCount === 0 ? 0 : instanceData.inboundErrorCount / instanceData.requestCount,
                    instanceData.requestCount === 0 ? 0 : instanceData.outboundErrorCount / instanceData.requestCount,
                    instanceData.requestCount === 0
                        ? 0
                        : instanceData.totalResponseTimeMilliSec / instanceData.requestCount,
                    instanceData.requestCount
                ]);
            }
        }

        return (
            <React.Fragment>
                <TopToolbar title={"Instances"} onUpdate={this.loadCellInfo}/>
                {
                    isLoading
                        ? null
                        : (
                            <Paper className={classes.root}>
                                {
                                    tableData.length > 0
                                        ? <DataTable columns={columns} options={options} data={tableData}/>
                                        : (
                                            <NotFound title={"No Instances Found"}
                                                description={"No Requests between instances found in the selected "
                                                    + "time range"}/>
                                        )
                                }
                            </Paper>
                        )
                }
            </React.Fragment>
        );
    };

}

CellList.propTypes = {
    classes: PropTypes.object.isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired,
    match: PropTypes.shape({
        url: PropTypes.string.isRequired
    }).isRequired
};

export default withStyles(styles)(withGlobalState(CellList));
