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
    @Path("/cells")
    @Produces("application/json")
    public Response getCellOverview(@DefaultValue("0") @QueryParam("queryStartTime") Long queryStartTime,
                                    @DefaultValue("0") @QueryParam("queryEndTime") Long queryEndTime)
            throws APIInvocationException {
        try {
            Model model = ServiceHolder.getModelManager().getGraph(queryStartTime, queryEndTime);
            return Response.ok().entity(model).build();
        } catch (Throwable e) {
            throw new APIInvocationException("Unexpected error occurred while fetching the Cell dependency model", e);
        }
    }

    @GET
    @Path("/cells/{cellName}")
    @Produces("application/json")
    public Response getCellDependencyView(@PathParam("cellName") String cellName,
                                          @DefaultValue("0") @QueryParam("queryStartTime") Long queryStartTime,
                                          @DefaultValue("0") @QueryParam("queryEndTime") Long queryEndTime)
            throws APIInvocationException {
        try {
            Model model = ServiceHolder.getModelManager().getDependencyModel(queryStartTime, queryEndTime, cellName);
            return Response.ok().entity(model).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred while fetching the dependency model for " +
                    "cell :" + cellName, e);
        }
    }

    @GET
    @Path("/cells/{cellName}/components/{componentName}")
    @Produces("application/json")
    public Response getComponentDependencyView(@PathParam("cellName") String cellName,
                                               @PathParam("componentName") String componentName,
                                               @DefaultValue("0") @QueryParam("queryStartTime") Long queryStartTime,
                                               @DefaultValue("0") @QueryParam("queryEndTime") Long queryEndTime)
            throws APIInvocationException {
        try {
            Model model = ServiceHolder.getModelManager().getDependencyModel(queryStartTime, queryEndTime, cellName,
                    componentName);
            return Response.ok().entity(model).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred while fetching the dependency model for " +
                    "component: " + componentName + " in cell: " + cellName, e);
        }
    }

    @OPTIONS
    @Path("/*")
    public Response getOptions() {
        return Response.ok().build();
    }
}
