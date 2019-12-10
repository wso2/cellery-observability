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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cellery.observability.auth.exception.AuthProviderException;
import io.cellery.observability.auth.internal.ServiceHolder;
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
@PrepareForTest({Utils.class, AuthConfig.class, AuthenticationProvider.class, EntityUtils.class})
@PowerMockIgnore("org.apache.log4j.*")
public class AuthenticationProviderTestCase {
    private static final JsonParser jsonParser = new JsonParser();
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
        AuthConfig authConfig = new AuthConfig();
        Whitebox.setInternalState(authConfig, "dashboardURL", DASHBOARD_URL);
        Whitebox.setInternalState(authConfig, "idpURL", IDP_URL);
        Whitebox.setInternalState(authConfig, "idpAdminUsername", IDP_ADMIN_USERNAME);
        Whitebox.setInternalState(authConfig, "idpAdminPassword", IDP_ADMIN_PASSWORD);
        Whitebox.setInternalState(AuthConfig.class, "authConfig", authConfig);
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
                    Assert.assertEquals(request.getURI().getRawPath(), Constants.OIDC_REGISTER_ENDPOINT);
                    Assert.assertEquals(request.getURI().getHost(), "idp.cellery-system");
                    Assert.assertEquals(request.getURI().getPort(), 9443);

                    HttpEntity entity = Whitebox.getInternalState(request, "entity");
                    byte[] content = Whitebox.getInternalState(entity, "content");
                    String actualRequestBody = new String(content, StandardCharsets.UTF_8);

                    JsonObject requestBodyJson = jsonParser.parse(actualRequestBody).getAsJsonObject();
                    Assert.assertEquals(requestBodyJson.getAsJsonArray(Constants.OIDC_GRANT_TYPES_KEY).size(), 1);
                    Assert.assertEquals(requestBodyJson.getAsJsonArray(
                            Constants.OIDC_GRANT_TYPES_KEY).get(0).getAsString(),
                            Constants.OIDC_AUTHORIZATION_CODE_KEY);
                    Assert.assertEquals(requestBodyJson.getAsJsonArray(Constants.OIDC_CALLBACK_URL_KEY).size(), 1);
                    Assert.assertEquals(
                            requestBodyJson.getAsJsonArray(Constants.OIDC_CALLBACK_URL_KEY).get(0).getAsString(),
                            DASHBOARD_URL);
                    Assert.assertEquals(requestBodyJson.get(Constants.OIDC_CLIENT_NAME_KEY).getAsString(),
                            Constants.CELLERY_APPLICATION_NAME);
                    Assert.assertEquals(requestBodyJson.get(Constants.OIDC_EXT_PARAM_CLIENT_ID_KEY).getAsString(),
                            Constants.CELLERY_CLIENT_ID);

