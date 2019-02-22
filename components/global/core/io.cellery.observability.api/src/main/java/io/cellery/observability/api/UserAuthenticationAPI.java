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
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * MSF4J service for Authentication services.
 */
@Path("/api/user-auth")
public class UserAuthenticationAPI {
    private String clientId;
    private String clientSecret;

    private static final Logger log = Logger.getLogger(AggregatedRequestsAPI.class);

    public UserAuthenticationAPI(String id, String secret) {
        this.clientId = id;
        this.clientSecret = secret;
    }

    @GET
    @Path("/requestToken/{authCode}")
    @Produces("application/json")
    public Response getTokens(@PathParam("authCode") String authCode) throws APIInvocationException {
        try {
            cancelCheck();
            OAuthClientRequest request = OAuthClientRequest
                    .tokenLocation(Constants.INTERNAL_TOKEN_LOCATION)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectURI(Constants.OBSERVABILITY_DASHBOARD_URL)
                    .setCode(authCode).buildBodyMessage();

            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
            JSONObject obj = new JSONObject(oAuthResponse.getBody());

            return Response.ok().entity(obj.get(Constants.ID_TOKEN)).build();

        } catch (Throwable throwable) {
            throw new APIInvocationException("Unexpected error occurred while fetching the aggregated HTTP request " +
                    "data for cells", throwable);
        }
    }

    @GET
    @Path("/getCredentials/client")
    @Produces("application/json")
    public Response getCredentials() {
        cancelCheck();
        return Response.ok().entity(clientId).build();
    }

    private static void cancelCheck() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String string, SSLSession ssls) {
                    return true;
                }
            });
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Error", e);
        }
    }

    @OPTIONS
    @Path("/*")
    public Response getOptions() {
        return Response.ok().build();
    }

}
