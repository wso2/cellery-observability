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

package io.cellery.observability.api.auth;

import io.cellery.observability.api.Constants;
import io.cellery.observability.api.Utils;
import io.cellery.observability.api.bean.CelleryConfig;
import io.cellery.observability.api.exception.OIDCProviderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.wso2.carbon.config.ConfigurationException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager for managing Open ID Connect related functionality.
 */
public class OIDCOauthManager {

    private static final Logger logger = Logger.getLogger(OIDCOauthManager.class);

    private static final String ERROR = "error";
    private static final String ACTIVE_STATUS = "active";
    private static final String BASIC_AUTH = "Basic ";

    private String clientId;
    private char[] clientSecret;

    public OIDCOauthManager() {
        try {
            retrieveClientCredentials();
        } catch (OIDCProviderException e) {
            logger.warn("Fetching Client Credentials failed due to IDP unavailability, " +
                    "will be re-attempted when a user logs in");
        }
    }

    /**
     * Get the client ID of the SP created for observability portal.
     *
     * @return Client Id
     * @throws OIDCProviderException if fetching client id failed
     */
    public String getClientId() throws OIDCProviderException {
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
     * @throws OIDCProviderException if fetching client secret failed
     */
    public String getClientSecret() throws OIDCProviderException {
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
     * @return the credentials returned in JSON format
     * @throws OIDCProviderException if getting the Client Credentials fails
     */
    private void retrieveClientCredentials() throws OIDCProviderException {
        if (this.clientId == null || this.clientSecret == null) {
            JSONObject jsonObject = createClientWithDcr();
            if (jsonObject.has(ERROR)) {
                logger.info("Fetching the credentials of the already existing client " + Constants.APPLICATION_NAME);
                jsonObject = retrieveExistingClientCredentials();
            }

            this.clientId = jsonObject.getString(Constants.CLIENT_ID_TXT);
            this.clientSecret = jsonObject.getString(Constants.CLIENT_SECRET_TXT).toCharArray();
        }
    }

    /**
     * Register a Client for the Observability Portal.
     *
     * @return the HttpPost object
     * @throws OIDCProviderException if the HTTP call for creating the Client fails
     */
    private JSONObject createClientWithDcr() throws OIDCProviderException {
        try {
            List<String> callbackUris = Collections.singletonList(CelleryConfig.getInstance().getDashboardURL());
            List<String> grants = Collections.singletonList(Constants.AUTHORIZATION_CODE);

            String dcrEp = CelleryConfig.getInstance().getIdpURL() + Constants.IDP_REGISTERATION_ENDPOINT;
            JSONObject clientJson = new JSONObject();
            clientJson.put(Constants.CALL_BACK_URL, callbackUris);
            clientJson.put(Constants.CLIENT_NAME, Constants.APPLICATION_NAME);
            clientJson.put(Constants.GRANT_TYPE, grants);
            clientJson.put(Constants.EXT_PARAM_CLIENT_ID, Constants.STANDARD_CLIENT_ID);
            StringEntity requestEntity = new StringEntity(clientJson.toString(), ContentType.APPLICATION_JSON);

            HttpPost request = new HttpPost(dcrEp);
            request.setHeader(Constants.AUTHORIZATION, getEncodedIdpAdminCredentials());
            request.setHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
            request.setEntity(requestEntity);

            HttpClient client = Utils.getTrustAllClient();
            if (logger.isDebugEnabled()) {
                logger.debug("Creating new Client " + Constants.APPLICATION_NAME);
            }
            HttpResponse response = client.execute(request);
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException | ParseException | NoSuchAlgorithmException | KeyManagementException |
                ConfigurationException e) {
            throw new OIDCProviderException("Error occurred while registering client", e);
        }
    }

    /**
     * Retrieve the existing credentials for the Observability Portal Client.
     *
     * @return the credentials returned in JSON format
     * @throws OIDCProviderException if retrieving existing client credentials fails
     */
    private JSONObject retrieveExistingClientCredentials() throws OIDCProviderException {
        try {
            String dcrEp = CelleryConfig.getInstance().getIdpURL() + Constants.IDP_REGISTERATION_ENDPOINT;
            HttpGet request = new HttpGet(dcrEp + "?"
                    + Constants.CLIENT_NAME_PARAM + "=" + Constants.APPLICATION_NAME);
            request.setHeader(Constants.AUTHORIZATION, getEncodedIdpAdminCredentials());

            HttpClient client = Utils.getTrustAllClient();
            HttpResponse response = client.execute(request);
            String result = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() == 200 && result.contains(Constants.CLIENT_ID_TXT)) {
                return new JSONObject(result);
            } else {
                throw new OIDCProviderException("Error while retrieving client credentials." +
                        " Expected client credentials are not found in the response");
            }
        } catch (IOException | ParseException | NoSuchAlgorithmException | KeyManagementException |
                ConfigurationException e) {
            throw new OIDCProviderException("Error occurred while retrieving the client credentials with name " +
                    Constants.APPLICATION_NAME, e);
        }
    }

    /**
     * Validate the token.
     *
     * @param token This will be the access token
     * @return the boolean status of the validity of the access token
     * @throws OIDCProviderException if validating the token fails
     */
    public boolean validateToken(String token) throws OIDCProviderException {
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", token));

            String introspectEP = CelleryConfig.getInstance().getIdpURL() + Constants.INTROSPECT_ENDPOINT;
            HttpPost request = new HttpPost(introspectEP);
            request.setHeader(Constants.AUTHORIZATION, getEncodedIdpAdminCredentials());
            request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8.name()));

            HttpClient client = Utils.getTrustAllClient();
            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 400) {
                JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
                if (!jsonObject.getBoolean(ACTIVE_STATUS)) {
                    return false;
                }
            } else {
                logger.error("Failed to connect to Introspect endpoint in Identity Provider server." +
                        " Exited with Status Code " + statusCode);
                return false;
            }
        } catch (IOException | ParseException | NoSuchAlgorithmException | KeyManagementException |
                ConfigurationException e) {
            throw new OIDCProviderException("Error occurred while calling the introspect endpoint", e);
        }
        return true;
    }

    /**
     * Get the Base64 encoded IdP Admin Credentials.
     *
     * @return the String value of encoded credential is returned
     */
    private String getEncodedIdpAdminCredentials() throws ConfigurationException {
        String authString = CelleryConfig.getInstance().getIdpAdminUsername() + ":"
                + CelleryConfig.getInstance().getIdpAdminPassword();
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes(Charset.forName(StandardCharsets.UTF_8.name())));
        String authStringEnc = new String(authEncBytes, Charset.forName(StandardCharsets.UTF_8.name()));
        return BASIC_AUTH + authStringEnc;
    }
}
