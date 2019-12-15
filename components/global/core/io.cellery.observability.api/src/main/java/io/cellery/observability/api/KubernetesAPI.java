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
@Path("/api/runtimes/{runtime}/namespaces/{namespace}/k8s")
public class KubernetesAPI {

    @GET
    @Path("/pods")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getK8sPods(@PathParam("runtime") String runtime,
                               @PathParam("namespace") String namespace,
                               @DefaultValue("-1") @QueryParam("queryStartTime") long queryStartTime,
                               @DefaultValue("-1") @QueryParam("queryEndTime") long queryEndTime,
                               @DefaultValue("") @QueryParam("instance") String instance,
                               @DefaultValue("") @QueryParam("component") String component)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateCelleryIdParam("instance", instance);
        Utils.validateCelleryIdParam("component", component);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        try {
            Object[][] results = SiddhiStoreQueryTemplates.K8S_GET_PODS_FOR_COMPONENT.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, instance)
                    .setArg(SiddhiStoreQueryTemplates.Params.COMPONENT, component)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
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
    public Response getAllInstancesInfo(@PathParam("runtime") String runtime,
                                        @PathParam("namespace") String namespace,
                                        @DefaultValue("-1") @QueryParam("queryStartTime") long queryStartTime,
                                        @DefaultValue("-1") @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        return getInstancesAndComponentsInfo(runtime, namespace, "", "", queryStartTime, queryEndTime);
    }

    @GET
    @Path("/instances/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceInfo(@PathParam("runtime") String runtime,
                                    @PathParam("namespace") String namespace,
                                    @PathParam("instanceName") String instance,
                                    @DefaultValue("-1") @QueryParam("queryStartTime") long queryStartTime,
                                    @DefaultValue("-1") @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateCelleryIdParam("instance", instance);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        return getInstancesAndComponentsInfo(runtime, namespace, instance, "", queryStartTime, queryEndTime);
    }

    @GET
    @Path("/instances/{instanceName}/components/{componentName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComponentInfo(@PathParam("runtime") String runtime,
                                     @PathParam("namespace") String namespace,
                                     @PathParam("instanceName") String instance,
                                     @PathParam("componentName") String component,
                                     @DefaultValue("-1") @QueryParam("queryStartTime") long queryStartTime,
                                     @DefaultValue("-1") @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        Utils.validateCelleryIdParam("runtime", runtime);
        Utils.validateCelleryIdParam("namespace", namespace);
        Utils.validateCelleryIdParam("instance", instance);
        Utils.validateCelleryIdParam("component", component);
        Utils.validateQueryRangeParam(queryStartTime, queryEndTime);
        return getInstancesAndComponentsInfo(runtime, namespace, instance, component, queryStartTime, queryEndTime);
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }

    private Response getInstancesAndComponentsInfo(String runtime, String namespace, String instance, String component,
                                                   long queryStartTime, long queryEndTime)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.K8S_GET_COMPONENTS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.RUNTIME, runtime)
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, instance)
                    .setArg(SiddhiStoreQueryTemplates.Params.COMPONENT, component)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("API Invocation error occurred while fetching " +
                    "Kubernetes Component information", throwable);
        }
    }

}
