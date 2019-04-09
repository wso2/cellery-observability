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

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.cellery.observability.api.Constants;
import io.cellery.observability.api.Utils;
import io.cellery.observability.api.bean.CelleryConfig;
import io.cellery.observability.api.exception.oidc.OIDCProviderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.config.ConfigurationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.ws.rs.core.MediaType;

/**
 * This class is used to create a DCR request for Service Provider registeration.
 */
public class OIDCOauthManager {

    private static final Logger log = Logger.getLogger(OIDCOauthManager.class);
    private static final String ERROR = "error";
    private static final String ACTIVE_STATUS = "active";
    private static final String BASIC_AUTH = "Basic ";
    private String clientId;
    private char[] clientSecret;

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return String.valueOf(this.clientSecret);
    }

    public OIDCOauthManager() throws OIDCProviderException {
        JSONObject clientJson = getClientCredentials();
        this.clientId = clientJson.getString(Constants.CLIENT_ID_TXT);
        this.clientSecret = clientJson.getString(Constants.CLIENT_SECRET_TXT).toCharArray();
    }

    private JSONObject getClientCredentials() throws OIDCProviderException {
        BufferedReader bufReader = null;
        InputStreamReader inputStreamReader = null;

        try {
            ArrayList<String> uris = new ArrayList<>(Arrays.asList(CelleryConfig.getInstance().getDashboardURL()));
            ArrayList<String> grants = new ArrayList<>(Arrays.asList(Constants.AUTHORIZATION_CODE));
            HttpClient client = Utils.getAllSSLClient();
            String dcrEP = CelleryConfig.getInstance().getIdpURL() + Constants.IDP_REGISTERATION_ENDPOINT;
            HttpPost request = constructDCRRequestBody(dcrEP, uris, grants);

            HttpResponse response = client.execute(request);
            inputStreamReader = new InputStreamReader(
                    response.getEntity().getContent(), StandardCharsets.UTF_8);
            bufReader = new BufferedReader(inputStreamReader);
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = bufReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            JSONObject jsonObject = new JSONObject(builder.toString());
            if (jsonObject.has(ERROR)) {
                try {
                    jsonObject = retrieveClientCredentials(dcrEP, client);
                    log.info("Client with name " + Constants.APPLICATION_NAME + " already exists.");

                } catch (OIDCProviderException e) {
                    throw new OIDCProviderException("Error while checking for existing client application in IDP." +
                            " Unable to retrieve existing client", e);
                }
            } else {
                jsonObject = new JSONObject(builder.toString());
            }
            return jsonObject;
        } catch (NoSuchAlgorithmException | KeyManagementException | ConfigurationException | IOException e) {
            throw new OIDCProviderException("Error occured while registering client", e);
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    log.debug("Error occured while closing buffered reader ", e);
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    log.debug("Error occured while closing input stream reader ", e);
                }
            }
        }
    }

    private HttpPost constructDCRRequestBody(String dcrEp, ArrayList<String> callbackUris,
                                             ArrayList<String> grants) throws ConfigurationException {
        HttpPost request = new HttpPost(dcrEp);
        JSONObject clientJson = new JSONObject();
        clientJson.put(Constants.CALL_BACK_URL, callbackUris);
        clientJson.put(Constants.CLIENT_NAME, Constants.APPLICATION_NAME);
        clientJson.put(Constants.GRANT_TYPE, grants);
        clientJson.put(Constants.EXT_PARAM_CLIENT_ID, Constants.STANDARD_CLIENT_ID);
        StringEntity requestEntity = new StringEntity(clientJson.toString(), ContentType.APPLICATION_JSON);
        request.setHeader(Constants.AUTHORIZATION, encodeAuthCredentials());
        request.setHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        request.setEntity(requestEntity);
        return request;
    }

    private String encodeAuthCredentials() throws ConfigurationException {
        String authString = CelleryConfig.getInstance().getIdpAdminUsername() + ":"
                + CelleryConfig.getInstance().getIdpAdminPassword();
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes(Charset.forName(StandardCharsets.UTF_8.name())));
        String authStringEnc = new String(authEncBytes, Charset.forName(StandardCharsets.UTF_8.name()));
        return BASIC_AUTH + authStringEnc;
    }

    private JSONObject retrieveClientCredentials(String dcrEp, HttpClient client) throws OIDCProviderException {
        try {
            HttpGet getRequest = new HttpGet(dcrEp + "?"
                    + Constants.CLIENT_NAME_PARAM + "=" + Constants.APPLICATION_NAME);
            getRequest.setHeader(Constants.AUTHORIZATION, encodeAuthCredentials());

            HttpResponse response = client.execute(getRequest);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, Charset.forName(StandardCharsets.UTF_8.name()));

            if (response.getStatusLine().getStatusCode() == 200 || result.contains(Constants.CLIENT_ID_TXT)) {
                return new JSONObject(result);
            } else {
                throw new OIDCProviderException("Error while retrieving client credentials." +
                        " Expected client credentials are not found in the response");
            }
        } catch (IOException | ConfigurationException e) {
            throw new OIDCProviderException("Error occured while checking for client with name " +
                    Constants.APPLICATION_NAME, e);
        }
    }

    public boolean validateToken(String token) throws OIDCProviderException {
        try {
            Unirest.setHttpClient(Utils.getAllSSLClient());
            com.mashape.unirest.http.HttpResponse<String> stringResponse
                    = Unirest.post(CelleryConfig.getInstance().getIdpURL() + Constants.INTROSPECT_ENDPOINT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                    .basicAuth(CelleryConfig.getInstance().getIdpAdminUsername()
                            , CelleryConfig.getInstance().getIdpAdminPassword()).body("token=" + token).asString();

            JSONObject jsonResponse = new JSONObject(stringResponse.getBody());
            if (stringResponse.getStatus() != 200) {
                log.error("Failed to connect to Introspect endpoint in Identity Provider server." +
                        " Exited with Status Code " + stringResponse.getStatus());
                return false;
            } else if (!jsonResponse.getBoolean(ACTIVE_STATUS)) {
                return false;
            }
        } catch (JSONException e) {
            log.error("Error occured while reading data from Introspect endpoint", e);
            return false;
        } catch (UnirestException | NoSuchAlgorithmException |
                KeyManagementException | ConfigurationException e) {
            throw new OIDCProviderException("Error occured while validating access token ", e);
        }
        return true;
    }
}
