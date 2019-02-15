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

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import io.cellery.observability.api.exception.APIInvocationException;
import org.wso2.msf4j.MicroservicesRunner;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * MSF4J service for Authentication services.
 */
@Path("/api/user-authentication")
public class UserAuthenticationAPI {

    @GET
    @Path("/auth/{username}")
    @Produces("application/json")
    public Response getComponentDependencyView(@PathParam("username") String username)
            throws APIInvocationException {
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.get("https://api.github.com/users/" + username)
                    .header("accept", "application/json")
                    .asJson();
            return Response.ok().entity(jsonResponse.getBody().getObject()).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred " +
                    "while fetching the dependency model for ", e);
        }
    }

    @GET
    @Path("/auth/requestToken/{authCode}")
    @Produces("application/json")
    public Response getAuthTokens(@PathParam("authCode") String authCode)
            throws APIInvocationException {
        try {

            return Response.ok().entity(authCode + 1).build();
        } catch (Throwable e) {
            throw new APIInvocationException("API Invocation error occurred" +
                    " while fetching the dependency model for ", e);
        }
    }

    @GET
    @Path("/hello")
    public String get() {
        // TODO: Implementation for HTTP GET request

        return "Hello from WSO2 MSF4J";
    }

    public static void main(String[] args) {
        new MicroservicesRunner()
                .deploy(new UserAuthenticationAPI())
                .start();
    }

}
