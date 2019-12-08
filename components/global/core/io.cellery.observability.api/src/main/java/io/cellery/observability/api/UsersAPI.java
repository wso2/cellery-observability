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
import io.cellery.observability.api.internal.ServiceHolder;
import org.wso2.msf4j.Request;

import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * MSF4J service for User services.
 */
@Path("/api/users")
public class UsersAPI {

    @GET
    @Path("/runtimes/namespaces")
    @Produces("application/json")
    public Response getAuthorizedRunTimeNamespaces(@Context Request request) throws APIInvocationException {
        try {
            Object accessToken = request.getProperty(Constants.REQUEST_PROPERTY_ACCESS_TOKEN);
            if (accessToken instanceof String) {
                Map<String, String[]> availableRunTimeNamespaces
                        = ServiceHolder.getAuthProvider().getAuthorizedRuntimeNamespaces((String) accessToken);
                return Response.ok().entity(availableRunTimeNamespaces).build();
            } else {
                return Response.ok().entity(new JsonObject()).build();
            }
        } catch (Throwable e) {
            throw new APIInvocationException("Error while fetching authorized runtimes", e);
        }
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }

}
