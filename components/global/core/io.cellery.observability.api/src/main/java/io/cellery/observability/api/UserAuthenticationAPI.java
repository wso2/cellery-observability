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

import io.cellery.observability.api.bean.CelleryConfig;
import io.cellery.observability.api.exception.APIInvocationException;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * MSF4J service for Authentication services.
 */
@Path("/api/auth")
public class UserAuthenticationAPI {

    private static String clientId;
    private static String clientSecret;

    public static void setClientId(char[] clientId) {
        UserAuthenticationAPI.clientId = String.valueOf(clientId);
    }

    public static void setClientSecret(char[] clientSecret) {
        UserAuthenticationAPI.clientSecret = String.valueOf(clientSecret);
    }

    private static final Logger log = Logger.getLogger(UserAuthenticationAPI.class);

    @GET
    @Path("/tokens/{authCode}")
    @Produces("application/json")
    public Response getTokens(@PathParam("authCode") String authCode) throws APIInvocationException {
        try {
            OAuthClientRequest request = OAuthClientRequest
                    .tokenLocation(CelleryConfig.getInstance().getDcrEnpoint() + Constants.TOKEN_ENDPOINT)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectURI(CelleryConfig.getInstance().getDashboardURL())
                    .setCode(authCode).buildBodyMessage();

            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
            JSONObject jsonObj = new JSONObject(oAuthResponse.getBody());
            Map<Object, Object> responseMap = new HashMap<>();

            responseMap.put(Constants.ACCESS_TOKEN, oAuthResponse.getAccessToken());
            responseMap.put(Constants.ID_TOKEN, jsonObj.get(Constants.ID_TOKEN));

            NewCookie cookie = new NewCookie("cookie-test", "cookie-testval", "/",
                    "", "cookie description", 1000000, false, false);
            return Response.ok().cookie(cookie).entity(responseMap).build();

        } catch (Throwable throwable) {
            throw new APIInvocationException("Unexpected error occurred while fetching the authentication tokens."
                    , throwable);
        }
    }

    @GET
    @Path("/client-id")
    @Produces("application/json")
    public Response getCredentials() {
        return Response.ok().entity(clientId).build();
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }

}
