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

import Constants from "../../../utils/constants";
import FormControl from "@material-ui/core/FormControl";
import HttpUtils from "../../../utils/api/httpUtils";
import InputLabel from "@material-ui/core/InputLabel";
import Logger from "js-logger";
import MetricsGraphs from "../metricsGraphs";
import NotFound from "../../common/error/NotFound";
import NotificationUtils from "../../../utils/common/notificationUtils";
import QueryUtils from "../../../utils/common/queryUtils";
import React from "react";
import Select from "@material-ui/core/Select";
import StateHolder from "../../common/state/stateHolder";
import withGlobalState from "../../common/state/index";
import {withStyles} from "@material-ui/core/styles";
import * as PropTypes from "prop-types";

const styles = (theme) => ({
    filters: {
        marginTop: theme.spacing.unit * 4,
        marginBottom: theme.spacing.unit * 4
    },
    formControl: {
        marginRight: theme.spacing.unit * 4,
        minWidth: 150
    },
    graphs: {
        marginBottom: theme.spacing.unit * 4
    },
    button: {
        marginTop: theme.spacing.unit * 2
    }
});

class Metrics extends React.Component {

    static logger = Logger.get("components/instances/instance/Metrics");

    constructor(props) {
        super(props);

        this.state = {
            selectedType: props.globalFilterOverrides && props.globalFilterOverrides.selectedType
                ? props.globalFilterOverrides.selectedType
                : Constants.Dashboard.INBOUND,
            selectedInstance: props.globalFilterOverrides && props.globalFilterOverrides.selectedInstance
                ? props.globalFilterOverrides.selectedInstance
                : Constants.Dashboard.ALL_VALUE,
            instances: [],
            instanceData: [],
            loadingCount: 0
        };
    }

