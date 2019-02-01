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

import java.util.HashSet;
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
@Path("/api/http-requests")
public class AggregatedRequestsAPI {

    @GET
    @Path("/cells")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAggregatedRequestsForCells(@QueryParam("queryStartTime") long queryStartTime,
                                                  @QueryParam("queryEndTime") long queryEndTime,
                                                  @DefaultValue("seconds") @QueryParam("timeGranularity")
                                                          String timeGranularity) throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELLS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("Unexpected error occurred while fetching the aggregated HTTP request " +
                    "data for cells", throwable);
        }
    }

    @GET
    @Path("/cells/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsForCells(@QueryParam("queryStartTime") long queryStartTime,
                                       @QueryParam("queryEndTime") long queryEndTime,
                                       @DefaultValue("") @QueryParam("sourceCell") String sourceCell,
                                       @DefaultValue("") @QueryParam("destinationCell") String destinationCell,
                                       @DefaultValue("seconds") @QueryParam("timeGranularity") String timeGranularity)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELLS_METRICS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_CELL, sourceCell)
                    .setArg(SiddhiStoreQueryTemplates.Params.DESTINATION_CELL, destinationCell)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("Unexpected error occurred while fetching aggregated HTTP Request metrics",
                    throwable);
        }
    }

    @GET
    @Path("/cells/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataForCells(@QueryParam("queryStartTime") long queryStartTime,
                                        @QueryParam("queryEndTime") long queryEndTime) throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELLS_METADATA.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();

            Set<String> cells = new HashSet<>();
            for (Object[] result : results) {
                cells.add((String) result[0]);
                cells.add((String) result[1]);
            }

            return Response.ok().entity(cells).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching Cell metadata", throwable);
        }
    }

    @GET
    @Path("/cells/{cellName}/components")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAggregatedRequestsForComponents(@PathParam("cellName") String cellName,
                                                       @QueryParam("queryStartTime") long queryStartTime,
                                                       @QueryParam("queryEndTime") long queryEndTime,
                                                       @DefaultValue("seconds")
                                                       @QueryParam("timeGranularity") String timeGranularity)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELL_COMPONENTS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .setArg(SiddhiStoreQueryTemplates.Params.CELL, cellName)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching the aggregated HTTP " +
                    "requests for components in cell " + cellName, throwable);
        }
    }

    @GET
    @Path("/cells/components/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetricsForComponents(@QueryParam("queryStartTime") long queryStartTime,
                                            @QueryParam("queryEndTime") long queryEndTime,
                                            @DefaultValue("") @QueryParam("sourceCell") String sourceCell,
                                            @DefaultValue("")
                                            @QueryParam("sourceComponent") String sourceComponent,
                                            @DefaultValue("") @QueryParam("destinationCell") String destinationCell,
                                            @DefaultValue("")
                                            @QueryParam("destinationComponent") String destinationComponent,
                                            @DefaultValue("seconds")
                                            @QueryParam("timeGranularity") String timeGranularity)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METRICS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.TIME_GRANULARITY, timeGranularity)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_CELL, sourceCell)
                    .setArg(SiddhiStoreQueryTemplates.Params.SOURCE_COMPONENT, sourceComponent)
                    .setArg(SiddhiStoreQueryTemplates.Params.DESTINATION_CELL, destinationCell)
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
    @Path("/cells/components/metadata")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadataForComponents(@QueryParam("queryStartTime") long queryStartTime,
                                                @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METADATA.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();

            Set<JsonObject> components = new HashSet<>();
            for (Object[] result : results) {
                for (int i = 0; i < 2; i++) {
                    JsonObject component = new JsonObject();
                    component.addProperty("cell", (String) result[i * 2]);
                    component.addProperty("name", (String) result[i * 2 + 1]);
                    components.add(component);
                }
            }

            return Response.ok().entity(components).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching the Components metadata",
                    throwable);
        }
    }

    @Path("/*")
    @OPTIONS
    public Response getOptions() {
        return Response.ok().build();
    }
}
