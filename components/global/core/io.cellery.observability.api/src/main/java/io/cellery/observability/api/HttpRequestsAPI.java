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

package io.cellery.observability.api;

import com.google.gson.JsonObject;
import io.cellery.observability.api.exception.APIInvocationException;
import io.cellery.observability.api.siddhi.SiddhiStoreQueryTemplates;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * MSF4J service for fetching the aggregated request.
 */
@Path("/api/runtimes/{runtime}/namespaces/{namespace}/http-requests")
public class HttpRequestsAPI {

    @GET
    @Path("/instances")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAggregatedRequestsForInstances(@PathParam("runtime") String runtime,
                                                      @PathParam("namespace") String namespace,
                                                      @QueryParam("queryStartTime") long queryStartTime,
                                                      @QueryParam("queryEndTime") long queryEndTime,
                                                      @DefaultValue("seconds") @QueryParam("timeGranularity")
                                                                  String timeGranularity)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        Utils.validateTimeGranularityParam(timeGranularity);
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCES.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("Unexpected error occurred while fetching the aggregated HTTP request " +
                    "data for instances", throwable);
        }
    }

    @GET
    @Path("/instances/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsForInstances(@PathParam("runtime") String runtime,
                                           @PathParam("namespace") String namespace,
                                           @QueryParam("queryStartTime") long queryStartTime,
                                           @QueryParam("queryEndTime") long queryEndTime,
                                           @DefaultValue("seconds") @QueryParam("timeGranularity")
                                                       String timeGranularity,
                                           @DefaultValue("") @QueryParam("sourceInstance") String sourceInstance,
                                           @DefaultValue("") @QueryParam("destinationInstance")
                                                       String destinationInstance,
                                           @DefaultValue("false") @QueryParam("includeIntraInstance")
                                                       boolean includeIntraInstance)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateCelleryIdParam("sourceInstance", sourceInstance);
        Utils.validateCelleryIdParam("destinationInstance", destinationInstance);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        Utils.validateTimeGranularityParam(timeGranularity);
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCES_METRICS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_NAMESPACE,
                            StringUtils.isEmpty(sourceInstance) ? "" : namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_INSTANCE, sourceInstance)
                    .setArg(SiddhiStoreQueryTemplates.Params.DESTINATION_NAMESPACE,
                            StringUtils.isEmpty(destinationInstance) ? "" : namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.DESTINATION_INSTANCE, destinationInstance)
                    .setArg(SiddhiStoreQueryTemplates.Params.CONDITION,
                            includeIntraInstance
                                    ? "true"
                                    : "sourceInstance != destinationInstance")
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("Unexpected error occurred while fetching aggregated HTTP Request metrics",
                    throwable);
        }
    }

    @GET
    @Path("/instances/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataForInstances(@PathParam("runtime") String runtime,
                                            @PathParam("namespace") String namespace,
                                            @QueryParam("queryStartTime") long queryStartTime,
                                            @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCES_METADATA.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();

            Set<String> instances = new HashSet<>();
            for (Object[] result : results) {
                if (Objects.equals(namespace, result[0])) {
                    instances.add((String) result[1]);
                }
                if (Objects.equals(namespace, result[2])) {
                    instances.add((String) result[3]);
                }
            }

            return Response.ok().entity(instances).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching Instance metadata",
                    throwable);
        }
    }

    @GET
    @Path("/instances/{instanceName}/components")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAggregatedRequestsForComponents(@PathParam("runtime") String runtime,
                                                       @PathParam("namespace") String namespace,
                                                       @PathParam("instanceName") String instanceName,
                                                       @QueryParam("queryStartTime") long queryStartTime,
                                                       @QueryParam("queryEndTime") long queryEndTime,
                                                       @DefaultValue("seconds") @QueryParam("timeGranularity")
                                                                   String timeGranularity)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateCelleryIdParam("instanceName", instanceName);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        Utils.validateTimeGranularityParam(timeGranularity);
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCE_COMPONENTS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, instanceName)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching the aggregated HTTP " +
                    "requests for components in instance " + instanceName, throwable);
        }
    }

    @GET
    @Path("/instances/components/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsForComponents(@PathParam("runtime") String runtime,
                                            @PathParam("namespace") String namespace,
                                            @QueryParam("queryStartTime") long queryStartTime,
                                            @QueryParam("queryEndTime") long queryEndTime,
                                            @DefaultValue("seconds") @QueryParam("timeGranularity")
                                                        String timeGranularity,
                                            @DefaultValue("") @QueryParam("sourceInstance") String sourceInstance,
                                            @DefaultValue("") @QueryParam("sourceComponent")
                                                        String sourceComponent,
                                            @DefaultValue("") @QueryParam("destinationInstance")
                                                        String destinationInstance,
                                            @DefaultValue("") @QueryParam("destinationComponent")
                                                        String destinationComponent)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateCelleryIdParam("sourceInstance", sourceInstance);
        Utils.validateCelleryIdParam("sourceComponent", sourceComponent);
        Utils.validateCelleryIdParam("destinationInstance", destinationInstance);
        Utils.validateCelleryIdParam("destinationComponent", destinationComponent);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        Utils.validateTimeGranularityParam(timeGranularity);
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METRICS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_NAMESPACE,
                            StringUtils.isEmpty(sourceInstance) ? "" : namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_INSTANCE, sourceInstance)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_COMPONENT, sourceComponent)
                    .setArg(SiddhiStoreQueryTemplates.Params.DESTINATION_NAMESPACE,
                            StringUtils.isEmpty(destinationInstance) ? "" : namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.DESTINATION_INSTANCE, destinationInstance)
                    .setArg(SiddhiStoreQueryTemplates.Params.DESTINATION_COMPONENT, destinationComponent)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching the aggregated Component " +
                    "metrics", throwable);
        }
    }

    @GET
    @Path("/instances/components/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataForComponents(@PathParam("runtime") String runtime,
                                             @PathParam("namespace") String namespace,
                                             @QueryParam("queryStartTime") long queryStartTime,
                                             @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METADATA.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();

            Set<JsonObject> components = new HashSet<>();
            for (Object[] result : results) {
                if (Objects.equals(namespace, result[0])) {
                    JsonObject component = new JsonObject();
                    component.addProperty("instance", (String) result[1]);
                    component.addProperty("component", (String) result[2]);
                    components.add(component);
                }
                if (Objects.equals(namespace, result[3])) {
                    JsonObject component = new JsonObject();
                    component.addProperty("instance", (String) result[4]);
                    component.addProperty("component", (String) result[5]);
                    components.add(component);
                }
            }

            return Response.ok().entity(components).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching the Components metadata",
                    throwable);
        }
    }

    @Path(".*")
    @OPTIONS
    public Response getOptions() {
        return Response.ok().build();
    }
}