    componentDidMount = () => {
        const {globalState} = this.props;

        this.update(
            true,
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).startTime),
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).endTime)
        );
    };

    update = (isUserAction, startTime, endTime, selectedTypeOverride, selectedInstanceOverride) => {
        const {selectedType, selectedInstance} = this.state;
        const queryStartTime = startTime.valueOf();
        const queryEndTime = endTime.valueOf();

        this.loadMetrics(
            isUserAction, queryStartTime, queryEndTime,
            selectedTypeOverride ? selectedTypeOverride : selectedType,
            selectedInstanceOverride ? selectedInstanceOverride : selectedInstance
        );
        this.loadInstanceMetadata(isUserAction, queryStartTime, queryEndTime);
    };

    getFilterChangeHandler = (name) => (event) => {
        const {globalState, onFilterUpdate} = this.props;
        const {selectedType, selectedInstance} = this.state;

        const newValue = event.target.value;
        this.setState({
            [name]: newValue
        });

        if (onFilterUpdate) {
            onFilterUpdate({
                selectedType: selectedType,
                selectedInstance: selectedInstance,
                [name]: newValue
            });
        }

        this.update(
            true,
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).startTime),
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).endTime),
            name === "selectedType" ? newValue : null,
            name === "selectedInstance" ? newValue : null,
        );
    };

    loadInstanceMetadata = (isUserAction, queryStartTime, queryEndTime) => {
        const {globalState, instance} = this.props;
        const self = this;

        const search = {
            queryStartTime: queryStartTime,
            queryEndTime: queryEndTime
        };

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Instance Metadata", globalState);
            self.setState((prevState) => ({
                loadingCount: prevState.loadingCount + 1
            }));
        }
        const globalFilter = globalState.get(StateHolder.GLOBAL_FILTER);
        const pathPrefix = `/runtimes/${globalFilter.runtime}/namespaces/${globalFilter.namespace}`;
        HttpUtils.callObservabilityAPI(
            {
                url: `${pathPrefix}/http-requests/instances/metadata${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            globalState
        ).then((data) => {
            self.setState({
                instances: data.filter((datum) => Boolean(datum) && datum !== instance)
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
            }
        }).catch((error) => {
            Metrics.logger.error("Failed to load instance HTTP request metadata", error);
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
                NotificationUtils.showNotification(
                    "Failed to load Instance Metadata",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    loadMetrics = (isUserAction, queryStartTime, queryEndTime, selectedType, selectedInstance) => {
        const {globalState, instance} = this.props;
        const self = this;

        // Creating the search params
        const search = {
            queryStartTime: queryStartTime,
            queryEndTime: queryEndTime,
            includeIntraInstance: false
        };
        if (selectedInstance !== Constants.Dashboard.ALL_VALUE) {
            if (selectedType === Constants.Dashboard.INBOUND) {
                search.sourceInstance = selectedInstance;
            } else {
                search.destinationInstance = selectedInstance;
            }
        }
        if (selectedType === Constants.Dashboard.INBOUND) {
            search.destinationInstance = instance;
        } else {
            search.sourceInstance = instance;
        }

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Instance Metrics", globalState);
            self.setState((prevState) => ({
                loadingCount: prevState.loadingCount + 1
            }));
        }
        const globalFilter = globalState.get(StateHolder.GLOBAL_FILTER);
        const pathPrefix = `/runtimes/${globalFilter.runtime}/namespaces/${globalFilter.namespace}`;
        HttpUtils.callObservabilityAPI(
            {
                url: `${pathPrefix}/http-requests/instances/metrics${HttpUtils.generateQueryParamString(search)}`,
                method: "GET"
            },
            globalState
        ).then((data) => {
            const instanceData = data.map((datum) => ({
                timestamp: datum[0],
                httpResponseGroup: datum[1],
                totalResponseTimeMilliSec: datum[2],
                totalRequestSizeBytes: datum[3],
                totalResponseSizeBytes: datum[4],
                requestCount: datum[5]
            }));

            self.setState({
                instanceData: instanceData
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
            }
        }).catch((error) => {
            Metrics.logger.error("Failed to load instance HTTP request metrics", error);
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
                NotificationUtils.showNotification(
                    "Failed to load Instance Metrics",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    render = () => {
        const {classes, instance} = this.props;
        const {selectedType, selectedInstance, instances, instanceData, loadingCount} = this.state;

        const targetSourcePrefix = selectedType === Constants.Dashboard.INBOUND ? "Source" : "Target";

        return (
            loadingCount > 0
                ? null
                : (
                    <React.Fragment>
                        <div className={classes.filters}>
                            <FormControl className={classes.formControl}>
                                <InputLabel htmlFor="selected-type">Type</InputLabel>
                                <Select value={selectedType}
                                    onChange={this.getFilterChangeHandler("selectedType")}
                                    inputProps={{
                                        name: "selected-type",
                                        id: "selected-type"
                                    }}>
                                    <option value={Constants.Dashboard.INBOUND}>Inbound</option>
                                    <option value={Constants.Dashboard.OUTBOUND}>Outbound</option>
                                </Select>
                            </FormControl>
                            <FormControl className={classes.formControl}>
                                <InputLabel htmlFor="selected-instance">{targetSourcePrefix} Instance</InputLabel>
                                <Select value={selectedInstance}
                                    onChange={this.getFilterChangeHandler("selectedInstance")}
                                    inputProps={{
                                        name: "selected-instance",
                                        id: "selected-instance"
                                    }}>
                                    <option value={Constants.Dashboard.ALL_VALUE}>
                                        {Constants.Dashboard.ALL_VALUE}
                                    </option>
                                    {
                                        instances.map((instance) => (
                                            <option key={instance} value={instance}>{instance}</option>))
                                    }
                                </Select>
                            </FormControl>
                        </div>
                        <div className={classes.graphs}>
                            {
                                instanceData.length > 0
                                    ? (
                                        <MetricsGraphs instance={instance} data={instanceData}
                                            direction={selectedType === Constants.Dashboard.INBOUND ? "In" : "Out"}/>
                                    )
                                    : (
                                        <NotFound title={"No Metrics Found"}
                                            description={
                                                selectedType === Constants.Dashboard.INBOUND
                                                    ? `No Requests from the selected instance to "${instance}" instance`
                                                    : `No Requests from "${instance}" instance to the selected instance`
                                            }/>
                                    )
                            }
                        </div>
                    </React.Fragment>
                )
        );
    };

}

Metrics.propTypes = {
    classes: PropTypes.object.isRequired,
    globalState: PropTypes.instanceOf(StateHolder).isRequired,
    instance: PropTypes.string.isRequired,
    onFilterUpdate: PropTypes.func.isRequired,
    globalFilterOverrides: PropTypes.shape({
        selectedType: PropTypes.string,
        selectedInstance: PropTypes.string
    })
};

export default withStyles(styles)(withGlobalState(Metrics));
