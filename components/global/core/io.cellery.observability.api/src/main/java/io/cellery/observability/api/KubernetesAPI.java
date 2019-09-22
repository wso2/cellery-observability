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

import io.cellery.observability.api.exception.APIInvocationException;
import io.cellery.observability.api.siddhi.SiddhiStoreQueryTemplates;

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
 * MSF4J service for fetching K8s level information.
 */
@Path("/api/k8s")
public class KubernetesAPI {

    @GET
    @Path("/pods")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getK8sPods(@QueryParam("queryStartTime") long queryStartTime,
                               @QueryParam("queryEndTime") long queryEndTime,
                               @DefaultValue("") @QueryParam("instance") String instance,
                               @DefaultValue("") @QueryParam("component") String component)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.K8S_GET_PODS_FOR_COMPONENT.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, instance)
                    .setArg(SiddhiStoreQueryTemplates.Params.COMPONENT, component)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching Kubernetes pod information",
                    throwable);
        }
    }

    @GET
    @Path("/instances")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllInstancesInfo(@QueryParam("queryStartTime") long queryStartTime,
                                        @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        return getInstancesAndComponentsInfo(queryStartTime, queryEndTime, "", "");
    }

    @GET
    @Path("/instances/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceInfo(@QueryParam("queryStartTime") long queryStartTime,
                                    @QueryParam("queryEndTime") long queryEndTime,
                                    @PathParam("instanceName") String instance)
            throws APIInvocationException {
        return getInstancesAndComponentsInfo(queryStartTime, queryEndTime, instance, "");
    }

    @GET
    @Path("/instances/{instanceName}/components/{componentName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComponentInfo(@QueryParam("queryStartTime") long queryStartTime,
                                     @QueryParam("queryEndTime") long queryEndTime,
                                     @PathParam("instanceName") String instance,
                                     @PathParam("componentName") String component)
            throws APIInvocationException {
        return getInstancesAndComponentsInfo(queryStartTime, queryEndTime, instance, component);
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }

    private Response getInstancesAndComponentsInfo(long queryStartTime, long queryEndTime, String instance,
                                                   String component)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.K8S_GET_COMPONENTS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, instance)
                    .setArg(SiddhiStoreQueryTemplates.Params.COMPONENT, component)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching " +
                    "Kubernetes Component information", throwable);
        }
    }

}
