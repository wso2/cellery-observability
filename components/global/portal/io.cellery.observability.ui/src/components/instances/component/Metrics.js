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
import withGlobalState from "../../common/state";
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

    static logger = Logger.get("components/instances/component/Metrics");

    constructor(props) {
        super(props);

        this.state = {
            selectedType: props.globalFilterOverrides && props.globalFilterOverrides.selectedType
                ? props.globalFilterOverrides.selectedType
                : Constants.Dashboard.INBOUND,
            selectedInstance: props.globalFilterOverrides && props.globalFilterOverrides.selectedInstance
                ? props.globalFilterOverrides.selectedInstance
                : Constants.Dashboard.ALL_VALUE,
            selectedComponent: props.globalFilterOverrides && props.globalFilterOverrides.selectedComponent
                ? props.globalFilterOverrides.selectedComponent
                : Constants.Dashboard.ALL_VALUE,
            components: [],
            metadata: {
                availableInstances: [],
                availableComponents: [] // Filtered based on the selected instance
            },
            componentData: [],
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

    update = (isUserAction, startTime, endTime, selectedTypeOverride, selectedInstanceOverride,
        selectedComponentOverride) => {
        const {selectedType, selectedInstance, selectedComponent} = this.state;
        const queryStartTime = startTime.valueOf();
        const queryEndTime = endTime.valueOf();

        this.loadMetrics(
            isUserAction, queryStartTime, queryEndTime,
            selectedTypeOverride ? selectedTypeOverride : selectedType,
            selectedInstanceOverride ? selectedInstanceOverride : selectedInstance,
            selectedComponentOverride ? selectedComponentOverride : selectedComponent
        );
        this.loadComponentMetadata(isUserAction, queryStartTime, queryEndTime);
    };

    getFilterChangeHandler = (name) => (event) => {
        const {globalState, onFilterUpdate} = this.props;
        const {selectedType, selectedInstance, selectedComponent} = this.state;

        const newValue = event.target.value;
        this.setState({
            [name]: newValue
        });

        if (onFilterUpdate) {
            onFilterUpdate({
                selectedType: selectedType,
                selectedInstance: selectedInstance,
                selectedComponent: selectedComponent,
                [name]: newValue
            });
        }

        this.update(
            true,
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).startTime),
            QueryUtils.parseTime(globalState.get(StateHolder.GLOBAL_FILTER).endTime),
            name === "selectedType" ? newValue : null,
            name === "selectedInstance" ? newValue : null,
            name === "selectedComponent" ? newValue : null
        );
    };

    loadComponentMetadata = (isUserAction, queryStartTime, queryEndTime) => {
        const {globalState, instance, component} = this.props;
        const self = this;

        const searchQueryParams = HttpUtils.generateQueryParamString({
            queryStartTime: queryStartTime,
            queryEndTime: queryEndTime
        });

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Component Info", globalState);
            self.setState((prevState) => ({
                loadingCount: prevState.loadingCount + 1
            }));
        }
        const globalFilter = globalState.get(StateHolder.GLOBAL_FILTER);
        const pathPrefix = `/runtimes/${globalFilter.runtime}/namespaces/${globalFilter.namespace}`;
        HttpUtils.callObservabilityAPI(
            {
                url: `${pathPrefix}/http-requests/instances/components/metadata${searchQueryParams}`,
                method: "GET"
            },
            globalState
        ).then((data) => {
            self.setState({
                components: data
                    .filter((datum) => (datum.instance !== instance || datum.component !== component))
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
            }
        }).catch((error) => {
            Metrics.logger.error("Failed to load component HTTP request metadata", error);
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
                NotificationUtils.showNotification(
                    "Failed to load component information",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    loadMetrics = (isUserAction, queryStartTime, queryEndTime, selectedType, selectedInstance, selectedComponent) => {
        const {globalState, instance, component} = this.props;
        const self = this;

        // Creating the search params
        const search = {
            queryStartTime: queryStartTime,
            queryEndTime: queryEndTime
        };
        if (selectedInstance !== Constants.Dashboard.ALL_VALUE) {
            if (selectedType === Constants.Dashboard.INBOUND) {
                search.sourceInstance = selectedInstance;
            } else {
                search.destinationInstance = selectedInstance;
            }
        }
        if (selectedComponent !== Constants.Dashboard.ALL_VALUE) {
            if (selectedType === Constants.Dashboard.INBOUND) {
                search.sourceComponent = selectedComponent;
            } else {
                search.destinationComponent = selectedComponent;
            }
        }
        if (selectedType === Constants.Dashboard.INBOUND) {
            search.destinationInstance = instance;
            search.destinationComponent = component;
        } else {
            search.sourceInstance = instance;
            search.sourceComponent = component;
        }
        const searchQueryParams = HttpUtils.generateQueryParamString(search);

        if (isUserAction) {
            NotificationUtils.showLoadingOverlay("Loading Component Metrics", globalState);
            self.setState((prevState) => ({
                loadingCount: prevState.loadingCount + 1
            }));
        }
        const globalFilter = globalState.get(StateHolder.GLOBAL_FILTER);
        const pathPrefix = `/runtimes/${globalFilter.runtime}/namespaces/${globalFilter.namespace}`;
        HttpUtils.callObservabilityAPI(
            {
                url: `${pathPrefix}/http-requests/instances/components/metrics${searchQueryParams}`,
                method: "GET"
            },
            globalState
        ).then((data) => {
            const componentData = data.map((datum) => ({
                timestamp: datum[0],
                httpResponseGroup: datum[1],
                totalResponseTimeMilliSec: datum[2],
                totalRequestSizeBytes: datum[3],
                totalResponseSizeBytes: datum[4],
                requestCount: datum[5]
            }));

            self.setState({
                componentData: componentData
            });
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
            }
        }).catch((error) => {
            Metrics.logger.error("Failed to load component HTTP request metrics", error);
            if (isUserAction) {
                NotificationUtils.hideLoadingOverlay(globalState);
                self.setState((prevState) => ({
                    loadingCount: prevState.loadingCount - 1
                }));
                NotificationUtils.showNotification(
                    "Failed to load component metrics",
                    NotificationUtils.Levels.ERROR,
                    globalState
                );
            }
        });
    };

    static getDerivedStateFromProps = (props, state) => {
        const {instance, component} = props;
        const {components, selectedInstance, selectedComponent} = state;

        const availableInstances = [];
        components.forEach((componentDatum) => {
            // Validating whether at-least one relevant component in this instance exists
            let hasRelevantComponent = true;
            if (Boolean(componentDatum.instance) && componentDatum.instance === instance) {
                const relevantComponents = components.find(
                    (datum) => instance === datum.instance && datum.component !== component);
                hasRelevantComponent = Boolean(relevantComponents);
            }

            if (hasRelevantComponent && Boolean(componentDatum.instance)
                    && !availableInstances.includes(componentDatum.instance)) {
                availableInstances.push(componentDatum.instance);
            }
        });

        const availableComponents = [];
        components.forEach((componentDatum) => {
            if (Boolean(componentDatum.component) && Boolean(componentDatum.instance)
                && (selectedInstance === Constants.Dashboard.ALL_VALUE || componentDatum.instance === selectedInstance)
                && (componentDatum.instance !== instance || componentDatum.component !== component)
                && !availableComponents.includes(componentDatum.component)) {
                availableComponents.push(componentDatum.component);
            }
        });

        const selectedComponentToShow = components.length === 0 || availableComponents.includes(selectedComponent)
            ? selectedComponent
            : Constants.Dashboard.ALL_VALUE;

        return {
            ...state,
            selectedComponent: selectedComponentToShow,
            metadata: {
                availableInstances: availableInstances,
                availableComponents: availableComponents
            }
        };
    };

    render = () => {
        const {classes, instance, component} = this.props;
        const {selectedType, selectedInstance, selectedComponent, componentData, metadata, loadingCount} = this.state;

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
                                    <option value={Constants.Dashboard.INBOUND}>{Constants.Dashboard.INBOUND}</option>
                                    <option value={Constants.Dashboard.OUTBOUND}>{Constants.Dashboard.OUTBOUND}</option>
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
                                        metadata.availableInstances.map(
                                            (instance) => (<option key={instance} value={instance}>{instance}</option>))
                                    }
                                </Select>
                            </FormControl>
                            <FormControl className={classes.formControl}>
                                <InputLabel htmlFor="selected-component">
                                    {targetSourcePrefix} Component
                                </InputLabel>
                                <Select value={selectedComponent}
                                    onChange={this.getFilterChangeHandler("selectedComponent")}
                                    inputProps={{
                                        name: "selected-component",
                                        id: "selected-component"
                                    }}>
                                    <option value={Constants.Dashboard.ALL_VALUE}>
                                        {Constants.Dashboard.ALL_VALUE}
                                    </option>
                                    {
                                        metadata.availableComponents.map((component) => (
                                            <option key={component} value={component}>{component}</option>
                                        ))
                                    }
                                </Select>
                            </FormControl>
                        </div>
                        <div className={classes.graphs}>
                            {
                                componentData.length > 0
                                    ? (
                                        <MetricsGraphs instance={instance} component={component} data={componentData}
                                            direction={selectedType === Constants.Dashboard.INBOUND ? "In" : "Out"}/>
                                    )
                                    : (
                                        <NotFound title={"No Metrics Found"}
                                            description={
                                                selectedType === Constants.Dashboard.INBOUND
                                                    ? "No Requests from the selected component "
                                                        + `to the "${instance}" instance's "${component}" component`
                                                    : `No Requests from the "${instance}" instance's "${component}" `
                                                        + "component to the selected component"
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
    component: PropTypes.string.isRequired,
    onFilterUpdate: PropTypes.func.isRequired,
    globalFilterOverrides: PropTypes.shape({
        selectedType: PropTypes.string,
        selectedInstance: PropTypes.string,
        selectedComponent: PropTypes.string
    })
};

export default withStyles(styles)(withGlobalState(Metrics));
