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

package io.cellery.observability.api.idp;

import io.cellery.observability.api.Constants;
import io.cellery.observability.api.bean.CelleryConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This class is used to create a DCR request for Service Provider registeration.
 */
public class IdpClientManager {

    private static final Logger log = Logger.getLogger(IdpClientManager.class);
    private static final String ERROR = "error";

    public JSONObject getClientCredentials() throws IOException, ConfigurationException {
        BufferedReader bufReader = null;
        InputStreamReader inputStreamReader = null;
        JSONObject jsonObject = null;
        ArrayList<String> uris = new ArrayList<>(Arrays.asList(CelleryConfig.getInstance().getDashboardURL()));
        ArrayList<String> grants = new ArrayList<>(Arrays.asList(Constants.AUTHORIZATION_CODE));
        try {
            HttpClient client = getAllSSLClient();
            String dcrEP = CelleryConfig.getInstance().getIdpURL() + Constants.IDP_REGISTERATION_ENDPOINT;
            HttpPost request = constructRequestBody(dcrEP, uris, grants);

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
            jsonObject = new JSONObject(builder.toString());
            if (jsonObject.has(ERROR)) {
                jsonObject = checkRegisteration(dcrEP, client);
            } else {
                jsonObject = new JSONObject(builder.toString());
            }
            return jsonObject;
        } catch (NoSuchAlgorithmException | KeyManagementException | ConfigurationException e) {
            log.error("Error while fetching the Client-Id for the dynamically created client ", e);
            return jsonObject;
        } finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (IOException e) {
                    log.error("Error in closing the BufferedReader", e);
                }
            }
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    log.error("Error in closing the InputStreamReader", e);
                }
            }
        }
    }

    private HttpPost constructRequestBody(String dcrEp, ArrayList<String> callbackUris,
                                          ArrayList<String> grants) throws ConfigurationException {
        HttpPost request =
                new HttpPost(dcrEp);
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
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes(Charset.forName("UTF-8")));
        String authStringEnc = new String(authEncBytes, Charset.forName("UTF-8"));
        return "Basic " + authStringEnc;
    }

    private static HttpClient getAllSSLClient()
            throws NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                java.security.cert.X509Certificate[] obj = new java.security.cert.X509Certificate[1];
                return obj;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        }};
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, trustAllCerts, null);

        HttpClientBuilder builder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslConnectionFactory =
                new SSLConnectionSocketFactory(context, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        builder.setSSLSocketFactory(sslConnectionFactory);

        PlainConnectionSocketFactory plainConnectionSocketFactory = new PlainConnectionSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .register("http", plainConnectionSocketFactory)
                .build();

        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
        builder.setConnectionManager(ccm);
        return builder.build();
    }

    private JSONObject checkRegisteration(String dcrEp, HttpClient client) throws ConfigurationException, IOException {
        try {
            HttpGet getRequest = new HttpGet(dcrEp + "?"
                    + Constants.CLIENT_NAME_PARAM + "=" + Constants.APPLICATION_NAME);
            getRequest.setHeader(Constants.AUTHORIZATION, encodeAuthCredentials());

            HttpResponse resp = client.execute(getRequest);
            HttpEntity entity = resp.getEntity();
            String result = EntityUtils.toString(entity, Charset.forName("utf-8"));
            return new JSONObject(result);
        } catch (IOException | ConfigurationException e) {
            log.error("Error occured while verifying existing clients in IDP", e);
            throw e;
        }
    }
}
