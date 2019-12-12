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
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import sun.security.ssl.SSLSocketFactoryImpl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Test Cases for API Utils.
 */
@PrepareForTest(HttpsURLConnection.class)
@PowerMockIgnore({"org.apache.log4j.*", "javax.net.ssl.*"})
public class AuthUtilsTestCase {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void testGetTrustAllClient() throws KeyManagementException, NoSuchAlgorithmException, CertificateException {
        HttpClient httpClient = AuthUtils.getTrustAllClient();

        BasicHttpClientConnectionManager connectionManager =
                Whitebox.getInternalState(httpClient, "connManager");
        Object connectionOperator = Whitebox.getInternalState(connectionManager, "connectionOperator");
        Map<String, ConnectionSocketFactory> socketFactoryRegistry =
                Whitebox.getInternalState(
                        Whitebox.getInternalState(connectionOperator, "socketFactoryRegistry"), "map");
        ConnectionSocketFactory httpSocketFactory = socketFactoryRegistry.get("http");
        SSLConnectionSocketFactory httpsSocketFactory = (SSLConnectionSocketFactory) socketFactoryRegistry.get("https");
        SSLSocketFactoryImpl sslSocketFactory = Whitebox.getInternalState(httpsSocketFactory, "socketfactory");
        TrustManager trustManager = Whitebox.getInternalState(
                Whitebox.getInternalState(Whitebox.getInternalState(sslSocketFactory,  "context"),
                        "trustManager"),
                "tm");

        Assert.assertNotNull(httpClient);
        Assert.assertEquals(socketFactoryRegistry.size(), 2);
        Assert.assertNotNull(httpSocketFactory);
        Assert.assertTrue(httpSocketFactory instanceof PlainConnectionSocketFactory);
        Assert.assertEquals(Whitebox.getInternalState(httpsSocketFactory, "hostnameVerifier"),
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        Assert.assertTrue(trustManager instanceof X509TrustManager);
        X509TrustManager actualTrustManager = (X509TrustManager) trustManager;
        Assert.assertEquals(actualTrustManager.getAcceptedIssuers().length, 0);
        actualTrustManager.checkClientTrusted(new X509Certificate[]{}, "test1");    // Should not throw exceptions
        actualTrustManager.checkServerTrusted(new X509Certificate[]{}, "test2");    // Should not throw exceptions
    }

    @Test
    public void testDisableSslVerification() throws Exception {
        SSLSocketFactory sslSocketFactory;
        HostnameVerifier hostnameVerifier;
        PowerMockito.mockStatic(HttpsURLConnection.class);

        SSLSocketFactory[] sslSocketFactoryHolder = new SSLSocketFactory[1];
        PowerMockito
                .doAnswer(invocation -> sslSocketFactoryHolder[0] = invocation.getArgumentAt(0, SSLSocketFactory.class))
                .when(HttpsURLConnection.class, "setDefaultSSLSocketFactory",
                        Mockito.any(SSLSocketFactory.class));
        HostnameVerifier[] hostnameVerifierHolder = new HostnameVerifier[1];
        PowerMockito
                .doAnswer(invocation -> hostnameVerifierHolder[0] = invocation.getArgumentAt(0, HostnameVerifier.class))
                .when(HttpsURLConnection.class, "setDefaultHostnameVerifier",
                        Mockito.any(HostnameVerifier.class));

        AuthUtils.disableSSLVerification();
        sslSocketFactory = sslSocketFactoryHolder[0];
        hostnameVerifier = hostnameVerifierHolder[0];

        Assert.assertNotNull(hostnameVerifier);
        Assert.assertTrue(hostnameVerifier.verify("test1", Mockito.mock(SSLSession.class)));
        Assert.assertNotNull(sslSocketFactory);

        TrustManager trustManager = Whitebox.getInternalState(
                Whitebox.getInternalState(Whitebox.getInternalState(sslSocketFactory,  "context"),
                        "trustManager"),
                "tm");

        Assert.assertTrue(trustManager instanceof X509TrustManager);
        X509TrustManager actualTrustManager = (X509TrustManager) trustManager;
        Assert.assertEquals(actualTrustManager.getAcceptedIssuers().length, 0);
        actualTrustManager.checkClientTrusted(new X509Certificate[]{}, "test1");    // Should not throw exceptions
        actualTrustManager.checkServerTrusted(new X509Certificate[]{}, "test2");    // Should not throw exceptions
    }
}
