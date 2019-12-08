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
import io.cellery.observability.api.exception.UnexpectedException;
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
 * MSF4J service for fetching instances related data.
 */
@Path("/api/runtimes/{runtime}/namespaces/{namespace}/instances")
public class InstanceAPI {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstancesList(@PathParam("namespace") String namespace,
                                     @DefaultValue("-1") @QueryParam("queryStartTime") long queryStartTime,
                                     @DefaultValue("-1") @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.K8S_GET_INSTANCES.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, "")
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred while fetching Tracing metadata", e);
        }
    }

    @GET
    @Path("/{instanceName}/components")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstanceComponents(@PathParam("namespace") String namespace,
                                          @PathParam("instanceName") String instanceName,
                                          @DefaultValue("-1") @QueryParam("queryStartTime") long queryStartTime,
                                          @DefaultValue("-1") @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        try {
            Object[][] results = SiddhiStoreQueryTemplates.K8S_GET_COMPONENTS.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, instanceName)
                    .setArg(SiddhiStoreQueryTemplates.Params.COMPONENT, "")
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();
            return Response.ok().entity(results).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred while fetching Tracing metadata", e);
        }
    }

    @GET
    @Path("/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstance(@PathParam("namespace") String namespace,
                                @PathParam("instanceName") String instanceName,
                                @DefaultValue("-1") @QueryParam("queryStartTime") long queryStartTime,
                                @DefaultValue("-1") @QueryParam("queryEndTime") long queryEndTime)
            throws APIInvocationException {
        try {
            JsonObject instanceInfo = new JsonObject();
            Object[][] results = SiddhiStoreQueryTemplates.K8S_GET_INSTANCES.builder()
                    .setArg(SiddhiStoreQueryTemplates.Params.NAMESPACE, namespace)
                    .setArg(SiddhiStoreQueryTemplates.Params.INSTANCE, instanceName)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_START_TIME, queryStartTime)
                    .setArg(SiddhiStoreQueryTemplates.Params.QUERY_END_TIME, queryEndTime)
                    .build()
                    .execute();
            Response response;
            if (results.length == 1) {
                instanceInfo.addProperty("instance", (String) results[0][0]);
                instanceInfo.addProperty("instanceKind", (String) results[0][1]);
                response = Response.ok().entity(instanceInfo).build();
            } else if (results.length == 0) {
                response = Response.status(Response.Status.NOT_FOUND).build();
            } else {
                throw new UnexpectedException("Found more than one instance for instance " + instanceName);
            }
            return response;
        } catch (Throwable e) {
            if (e instanceof UnexpectedException) {
                throw e;
            } else {
                throw new APIInvocationException("API Invocation error occurred while fetching Instance information",
                        e);
            }
        }
    }

    @OPTIONS
    public Response getRootOptions() {
        return Response.ok().build();
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }

}
