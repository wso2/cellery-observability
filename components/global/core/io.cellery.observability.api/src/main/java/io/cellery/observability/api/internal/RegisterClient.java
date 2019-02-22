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

package io.cellery.observability.api.internal;

import io.cellery.observability.api.AggregatedRequestsAPI;
import io.cellery.observability.api.Constants;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This class is used to create a DCR request for Service Provider registeration.
 */
public class RegisterClient {

    private static final Logger log = Logger.getLogger(AggregatedRequestsAPI.class);

    public static JSONObject getClientCredentials() throws IOException {
        BufferedReader bufReader = null;
        InputStreamReader inputStreamReader = null;
        JSONObject jsonObject = null;
        try {
            HttpClient client = getAllSSLClient();
            HttpPost request =
                    new HttpPost(Constants.CLIENT_REGISTERATION_ENDPOINT);
            JSONObject clientJson = new JSONObject();
            clientJson.put(Constants.CALL_BACK_URL, Constants.OBSERVABILITY_DASHBOARD_URL);
            clientJson.put(Constants.CLIENT_NAME, Constants.APPLICATION_NAME);
            clientJson.put(Constants.OWNER, Constants.ADMIN);
            clientJson.put(Constants.GRANT_TYPE, Constants.AUTHORIZATION_CODE);
            clientJson.put(Constants.SAAS_APP, Constants.TRUE);
            request.setHeader(Constants.AUTHORIZATION, Constants.BASIC_ADMIN_AUTH);
            request.setHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
            request.setEntity(new StringEntity(clientJson.toString()));

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
            return jsonObject;
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Error while fetching the Client-Id for the dynamically created service provider ", e);
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
        return jsonObject;
    }

    public static HttpClient getAllSSLClient()
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

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
}

