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
import io.cellery.observability.api.internal.ServiceHolder;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Test Cases for OIDC OAuth Manager
 */
@PrepareForTest({Utils.class, CelleryConfig.class, OIDCOauthManager.class, EntityUtils.class})
@PowerMockIgnore("org.apache.log4j.*")
public class OIDCOauthManagerTestCase {

    private static final String DASHBOARD_URL = "http://cellery-dashboard";
    private static final String IDP_URL = "http://idp.cellery-system:9443";
    private static final String IDP_ADMIN_USERNAME = "testadminuser";
    private static final String IDP_ADMIN_PASSWORD = "testadminpass";

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @BeforeClass
    public void initTestCase() {
        CelleryConfig celleryConfig = new CelleryConfig();
        Whitebox.setInternalState(celleryConfig, "dashboardURL", DASHBOARD_URL);
        Whitebox.setInternalState(celleryConfig, "idpURL", IDP_URL);
        Whitebox.setInternalState(celleryConfig, "idpAdminUsername", IDP_ADMIN_USERNAME);
        Whitebox.setInternalState(celleryConfig, "idpAdminPassword", IDP_ADMIN_PASSWORD);
        Whitebox.setInternalState(CelleryConfig.class, "celleryConfig", celleryConfig);
    }

    @Test
    public void testInitialization() throws Exception {
        String clientId = "testClientId1";
        String clientSecret = "snoewjn324vdsfew";

        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
                    Assert.assertEquals(request.getMethod(), "POST");
                    Assert.assertEquals(request.getURI().getRawPath(), Constants.IDP_REGISTERATION_ENDPOINT);
                    Assert.assertEquals(request.getURI().getHost(), "idp.cellery-system");
                    Assert.assertEquals(request.getURI().getPort(), 9443);

                    HttpEntity entity = Whitebox.getInternalState(request, "entity");
                    byte[] content = Whitebox.getInternalState(entity, "content");
                    String actualRequestBody = new String(content, StandardCharsets.UTF_8);

                    JSONObject requestBodyJson = new JSONObject(actualRequestBody);
                    Assert.assertEquals(requestBodyJson.getJSONArray(Constants.GRANT_TYPE).length(), 1);
                    Assert.assertEquals(requestBodyJson.getJSONArray(Constants.GRANT_TYPE).get(0),
                            Constants.AUTHORIZATION_CODE);
                    Assert.assertEquals(requestBodyJson.getJSONArray(Constants.CALL_BACK_URL).length(), 1);
                    Assert.assertEquals(requestBodyJson.getJSONArray(Constants.CALL_BACK_URL).get(0), DASHBOARD_URL);
                    Assert.assertEquals(requestBodyJson.getString(Constants.CLIENT_NAME), Constants.APPLICATION_NAME);
                    Assert.assertEquals(requestBodyJson.getString(Constants.EXT_PARAM_CLIENT_ID),
                            Constants.STANDARD_CLIENT_ID);

                    Assert.assertEquals(request.getFirstHeader(Constants.AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");
                    Assert.assertEquals(request.getFirstHeader(Constants.CONTENT_TYPE).getValue(),
                            Constants.APPLICATION_JSON);

                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenReturn(httpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();

        Assert.assertEquals(oidcOauthManager.getClientId(), clientId);
        Assert.assertEquals(oidcOauthManager.getClientSecret(), clientSecret);
    }

    @Test
    public void testInitializationWithConfigurationException() throws Exception {
        ConfigProvider configProvider = Mockito.mock(ConfigProvider.class);
        Mockito.when(configProvider.getConfigurationObject(CelleryConfig.class))
                .thenThrow(new ConfigurationException("Test Exception"));
        ServiceHolder.setConfigProvider(configProvider);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
        ServiceHolder.setConfigProvider(null);
    }

    @Test
    public void testInitializationWithHttpRequestThrowingIOException() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenReturn(httpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testInitializationWithTrustAllClientThrowingKeyManagementException() throws Exception {
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenThrow(new KeyManagementException("Test Exception"));

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testInitializationWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenThrow(new NoSuchAlgorithmException("Test Exception"));

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testInitializationWithGetContentThrowingIOException() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    HttpResponse response = Mockito.mock(HttpResponse.class);
                    HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
                    Mockito.when(httpEntity.getContent()).thenThrow(new IOException("Test Exception"));
                    Mockito.when(response.getEntity()).thenReturn(httpEntity);
                    return response;
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenReturn(httpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testInitializationWithEntityUtilsThrowingParseException() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    Header header = Mockito.mock(Header.class);
                    Mockito.when(header.getElements()).thenThrow(new ParseException("Test Exception"));
                    HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
                    Mockito.when(httpEntity.getContentType()).thenReturn(header);

                    HttpResponse response = Mockito.mock(HttpResponse.class);
                    Mockito.when(response.getEntity()).thenReturn(httpEntity);
                    return response;
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenReturn(httpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testGetExistingApp() throws Exception {
        String clientId = "testClientId2";
        String clientSecret = "34fdnijn4rs";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
                    Assert.assertEquals(request.getMethod(), "GET");
                    Assert.assertEquals(request.getURI().getRawPath(), Constants.IDP_REGISTERATION_ENDPOINT);
                    Assert.assertEquals(request.getURI().getHost(), "idp.cellery-system");
                    Assert.assertEquals(request.getURI().getPort(), 9443);
                    Assert.assertEquals(request.getURI().getQuery(),
                            Constants.CLIENT_NAME_PARAM + "=" + Constants.APPLICATION_NAME);

                    Assert.assertEquals(request.getFirstHeader(Constants.AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");

                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();

        Assert.assertEquals(oidcOauthManager.getClientId(), clientId);
        Assert.assertEquals(oidcOauthManager.getClientSecret(), clientSecret);
    }

    @Test
    public void testGetExistingAppWithErrorStatusCode() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "internal_error");
                    responseJson.put("description", "Internal server error");
                    return generateHttpResponse(responseJson, 500);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithErrorMessage() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "test_error");
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithHttpRequestThrowingIOException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithTrustAllClientThrowingKeyManagementException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new KeyManagementException("Test Exception"));

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new NoSuchAlgorithmException("Test Exception"));

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithGetContentThrowingIOException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    HttpResponse response = Mockito.mock(HttpResponse.class);
                    HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
                    Mockito.when(httpEntity.getContent()).thenThrow(new IOException("Test Exception"));
                    Mockito.when(response.getEntity()).thenReturn(httpEntity);
                    return response;
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithEntityUtilsThrowingParseException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "invalid_client_metadata");
                    responseJson.put("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    Header header = Mockito.mock(Header.class);
                    Mockito.when(header.getElements()).thenThrow(new ParseException("Test Exception"));
                    HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
                    Mockito.when(httpEntity.getContentType()).thenReturn(header);

                    HttpResponse response = Mockito.mock(HttpResponse.class);
                    Mockito.when(response.getEntity()).thenReturn(httpEntity);
                    return response;
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(oidcOauthManager, "clientSecret"));
    }

    @Test
    public void testValidateToken() throws Exception {
        String clientId = "testClientId3";
        String clientSecret = "ksdfkwnrb32";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
                    Assert.assertEquals(request.getMethod(), "POST");
                    Assert.assertEquals(request.getURI().getRawPath(), Constants.INTROSPECT_ENDPOINT);
                    Assert.assertEquals(request.getURI().getHost(), "idp.cellery-system");
                    Assert.assertEquals(request.getURI().getPort(), 9443);

                    HttpEntity entity = Whitebox.getInternalState(request, "entity");
                    byte[] content = Whitebox.getInternalState(entity, "content");
                    String actualRequestBody = new String(content, StandardCharsets.UTF_8);
                    Assert.assertEquals(actualRequestBody, "token=test+token+1");

                    Assert.assertEquals(request.getFirstHeader(Constants.AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");

                    JSONObject responseJson = new JSONObject();
                    responseJson.put("active", true);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(validationHttpClient);

        Assert.assertTrue(oidcOauthManager.validateToken("test token 1"));
    }

    @Test
    public void testValidateTokenWithInvalidToken() throws Exception {
        String clientId = "testClientId4";
        String clientSecret = "sdf345fsdf432";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("active", false);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(validationHttpClient);

        Assert.assertFalse(oidcOauthManager.validateToken("test token 2"));
    }

    @Test
    public void testValidateTokenWith1xxStatusCode() throws Exception {
        String clientId = "testClientId5";
        String clientSecret = "4ngdjrk4j432";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();

        int[] statusCodes = new int[]{100, 102};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JSONObject responseJson = new JSONObject();
                        return generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(oidcOauthManager.validateToken("test token 3-" + i));
        }
    }

    @Test
    public void testValidateTokenWithSuccessStatusCode() throws Exception {
        String clientId = "testClientId6";
        String clientSecret = "i4nsdfm4fn44";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();

        int[] statusCodes = new int[]{200, 202, 302, 304};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JSONObject responseJson = new JSONObject();
                        responseJson.put("active", false);
                        return generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(oidcOauthManager.validateToken("test token 4-" + i));
        }
    }

    @Test
    public void testValidateTokenWithErrorStatusCode() throws Exception {
        String clientId = "testClientId7";
        String clientSecret = "unkjvnfwervsd";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();

        int[] statusCodes = new int[]{400, 404, 500, 502};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JSONObject responseJson = new JSONObject();
                        return generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(oidcOauthManager.validateToken("test token 5-" + i));
        }
    }

    @Test(expectedExceptions = OIDCProviderException.class)
    public void testValidateTokenWithHttpRequestThrowingIOException() throws Exception {
        String clientId = "testClientId8";
        String clientSecret = "m34uniscrf4rv";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(validationHttpClient);
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        oidcOauthManager.validateToken("test token 6");
    }

    @Test(expectedExceptions = OIDCProviderException.class)
    public void testValidateTokenWithTrustAllClientThrowingKeyManagementException() throws Exception {
        String clientId = "testClientId9";
        String clientSecret = "4unifhbs4hbr4";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new KeyManagementException("Test Exception"));
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        oidcOauthManager.validateToken("test token 7");
    }

    @Test(expectedExceptions = OIDCProviderException.class)
    public void testValidateTokenWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        String clientId = "testClientId10";
        String clientSecret = "niu43nfhref4f4";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new NoSuchAlgorithmException("Test Exception"));
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        oidcOauthManager.validateToken("test token 8");
    }

    @Test(expectedExceptions = OIDCProviderException.class)
    public void testValidateTokenWithGetContentThrowingIOException() throws Exception {
        String clientId = "testClientId11";
        String clientSecret = "452nkjnkjbr44r";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    HttpResponse response = Mockito.mock(HttpResponse.class);
                    HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
                    Mockito.when(httpEntity.getContent()).thenThrow(new IOException("Test Exception"));
                    Mockito.when(response.getEntity()).thenReturn(httpEntity);

                    StatusLine statusLine = Mockito.mock(StatusLine.class);
                    Mockito.when(statusLine.getStatusCode()).thenReturn(200);
                    Mockito.when(response.getStatusLine()).thenReturn(statusLine);
                    return response;
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);
        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        oidcOauthManager.validateToken("test token 9");
    }

    @Test(expectedExceptions = OIDCProviderException.class)
    public void testValidateTokenWithEntityUtilsThrowingParseException() throws Exception {
        String clientId = "testClientId12";
        String clientSecret = "0345ihver43r43";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put(Constants.CLIENT_ID_TXT, clientId);
                    responseJson.put(Constants.CLIENT_SECRET_TXT, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    Header header = Mockito.mock(Header.class);
                    Mockito.when(header.getElements()).thenThrow(new ParseException("Test Exception"));
                    HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
                    Mockito.when(httpEntity.getContentType()).thenReturn(header);

                    HttpResponse response = Mockito.mock(HttpResponse.class);
                    Mockito.when(response.getEntity()).thenReturn(httpEntity);

                    StatusLine statusLine = Mockito.mock(StatusLine.class);
                    Mockito.when(statusLine.getStatusCode()).thenReturn(200);
                    Mockito.when(response.getStatusLine()).thenReturn(statusLine);
                    return response;
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        OIDCOauthManager oidcOauthManager = new OIDCOauthManager();
        oidcOauthManager.validateToken("test token 10");
    }

    /**
     * Generate a mock Http Response object that would return the string of the JSON object provided.
     *
     * @param jsonObject The JSON object to be returned from the HTTP response
     * @param statusCode The status code of the response
     * @return The HTTP response
     * @throws Exception if mocking fails
     */
    private HttpResponse generateHttpResponse(JSONObject jsonObject, int statusCode) throws Exception {
        char[] resultantCharArray = jsonObject.toString().toCharArray();
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        Mockito.when(httpEntity.getContent()).thenReturn(inputStream);
        Mockito.when(httpEntity.getContentLength()).thenReturn((long) resultantCharArray.length);

        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.when(statusLine.getStatusCode()).thenReturn(statusCode);
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);

        NameValuePair nameValuePair = Mockito.mock(NameValuePair.class);
        Mockito.when(nameValuePair.getName()).thenReturn("charset");
        Mockito.when(nameValuePair.getValue()).thenReturn(StandardCharsets.UTF_8.toString());
        HeaderElement headerElement = Mockito.mock(HeaderElement.class);
        Mockito.when(headerElement.getParameters()).thenReturn(new NameValuePair[]{nameValuePair});
        Header header = Mockito.mock(Header.class);
        Mockito.when(header.getElements()).thenReturn(new HeaderElement[]{headerElement});
        Mockito.when(httpEntity.getContentType()).thenReturn(header);

        InputStreamReader inputStreamReader = Mockito.mock(InputStreamReader.class);
        PowerMockito.whenNew(InputStreamReader.class)
                .withParameterTypes(InputStream.class, Charset.class)
                .withArguments(Mockito.eq(inputStream), Mockito.any(Charset.class))
                .thenReturn(inputStreamReader);

        Mockito.when(inputStreamReader.read(Mockito.any(char[].class)))
                .thenAnswer(invocationOnMock -> {
                    char[] charArray = invocationOnMock.getArgumentAt(0, char[].class);
                    System.arraycopy(resultantCharArray, 0, charArray, 0, resultantCharArray.length);
                    return resultantCharArray.length;
                })
                .thenReturn(-1);

        return response;
    }

    @Test
    public void testGetClientId() throws Exception {
        String clientId = "testClientId13";
        String clientSecret = "8nekrnfqbrbeihr";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        Whitebox.setInternalState(oidcOauthManager, "clientId", clientId);
        Whitebox.setInternalState(oidcOauthManager, "clientSecret", clientSecret.toCharArray());

        Assert.assertEquals(oidcOauthManager.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitialization() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        JSONObject credentialsJsonObject = new JSONObject();
        credentialsJsonObject.put(Constants.CLIENT_ID_TXT, clientId);
        credentialsJsonObject.put(Constants.CLIENT_SECRET_TXT, clientSecret);
        PowerMockito.when(oidcOauthManager, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(oidcOauthManager, "createClientWithDcr").thenReturn(credentialsJsonObject);

        Assert.assertEquals(oidcOauthManager.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitializationAndExistingApp() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        JSONObject credentialsJsonObject = new JSONObject();
        credentialsJsonObject.put(Constants.CLIENT_ID_TXT, clientId);
        credentialsJsonObject.put(Constants.CLIENT_SECRET_TXT, clientSecret);
        PowerMockito.when(oidcOauthManager, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(oidcOauthManager, "retrieveExistingClientCredentials")
                .thenReturn(credentialsJsonObject);

        JSONObject errorJsonObject = new JSONObject();
        errorJsonObject.put("error", "client_exists");
        PowerMockito.when(oidcOauthManager, "createClientWithDcr").thenReturn(errorJsonObject);

        Assert.assertEquals(oidcOauthManager.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitializationByMultipleThreads() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        PowerMockito.when(oidcOauthManager, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(oidcOauthManager, "createClientWithDcr").thenAnswer(invocation -> {
            Thread.sleep(1000);
            JSONObject credentialsJsonObject = new JSONObject();
            credentialsJsonObject.put(Constants.CLIENT_ID_TXT, clientId);
            credentialsJsonObject.put(Constants.CLIENT_SECRET_TXT, clientSecret);
            return credentialsJsonObject;
        });

        Thread thread = new Thread(() -> {
            try {
                Assert.assertEquals(oidcOauthManager.getClientId(), clientId);
                Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
                Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                        clientSecret.toCharArray());
            } catch (OIDCProviderException e) {
                Assert.fail("Unexpected failure", e);
            }
        });

        Assert.assertEquals(oidcOauthManager.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
        thread.join();
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecret() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        Whitebox.setInternalState(oidcOauthManager, "clientId", clientId);
        Whitebox.setInternalState(oidcOauthManager, "clientSecret", clientSecret.toCharArray());

        Assert.assertEquals(oidcOauthManager.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitialization() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        JSONObject credentialsJsonObject = new JSONObject();
        credentialsJsonObject.put(Constants.CLIENT_ID_TXT, clientId);
        credentialsJsonObject.put(Constants.CLIENT_SECRET_TXT, clientSecret);
        PowerMockito.when(oidcOauthManager, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(oidcOauthManager, "createClientWithDcr").thenReturn(credentialsJsonObject);

        Assert.assertEquals(oidcOauthManager.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitializationAndExistingApp() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        JSONObject credentialsJsonObject = new JSONObject();
        credentialsJsonObject.put(Constants.CLIENT_ID_TXT, clientId);
        credentialsJsonObject.put(Constants.CLIENT_SECRET_TXT, clientSecret);
        PowerMockito.when(oidcOauthManager, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(oidcOauthManager, "retrieveExistingClientCredentials")
                .thenReturn(credentialsJsonObject);

        JSONObject errorJsonObject = new JSONObject();
        errorJsonObject.put("error", "client_exists");
        PowerMockito.when(oidcOauthManager, "createClientWithDcr").thenReturn(errorJsonObject);

        Assert.assertEquals(oidcOauthManager.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitializationByMultipleThreads() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        OIDCOauthManager oidcOauthManager = mockOidcOAuthManagerWithInitialIdPDown();

        PowerMockito.when(oidcOauthManager, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(oidcOauthManager, "createClientWithDcr").thenAnswer(invocation -> {
            Thread.sleep(1000);
            JSONObject credentialsJsonObject = new JSONObject();
            credentialsJsonObject.put(Constants.CLIENT_ID_TXT, clientId);
            credentialsJsonObject.put(Constants.CLIENT_SECRET_TXT, clientSecret);
            return credentialsJsonObject;
        });

        Thread thread = new Thread(() -> {
            try {
                Assert.assertEquals(oidcOauthManager.getClientSecret(), clientSecret);
                Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
                Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                        clientSecret.toCharArray());
            } catch (OIDCProviderException e) {
                Assert.fail("Unexpected failure", e);
            }
        });

        Assert.assertEquals(oidcOauthManager.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(oidcOauthManager, "clientSecret"),
                clientSecret.toCharArray());
        thread.join();
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(oidcOauthManager, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    /**
     * mock the OIDC OAuth Manager mocking a IdP call failure.
     *
     * @return initialized OIDC OAuth manager
     * @throws Exception initialization fails
     */
    private OIDCOauthManager mockOidcOAuthManagerWithInitialIdPDown() throws Exception {
        OIDCOauthManager oidcOauthManager = PowerMockito.mock(OIDCOauthManager.class);
        Whitebox.setInternalState(oidcOauthManager, "clientId", (String) null);
        Whitebox.setInternalState(oidcOauthManager, "clientSecret", (String) null);

        Mockito.when(oidcOauthManager.getClientId()).thenCallRealMethod();
        Mockito.when(oidcOauthManager.getClientSecret()).thenCallRealMethod();
        return oidcOauthManager;
    }
}
