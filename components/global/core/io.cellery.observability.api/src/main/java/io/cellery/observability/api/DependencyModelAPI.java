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
import io.cellery.observability.api.internal.ServiceHolder;
import io.cellery.observability.model.generator.model.Model;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * MSF4J service for fetching the dependency models.
 */
@Path("/api/dependency-model")
public class DependencyModelAPI {

    @GET
    @Path("/instances")
    @Produces("application/json")
    public Response getInstanceOverview(@DefaultValue("0") @QueryParam("queryStartTime") Long queryStartTime,
                                        @DefaultValue("0") @QueryParam("queryEndTime") Long queryEndTime)
            throws APIInvocationException {
        try {
            // TODO: Read runtime and namespace from API
            Model model = ServiceHolder.getModelManager().getNamespaceDependencyModel(queryStartTime, queryEndTime,
                    "local", "default");
            return Response.ok().entity(model).build();
        } catch (Throwable e) {
            throw new APIInvocationException("Unexpected error occurred while fetching the Instance dependency model",
                    e);
        }
    }

    @GET
    @Path("/instances/{instanceName}")
    @Produces("application/json")
    public Response getInstanceDependencyView(@PathParam("instanceName") String instanceName,
                                              @DefaultValue("0") @QueryParam("queryStartTime") Long queryStartTime,
                                              @DefaultValue("0") @QueryParam("queryEndTime") Long queryEndTime)
            throws APIInvocationException {
        try {
            // TODO: Read runtime and namespace from API
            Model model = ServiceHolder.getModelManager().getInstanceDependencyModel(queryStartTime, queryEndTime,
                    "local", "default", instanceName);
            return Response.ok().entity(model).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred while fetching the dependency model for " +
                    "instance :" + instanceName, e);
        }
    }

    @GET
    @Path("/instances/{instanceName}/components/{componentName}")
    @Produces("application/json")
    public Response getComponentDependencyView(@PathParam("instanceName") String instanceName,
                                               @PathParam("componentName") String componentName,
                                               @DefaultValue("0") @QueryParam("queryStartTime") Long queryStartTime,
                                               @DefaultValue("0") @QueryParam("queryEndTime") Long queryEndTime)
            throws APIInvocationException {
        try {
            // TODO: Read runtime and namespace from API
            Model model = ServiceHolder.getModelManager().getComponentDependencyModel(queryStartTime, queryEndTime,
                    "local", "default", instanceName, componentName);
            return Response.ok().entity(model).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred while fetching the dependency model for " +
                    "component: " + componentName + " in instance: " + instanceName, e);
        }
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }
}
