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

package io.cellery.observability.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cellery.observability.auth.exception.AuthProviderException;
import io.cellery.observability.auth.internal.AuthConfig;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.wso2.carbon.config.ConfigurationException;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Dynamic Client Registration Provider for Cellery Observability.
 */
public class DcrProvider {
    private static final Logger logger = Logger.getLogger(DcrProvider.class);

    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();

    private static final String ERROR = "error";

    private String clientId;
    private char[] clientSecret;

    public DcrProvider() {
        try {
            retrieveClientCredentials();
        } catch (AuthProviderException | ConfigurationException e) {
            logger.warn("Fetching Client Credentials failed due to IDP unavailability, " +
                    "will be re-attempted when a user logs in", e);
        }
    }

    /**
     * Get the client ID of the SP created for observability portal.
     *
     * @return Client Id
     * @throws AuthProviderException if fetching client id failed
     */
    public String getClientId() throws AuthProviderException, ConfigurationException {
        if (this.clientId == null) {
            synchronized (this) {
                retrieveClientCredentials();
            }
        }
        return this.clientId;
    }

    /**
     * Get the client secret of the SP created for observability portal.
     *
     * @return Client secret
     * @throws AuthProviderException if fetching client secret failed
     */
    public String getClientSecret() throws AuthProviderException, ConfigurationException {
        if (this.clientSecret == null) {
            synchronized (this) {
                retrieveClientCredentials();
            }
        }
        return String.valueOf(this.clientSecret);
    }

    /**
     * This method will fetch or create and fetch (if client application is not registered)
     * the client application's credentials from IDP.
     *
     * @throws AuthProviderException if getting the Client Credentials fails
     */
    private void retrieveClientCredentials() throws AuthProviderException, ConfigurationException {
        if (this.clientId == null || this.clientSecret == null) {
            JsonObject jsonObject = createNewClient();
            if (jsonObject.has(ERROR)) {
                jsonObject = retrieveExistingClientCredentials();
                logger.info("Fetched the credentials of the already existing client "
                        + AuthConfig.getInstance().getDcrClientName());
            } else {
                logger.info("Created new Client " + AuthConfig.getInstance().getDcrClientName());
            }

            this.clientId = jsonObject.get(Constants.OIDC_CLIENT_ID_KEY).getAsString();
            this.clientSecret = jsonObject.get(Constants.OIDC_CLIENT_SECRET_KEY).getAsString().toCharArray();
        }
    }

    /**
     * Register a Client for the Observability Portal.
     *
     * @return the HttpPost object
     * @throws AuthProviderException if the HTTP call for creating the Client fails
     */
    private JsonObject createNewClient() throws AuthProviderException {
        try {
            JsonArray callbackUris = new JsonArray(1);
            callbackUris.add(AuthConfig.getInstance().getPortalHomeUrl());

            JsonArray grants = new JsonArray(1);
            grants.add(Constants.OIDC_AUTHORIZATION_CODE_KEY);

            String dcrEp = AuthConfig.getInstance().getIdpUrl() + AuthConfig.getInstance().getIdpDcrRegisterEndpoint();
            JsonObject clientJson = new JsonObject();
            clientJson.addProperty(Constants.OIDC_EXT_PARAM_CLIENT_ID_KEY, AuthConfig.getInstance().getDcrClientId());
            clientJson.addProperty(Constants.OIDC_CLIENT_NAME_KEY, AuthConfig.getInstance().getDcrClientName());
            clientJson.add(Constants.OIDC_CALLBACK_URL_KEY, callbackUris);
            clientJson.add(Constants.OIDC_GRANT_TYPES_KEY, grants);

            StringEntity requestEntity = new StringEntity(gson.toJson(clientJson), ContentType.APPLICATION_JSON);

            HttpPost request = new HttpPost(dcrEp);
            request.setHeader(Constants.HEADER_AUTHORIZATION, AuthUtils.generateBasicAuthHeaderValue(
                    AuthConfig.getInstance().getIdpUsername(), AuthConfig.getInstance().getIdpPassword()));
            request.setHeader(Constants.HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_APPLICATION_JSON);
            request.setEntity(requestEntity);

            HttpClient client = AuthUtils.getTrustAllClient();
            HttpResponse response = client.execute(request);
            return jsonParser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
        } catch (IOException | ParseException | NoSuchAlgorithmException | KeyManagementException |
                ConfigurationException e) {
            throw new AuthProviderException("Error occurred while registering client", e);
        }
    }

    /**
     * Retrieve the existing credentials for the Observability Portal Client.
     *
     * @return the credentials returned in JSON format
     * @throws AuthProviderException if retrieving existing client credentials fails
     */
    private JsonObject retrieveExistingClientCredentials() throws AuthProviderException, ConfigurationException {
        try {
            String dcrEp = AuthConfig.getInstance().getIdpUrl() + AuthConfig.getInstance().getIdpDcrRegisterEndpoint();
            HttpGet request = new HttpGet(dcrEp + "?"
                    + Constants.OIDC_CLIENT_NAME_KEY + "=" + AuthConfig.getInstance().getDcrClientName());
            request.setHeader(Constants.HEADER_AUTHORIZATION, AuthUtils.generateBasicAuthHeaderValue(
                    AuthConfig.getInstance().getIdpUsername(), AuthConfig.getInstance().getIdpPassword()));

            HttpClient client = AuthUtils.getTrustAllClient();
            HttpResponse response = client.execute(request);
            String result = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() == 200 && result.contains(Constants.OIDC_CLIENT_ID_KEY)) {
                return jsonParser.parse(result).getAsJsonObject();
            } else {
                throw new AuthProviderException("Error while retrieving client credentials." +
                        " Expected client credentials are not found in the response");
            }
        } catch (IOException | ParseException | NoSuchAlgorithmException | KeyManagementException |
                ConfigurationException e) {
            throw new AuthProviderException("Error occurred while retrieving the client credentials with name " +
                    AuthConfig.getInstance().getDcrClientName(), e);
        }
    }
}