                    Assert.assertEquals(request.getFirstHeader(Constants.HEADER_AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");
                    Assert.assertEquals(request.getFirstHeader(Constants.HEADER_CONTENT_TYPE).getValue(),
                            Constants.CONTENT_TYPE_APPLICATION_JSON);

                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenReturn(httpClient);

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();

        Assert.assertEquals(authenticationProvider.getClientId(), clientId);
        Assert.assertEquals(authenticationProvider.getClientSecret(), clientSecret);
    }

    @Test
    public void testInitializationWithConfigurationException() throws Exception {
        ConfigProvider configProvider = Mockito.mock(ConfigProvider.class);
        Mockito.when(configProvider.getConfigurationObject(AuthConfig.class))
                .thenThrow(new ConfigurationException("Test Exception"));
        ServiceHolder.setConfigProvider(configProvider);

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
        ServiceHolder.setConfigProvider(null);
    }

    @Test
    public void testInitializationWithHttpRequestThrowingIOException() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenReturn(httpClient);

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testInitializationWithTrustAllClientThrowingKeyManagementException() throws Exception {
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenThrow(new KeyManagementException("Test Exception"));

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testInitializationWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient()).thenThrow(new NoSuchAlgorithmException("Test Exception"));

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
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

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
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

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingApp() throws Exception {
        String clientId = "testClientId2";
        String clientSecret = "34fdnijn4rs";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
                    Assert.assertEquals(request.getMethod(), "GET");
                    Assert.assertEquals(request.getURI().getRawPath(), Constants.OIDC_REGISTER_ENDPOINT);
                    Assert.assertEquals(request.getURI().getHost(), "idp.cellery-system");
                    Assert.assertEquals(request.getURI().getPort(), 9443);
                    Assert.assertEquals(request.getURI().getQuery(),
                            Constants.OIDC_CLIENT_NAME_KEY + "=" + Constants.CELLERY_APPLICATION_NAME);

                    Assert.assertEquals(request.getFirstHeader(Constants.HEADER_AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");

                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();

        Assert.assertEquals(authenticationProvider.getClientId(), clientId);
        Assert.assertEquals(authenticationProvider.getClientSecret(), clientSecret);
    }

    @Test
    public void testGetExistingAppWithErrorStatusCode() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "internal_error");
                    responseJson.addProperty("description", "Internal server error");
                    return generateHttpResponse(responseJson, 500);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithErrorMessage() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "test_error");
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithHttpRequestThrowingIOException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithTrustAllClientThrowingKeyManagementException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new KeyManagementException("Test Exception"));

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new NoSuchAlgorithmException("Test Exception"));

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithGetContentThrowingIOException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
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

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithEntityUtilsThrowingParseException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
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

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(authenticationProvider, "clientSecret"));
    }

    @Test
    public void testValidateToken() throws Exception {
        String clientId = "testClientId3";
        String clientSecret = "ksdfkwnrb32";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
                    Assert.assertEquals(request.getMethod(), "POST");
                    Assert.assertEquals(request.getURI().getRawPath(), Constants.OIDC_INTROSPECT_ENDPOINT);
                    Assert.assertEquals(request.getURI().getHost(), "idp.cellery-system");
                    Assert.assertEquals(request.getURI().getPort(), 9443);

                    HttpEntity entity = Whitebox.getInternalState(request, "entity");
                    byte[] content = Whitebox.getInternalState(entity, "content");
                    String actualRequestBody = new String(content, StandardCharsets.UTF_8);
                    Assert.assertEquals(actualRequestBody, "token=test+token+1");

                    Assert.assertEquals(request.getFirstHeader(Constants.HEADER_AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");

                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("active", true);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(validationHttpClient);

        Assert.assertTrue(authenticationProvider.validateToken("test token 1"));
    }

    @Test
    public void testValidateTokenWithInvalidToken() throws Exception {
        String clientId = "testClientId4";
        String clientSecret = "sdf345fsdf432";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("active", false);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(validationHttpClient);

        Assert.assertFalse(authenticationProvider.validateToken("test token 2"));
    }

    @Test
    public void testValidateTokenWith1xxStatusCode() throws Exception {
        String clientId = "testClientId5";
        String clientSecret = "4ngdjrk4j432";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();

        int[] statusCodes = new int[]{100, 102};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        return generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(authenticationProvider.validateToken("test token 3-" + i));
        }
    }

    @Test
    public void testValidateTokenWithSuccessStatusCode() throws Exception {
        String clientId = "testClientId6";
        String clientSecret = "i4nsdfm4fn44";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();

        int[] statusCodes = new int[]{200, 202, 302, 304};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        responseJson.addProperty("active", false);
                        return generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(authenticationProvider.validateToken("test token 4-" + i));
        }
    }

    @Test
    public void testValidateTokenWithErrorStatusCode() throws Exception {
        String clientId = "testClientId7";
        String clientSecret = "unkjvnfwervsd";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();

        int[] statusCodes = new int[]{400, 404, 500, 502};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        return generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(authenticationProvider.validateToken("test token 5-" + i));
        }
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithHttpRequestThrowingIOException() throws Exception {
        String clientId = "testClientId8";
        String clientSecret = "m34uniscrf4rv";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(validationHttpClient);
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        authenticationProvider.validateToken("test token 6");
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithTrustAllClientThrowingKeyManagementException() throws Exception {
        String clientId = "testClientId9";
        String clientSecret = "4unifhbs4hbr4";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new KeyManagementException("Test Exception"));
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        authenticationProvider.validateToken("test token 7");
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        String clientId = "testClientId10";
        String clientSecret = "niu43nfhref4f4";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new NoSuchAlgorithmException("Test Exception"));
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        authenticationProvider.validateToken("test token 8");
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithGetContentThrowingIOException() throws Exception {
        String clientId = "testClientId11";
        String clientSecret = "452nkjnkjbr44r";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
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
        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        authenticationProvider.validateToken("test token 9");
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithEntityUtilsThrowingParseException() throws Exception {
        String clientId = "testClientId12";
        String clientSecret = "0345ihver43r43";

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
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

        AuthenticationProvider authenticationProvider = new AuthenticationProvider();
        authenticationProvider.validateToken("test token 10");
    }

    /**
     * Generate a mock Http Response object that would return the string of the JSON object provided.
     *
     * @param jsonObject The JSON object to be returned from the HTTP response
     * @param statusCode The status code of the response
     * @return The HTTP response
     * @throws Exception if mocking fails
     */
    private HttpResponse generateHttpResponse(JsonObject jsonObject, int statusCode) throws Exception {
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
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        Whitebox.setInternalState(authenticationProvider, "clientId", clientId);
        Whitebox.setInternalState(authenticationProvider, "clientSecret", clientSecret.toCharArray());

        Assert.assertEquals(authenticationProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitialization() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(authenticationProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(authenticationProvider, "createClientWithDcr").thenReturn(credentialsJsonObject);

        Assert.assertEquals(authenticationProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitializationAndExistingApp() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(authenticationProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(authenticationProvider, "retrieveExistingClientCredentials")
                .thenReturn(credentialsJsonObject);

        JsonObject errorJsonObject = new JsonObject();
        errorJsonObject.addProperty("error", "client_exists");
        PowerMockito.when(authenticationProvider, "createClientWithDcr").thenReturn(errorJsonObject);

        Assert.assertEquals(authenticationProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitializationByMultipleThreads() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        PowerMockito.when(authenticationProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(authenticationProvider, "createClientWithDcr").thenAnswer(invocation -> {
            Thread.sleep(1000);
            JsonObject credentialsJsonObject = new JsonObject();
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
            return credentialsJsonObject;
        });

        Thread thread = new Thread(() -> {
            try {
                Assert.assertEquals(authenticationProvider.getClientId(), clientId);
                Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
                Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                        clientSecret.toCharArray());
            } catch (AuthProviderException e) {
                Assert.fail("Unexpected failure", e);
            }
        });

        Assert.assertEquals(authenticationProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
        thread.join();
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecret() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        Whitebox.setInternalState(authenticationProvider, "clientId", clientId);
        Whitebox.setInternalState(authenticationProvider, "clientSecret", clientSecret.toCharArray());

        Assert.assertEquals(authenticationProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitialization() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(authenticationProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(authenticationProvider, "createClientWithDcr").thenReturn(credentialsJsonObject);

        Assert.assertEquals(authenticationProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitializationAndExistingApp() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(authenticationProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(authenticationProvider, "retrieveExistingClientCredentials")
                .thenReturn(credentialsJsonObject);

        JsonObject errorJsonObject = new JsonObject();
        errorJsonObject.addProperty("error", "client_exists");
        PowerMockito.when(authenticationProvider, "createClientWithDcr").thenReturn(errorJsonObject);

        Assert.assertEquals(authenticationProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitializationByMultipleThreads() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        AuthenticationProvider authenticationProvider = mockAthenticationProviderWithInitialIdPDown();

        PowerMockito.when(authenticationProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(authenticationProvider, "createClientWithDcr").thenAnswer(invocation -> {
            Thread.sleep(1000);
            JsonObject credentialsJsonObject = new JsonObject();
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
            return credentialsJsonObject;
        });

        Thread thread = new Thread(() -> {
            try {
                Assert.assertEquals(authenticationProvider.getClientSecret(), clientSecret);
                Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
                Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                        clientSecret.toCharArray());
            } catch (AuthProviderException e) {
                Assert.fail("Unexpected failure", e);
            }
        });

        Assert.assertEquals(authenticationProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(authenticationProvider, "clientSecret"),
                clientSecret.toCharArray());
        thread.join();
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(1))
                .invoke("createClientWithDcr");
        PowerMockito.verifyPrivate(authenticationProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    /**
     * mock the OIDC OAuth Manager mocking a IdP call failure.
     *
     * @return initialized OIDC OAuth manager
     * @throws Exception initialization fails
     */
    private AuthenticationProvider mockAthenticationProviderWithInitialIdPDown() throws Exception {
        AuthenticationProvider authenticationProvider = PowerMockito.mock(AuthenticationProvider.class);
        Whitebox.setInternalState(authenticationProvider, "clientId", (String) null);
        Whitebox.setInternalState(authenticationProvider, "clientSecret", (String) null);

        Mockito.when(authenticationProvider.getClientId()).thenCallRealMethod();
        Mockito.when(authenticationProvider.getClientSecret()).thenCallRealMethod();
        return authenticationProvider;
    }
}
