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

import io.cellery.observability.api.configs.CelleryConfig;
import io.cellery.observability.api.exception.APIInvocationException;
import io.cellery.observability.api.interceptor.AuthInterceptor;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

/**
 * MSF4J service for Authentication services.
 */
@Path("/api/auth")
public class UserAuthenticationAPI {

    private static String clientId;
    private static String clientSecret;

    public static String getClientSecret() {
        return clientSecret;
    }

    public static String getClientId() {
        return clientId;
    }

    public static void setClientId(String clientId) {
        UserAuthenticationAPI.clientId = clientId;
    }

    public static void setClientSecret(String clientSecret) {
        UserAuthenticationAPI.clientSecret = clientSecret;
    }

    private static final Logger log = Logger.getLogger(UserAuthenticationAPI.class);

    @GET
    @Path("/tokens/{authCode}")
    @Produces("application/json")
    public Response getTokens(@PathParam("authCode") String authCode) throws APIInvocationException {
        try {
            disableSSLVerification();
            OAuthClientRequest request = OAuthClientRequest
                    .tokenLocation(CelleryConfig.getInstance().getTokenEndpoint())
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectURI(CelleryConfig.getInstance().getDashboardURL())
                    .setCode(authCode).buildBodyMessage();

            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request);
            JSONObject jsonObj = new JSONObject(oAuthResponse.getBody());
            Map<Object, Object> responseMap = new HashMap<>();

            // Break the access token into two halves and send one to the frontend for security purposes
            String[] tokenParts = splitToken(oAuthResponse.getAccessToken());
            AuthInterceptor.setHalfToken(tokenParts[0]);

            responseMap.put("access_token", tokenParts[1]);
            responseMap.put("id_token", jsonObj.get(Constants.ID_TOKEN));

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
        disableSSLVerification();
        return Response.ok().entity(clientId).build();
    }

    @GET
    @Path("/hello")
    @Produces("application/json")
    public Response getHello() throws APIInvocationException {

        try {
            Map<Object, Object> responseMap = new HashMap<>();
            responseMap.put("access_token", "hello");
            responseMap.put("id_token", "test");
            NewCookie cookie = new NewCookie("cookie-test", "cookie-testval", "/",
                    "", "cookie description", 1000000, false, false);
            return Response.ok().cookie(cookie).entity(responseMap).build();
        } catch (Throwable throwable) {
            throw new APIInvocationException("Unexpected error occurred while fetching the authentication tokens."
                    , throwable);
        }
    }

//    @GET
//    @Path("/getAccessToken/{refresh_token}")
//    @Produces("application/json")
//    public Response getNewTokens(@PathParam("refresh_token") String refreshToken) {
//        JSONObject jsonResponse;
//        try {
//            SSLContext sslcontext = SSLContexts.custom()
//                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
//                    .build();
//
//            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext,
//                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//            CloseableHttpClient httpclient = HttpClients.custom()
//                    .setSSLSocketFactory(sslsf)
//                    .build();
//            Unirest.setHttpClient(httpclient);
//            HttpResponse<String> stringResponse
//                    = Unirest.post(Constants.INTERNAL_TOKEN_LOCATION)
//                    .header("Content-Type", "application/x-www-form-urlencoded")
//                    .basicAuth(UserAuthenticationAPI.getClientId(), UserAuthenticationAPI.getClientSecret())
//                    .body("grant_type=refresh_token&refresh_token=" + refreshToken).asString();
//
//            jsonResponse = new JSONObject(stringResponse.getBody());
//
//
//        } catch (UnirestException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
//            jsonResponse = new JSONObject("{\"error\":\"error while fetching data\"}");
//
//        }
//            return Response.ok().entity(jsonResponse).build();
//    }

    private static void disableSSLVerification() {
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
            log.error("Error occured while disabling SSL verification", e);
        }
    }

    private static String[] splitToken(String token) {
        final int mid = token.length() / 2;
        //get the middle of the String
        return new String[]{token.substring(0, mid), token.substring(mid)};
    }

    @OPTIONS
    @Path(".*")
    public Response getOptions() {
        return Response.ok().build();
    }

}
