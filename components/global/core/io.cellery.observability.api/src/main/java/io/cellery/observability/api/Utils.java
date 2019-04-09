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

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Common utilities for the API.
 */
public class Utils {

    /**
     * Generate a Siddhi match condition to match any value from a array of values for a particular attribute.
     *
     * Eg:-
     *     Input  - traceId, ["id01", "id02", "id03"]
     *     Output - traceId == "id01" or traceId == "id02" or traceId == "id03"
     *
     * @param attributeName The name of the attribute
     * @param values The array of values from which at least one should match
     * @return The match condition which would match any value from the provided array
     */
    public static String generateSiddhiMatchConditionForAnyValues(String attributeName, String[] values) {
        StringBuilder traceIdMatchConditionBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                traceIdMatchConditionBuilder.append(" or ");
            }
            traceIdMatchConditionBuilder.append(attributeName)
                    .append(" == \"")
                    .append(values[i])
                    .append("\"");
        }
        return traceIdMatchConditionBuilder.toString();
    }

    /**
     * This is the method to by pass SSL check.
     *
     * @throws KeyManagementException this will be thrown if an issue occurs while executing this method
     * @throws NoSuchAlgorithmException this will be thrown if an issue occurs while executing this method
     */
    public static HttpClient getAllSSLClient() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                java.security.cert.X509Certificate[] obj = new java.security.cert.X509Certificate[1];
                return obj;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                // Nothing to implement
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                // Nothing to implement
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
