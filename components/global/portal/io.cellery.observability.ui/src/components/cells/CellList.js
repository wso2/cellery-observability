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
        const globalFilter = globalState.get(StateHolder.GLOBAL_FILTER);
        const pathPrefix = `/runtimes/${globalFilter.runtime}/namespaces/${globalFilter.namespace}`;
        HttpUtils.callObservabilityAPI(
            {
                url: `${pathPrefix}/http-requests/instances${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            globalState
        ).then((data) => {
            const instanceInfo = data.map((dataItem) => ({
                sourceNamespace: dataItem[0],
                sourceInstance: dataItem[1],
                sourceInstanceKind: dataItem[2],
                destinationNamespace: dataItem[3],
                destinationInstance: dataItem[4],
                destinationInstanceKind: dataItem[5],
                httpResponseGroup: dataItem[6],
                totalResponseTimeMilliSec: dataItem[7],
                requestCount: dataItem[8]
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
        const {classes, match, globalState} = this.props;
        const {instanceInfo, isLoading} = this.state;
        const selectedNamespace = globalState.get(StateHolder.GLOBAL_FILTER).namespace;
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
                name: "Total Inbound Request Count",
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
                    inboundRequestCount: 0,
                    totalResponseTimeMilliSec: 0,
                    outboundErrorCount: 0,
                    outboundRequestCount: 0
                };
            }
        };
        const isInstanceRelevant = (namespace) => namespace === selectedNamespace;
        for (const instanceDatum of instanceInfo) {
            if (isInstanceRelevant(instanceDatum.sourceNamespace)) {
                initializeDataTableMapEntryIfNotPresent(instanceDatum.sourceInstance, instanceDatum.sourceInstanceKind);
                if (instanceDatum.httpResponseGroup === "5xx") {
                    dataTableMap[instanceDatum.sourceInstance].outboundErrorCount += instanceDatum.requestCount;
                }
                dataTableMap[instanceDatum.sourceInstance].outboundRequestCount += instanceDatum.requestCount;
            }
            if (isInstanceRelevant(instanceDatum.destinationNamespace)) {
                initializeDataTableMapEntryIfNotPresent(instanceDatum.destinationInstance,
                    instanceDatum.destinationInstanceKind);
                if (instanceDatum.httpResponseGroup === "5xx") {
                    dataTableMap[instanceDatum.destinationInstance].inboundErrorCount += instanceDatum.requestCount;
                }
                dataTableMap[instanceDatum.destinationInstance].inboundRequestCount += instanceDatum.requestCount;
                dataTableMap[instanceDatum.destinationInstance].totalResponseTimeMilliSec
                    += instanceDatum.totalResponseTimeMilliSec;
            }
        }

        // Transforming the objects into 2D array accepted by the Table library
        const tableData = [];
        for (const instance in dataTableMap) {
            if (dataTableMap.hasOwnProperty(instance) && Boolean(instance)) {
                const instanceData = dataTableMap[instance];
                tableData.push([
                    instanceData.inboundRequestCount === 0
                        ? -1
                        : 1 - instanceData.inboundErrorCount / instanceData.inboundRequestCount,
                    instance,
                    instanceData.instanceKind,
                    instanceData.inboundRequestCount === 0
                        ? 0
                        : instanceData.inboundErrorCount / instanceData.inboundRequestCount,
                    instanceData.outboundRequestCount === 0
                        ? 0
                        : instanceData.outboundErrorCount / instanceData.outboundRequestCount,
                    instanceData.inboundRequestCount === 0
                        ? 0
                        : instanceData.totalResponseTimeMilliSec / instanceData.inboundRequestCount,
                    instanceData.inboundRequestCount
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
