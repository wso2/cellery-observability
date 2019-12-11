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
import com.google.gson.JsonParser;
import io.cellery.observability.api.exception.APIInvocationException;
import io.cellery.observability.api.internal.ServiceHolder;
import io.cellery.observability.auth.AuthConfig;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;

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
 *
 * All endpoints in this API are unauthenticated
 */
@Path("/api/auth")
public class AuthenticationAPI {
    private static final JsonParser jsonParser = new JsonParser();

    @GET
    @Path("/tokens/{authCode}")
    @Produces("application/json")
    public Response getTokens(@PathParam("authCode") String authCode) throws APIInvocationException {
        try {
            OAuthClientRequest oAuthClientRequest = OAuthClientRequest
                    .tokenLocation(AuthConfig.getInstance().getIdpUrl() + Constants.TOKEN_ENDPOINT)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(ServiceHolder.getAuthenticationProvider().getClientId())
                    .setClientSecret(ServiceHolder.getAuthenticationProvider().getClientSecret())
                    .setRedirectURI(AuthConfig.getInstance().getCallbackUrl())
                    .setCode(authCode).buildBodyMessage();

            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(oAuthClientRequest);
            JsonObject jsonObj = jsonParser.parse(oAuthResponse.getBody()).getAsJsonObject();

            String accessToken = oAuthResponse.getAccessToken();
            final int mid = accessToken.length() / 2;

            Map<Object, Object> responseMap = new HashMap<>();
            responseMap.put(Constants.ACCESS_TOKEN, accessToken.substring(0, mid));
            responseMap.put(Constants.ID_TOKEN, jsonObj.get(Constants.ID_TOKEN));

            NewCookie cookie = new NewCookie(Constants.HTTP_ONLY_SESSION_COOKIE, accessToken.substring(mid),
                    "/", "", "", 3600, false, true);

            return Response.ok().cookie(cookie).entity(responseMap).build();
        } catch (Throwable e) {
            throw new APIInvocationException("Error while getting tokens from token endpoint", e);
        }
    }

    @GET
    @Path("/client-id")
    @Produces("application/json")
    public Response getCredentials() throws APIInvocationException {
        try {
            return Response.ok().entity(ServiceHolder.getAuthenticationProvider().getClientId()).build();
        } catch (Throwable e) {
            throw new APIInvocationException("Error while getting Client ID for Observability Portal", e);
        }
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }

}
