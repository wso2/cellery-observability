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
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Auth related utilities.
 */
public class Utils {

    /**
     * Get a HTTP Client which bypasses the SSL checks.
     *
     * @return HTTP Client which bypasses SSL validations
     * @throws KeyManagementException this will be thrown if an issue occurs while executing this method
     * @throws NoSuchAlgorithmException this will be thrown if an issue occurs while executing this method
     */
    public static HttpClient getTrustAllClient() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                // Do Nothing
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                // Do Nothing
            }
        }};
        SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, trustManagers, null);
        SSLConnectionSocketFactory sslConnectionFactory =
                new SSLConnectionSocketFactory(context, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        PlainConnectionSocketFactory plainConnectionSocketFactory = new PlainConnectionSocketFactory();

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .register("http", plainConnectionSocketFactory)
                .build();
        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

        return HttpClientBuilder.create()
                .setSSLSocketFactory(sslConnectionFactory)
                .setConnectionManager(ccm)
                .build();
    }

    /**
     * Disable SSL verification for the default URL connection.
     */
    public static void disableSSLVerification() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                // Do Nothing
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                // Do Nothing
            }
        }};
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((string, sslSession) -> true);
    }

    private Utils() {   // Prevent initialization
    }
}
