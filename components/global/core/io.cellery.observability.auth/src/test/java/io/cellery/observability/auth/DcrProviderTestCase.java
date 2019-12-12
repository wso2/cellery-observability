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
import io.cellery.observability.auth.internal.AuthConfig;
import io.cellery.observability.auth.internal.ServiceHolder;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
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
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Test Cases for OIDC OAuth Manager
 */
@PrepareForTest({AuthUtils.class, DcrProvider.class, EntityUtils.class})
@PowerMockIgnore("org.apache.log4j.*")
public class DcrProviderTestCase {
    private static final JsonParser jsonParser = new JsonParser();
    private static final String IDP_URL = "http://idp.cellery-system:9443";
    private static final String IDP_USERNAME = "testadminuser";
    private static final String IDP_PASSWORD = "testadminpass";
    private static final String CALLBACK_URL = "http://cellery-dashboard";
    private static final String CLIENT_ID = "celleryobs_0001";
    private static final String CLIENT_NAME = "cellery-observability-portal";
    private static final String AUTH_PROVIDER = CelleryAuthProvider.class.getName();

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @BeforeClass
    public void initTestCase() {
        AuthConfig authConfig = new AuthConfig();
        Whitebox.setInternalState(authConfig, "portalHomeUrl", CALLBACK_URL);
        Whitebox.setInternalState(authConfig, "idpUrl", IDP_URL);
        Whitebox.setInternalState(authConfig, "idpUsername", IDP_USERNAME);
        Whitebox.setInternalState(authConfig, "idpPassword", IDP_PASSWORD);
        Whitebox.setInternalState(authConfig, "dcrClientId", CLIENT_ID);
        Whitebox.setInternalState(authConfig, "dcrClientName", CLIENT_NAME);
        Whitebox.setInternalState(authConfig, "authProvider", AUTH_PROVIDER);
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
                    Assert.assertEquals(request.getURI().getRawPath(),
                            AuthConfig.getInstance().getIdpDcrRegisterEndpoint());
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
                            CALLBACK_URL);
                    Assert.assertEquals(requestBodyJson.get(Constants.OIDC_CLIENT_NAME_KEY).getAsString(),
                            AuthConfig.getInstance().getDcrClientName());
                    Assert.assertEquals(requestBodyJson.get(Constants.OIDC_EXT_PARAM_CLIENT_ID_KEY).getAsString(),
                            AuthConfig.getInstance().getDcrClientId());

                    Assert.assertEquals(request.getFirstHeader(Constants.HEADER_AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");
                    Assert.assertEquals(request.getFirstHeader(Constants.HEADER_CONTENT_TYPE).getValue(),
                            Constants.CONTENT_TYPE_APPLICATION_JSON);

                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient()).thenReturn(httpClient);

        DcrProvider dcrProvider = new DcrProvider();

        Assert.assertEquals(dcrProvider.getClientId(), clientId);
        Assert.assertEquals(dcrProvider.getClientSecret(), clientSecret);
    }

    @Test
    public void testInitializationWithConfigurationException() throws Exception {
        ConfigProvider configProvider = Mockito.mock(ConfigProvider.class);
        Mockito.when(configProvider.getConfigurationObject(AuthConfig.class))
                .thenThrow(new ConfigurationException("Test Exception"));
        ServiceHolder.setConfigProvider(configProvider);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
        ServiceHolder.setConfigProvider(null);
    }

    @Test
    public void testInitializationWithHttpRequestThrowingIOException() throws Exception {
        HttpClient httpClient = Mockito.mock(HttpClient.class);
        Mockito.when(httpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient()).thenReturn(httpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testInitializationWithTrustAllClientThrowingKeyManagementException() throws Exception {
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient()).thenThrow(new KeyManagementException("Test Exception"));

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testInitializationWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient()).thenThrow(new NoSuchAlgorithmException("Test Exception"));

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
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

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient()).thenReturn(httpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
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

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient()).thenReturn(httpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
                    Assert.assertEquals(request.getMethod(), "GET");
                    Assert.assertEquals(request.getURI().getRawPath(),
                            AuthConfig.getInstance().getIdpDcrRegisterEndpoint());
                    Assert.assertEquals(request.getURI().getHost(), "idp.cellery-system");
                    Assert.assertEquals(request.getURI().getPort(), 9443);
                    Assert.assertEquals(request.getURI().getQuery(),
                            Constants.OIDC_CLIENT_NAME_KEY + "=" + AuthConfig.getInstance().getDcrClientName());

                    Assert.assertEquals(request.getFirstHeader(Constants.HEADER_AUTHORIZATION).getValue(),
                            "Basic dGVzdGFkbWludXNlcjp0ZXN0YWRtaW5wYXNz");

                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        DcrProvider dcrProvider = new DcrProvider();

        Assert.assertEquals(dcrProvider.getClientId(), clientId);
        Assert.assertEquals(dcrProvider.getClientSecret(), clientSecret);
    }

    @Test
    public void testGetExistingAppWithErrorStatusCode() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "internal_error");
                    responseJson.addProperty("description", "Internal server error");
                    return TestUtils.generateHttpResponse(responseJson, 500);
                });

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithErrorMessage() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpGet.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "test_error");
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithHttpRequestThrowingIOException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        HttpClient getExistingCredentialsHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(getExistingCredentialsHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithTrustAllClientThrowingKeyManagementException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new KeyManagementException("Test Exception"));

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new NoSuchAlgorithmException("Test Exception"));

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithGetContentThrowingIOException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return TestUtils.generateHttpResponse(responseJson, 200);
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

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testGetExistingAppWithEntityUtilsThrowingParseException() throws Exception {
        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("error", "invalid_client_metadata");
                    responseJson.addProperty("description", "Client already exists");
                    return TestUtils.generateHttpResponse(responseJson, 200);
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

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        DcrProvider dcrProvider = new DcrProvider();
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientId"));
        Assert.assertNull(Whitebox.getInternalState(dcrProvider, "clientSecret"));
    }

    @Test
    public void testGetClientId() throws Exception {
        String clientId = "testClientId13";
        String clientSecret = "8nekrnfqbrbeihr";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        Whitebox.setInternalState(dcrProvider, "clientId", clientId);
        Whitebox.setInternalState(dcrProvider, "clientSecret", clientSecret.toCharArray());

        Assert.assertEquals(dcrProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitialization() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(dcrProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(dcrProvider, "createNewClient").thenReturn(credentialsJsonObject);

        Assert.assertEquals(dcrProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("createNewClient");
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitializationAndExistingApp() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(dcrProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(dcrProvider, "retrieveExistingClientCredentials")
                .thenReturn(credentialsJsonObject);

        JsonObject errorJsonObject = new JsonObject();
        errorJsonObject.addProperty("error", "client_exists");
        PowerMockito.when(dcrProvider, "createNewClient").thenReturn(errorJsonObject);

        Assert.assertEquals(dcrProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("createNewClient");
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientIdWithIdpFailureAtInitializationByMultipleThreads() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        PowerMockito.when(dcrProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(dcrProvider, "createNewClient").thenAnswer(invocation -> {
            Thread.sleep(1000);
            JsonObject credentialsJsonObject = new JsonObject();
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
            return credentialsJsonObject;
        });

        Thread thread = new Thread(() -> {
            try {
                Assert.assertEquals(dcrProvider.getClientId(), clientId);
                Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
                Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                        clientSecret.toCharArray());
            } catch (AuthProviderException | ConfigurationException e) {
                Assert.fail("Unexpected failure", e);
            }
        });

        Assert.assertEquals(dcrProvider.getClientId(), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
        thread.join();
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("createNewClient");
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecret() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        Whitebox.setInternalState(dcrProvider, "clientId", clientId);
        Whitebox.setInternalState(dcrProvider, "clientSecret", clientSecret.toCharArray());

        Assert.assertEquals(dcrProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitialization() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(dcrProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(dcrProvider, "createNewClient").thenReturn(credentialsJsonObject);

        Assert.assertEquals(dcrProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("createNewClient");
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitializationAndExistingApp() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        JsonObject credentialsJsonObject = new JsonObject();
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
        credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
        PowerMockito.when(dcrProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(dcrProvider, "retrieveExistingClientCredentials")
                .thenReturn(credentialsJsonObject);

        JsonObject errorJsonObject = new JsonObject();
        errorJsonObject.addProperty("error", "client_exists");
        PowerMockito.when(dcrProvider, "createNewClient").thenReturn(errorJsonObject);

        Assert.assertEquals(dcrProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("createNewClient");
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("retrieveExistingClientCredentials");
    }

    @Test
    public void testGetClientSecretWithIdpFailureAtInitializationByMultipleThreads() throws Exception {
        String clientId = "testClientId14";
        String clientSecret = "fiun3j4nfjfewf";
        DcrProvider dcrProvider = mockAthenticationProviderWithInitialIdPDown();

        PowerMockito.when(dcrProvider, "retrieveClientCredentials").thenCallRealMethod();
        PowerMockito.when(dcrProvider, "createNewClient").thenAnswer(invocation -> {
            Thread.sleep(1000);
            JsonObject credentialsJsonObject = new JsonObject();
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
            credentialsJsonObject.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
            return credentialsJsonObject;
        });

        Thread thread = new Thread(() -> {
            try {
                Assert.assertEquals(dcrProvider.getClientSecret(), clientSecret);
                Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
                Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                        clientSecret.toCharArray());
            } catch (AuthProviderException | ConfigurationException e) {
                Assert.fail("Unexpected failure", e);
            }
        });

        Assert.assertEquals(dcrProvider.getClientSecret(), clientSecret);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientId"), clientId);
        Assert.assertEquals(Whitebox.getInternalState(dcrProvider, "clientSecret"),
                clientSecret.toCharArray());
        thread.join();
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(1))
                .invoke("createNewClient");
        PowerMockito.verifyPrivate(dcrProvider, Mockito.times(0))
                .invoke("retrieveExistingClientCredentials");
    }

    /**
     * mock the OIDC OAuth Manager mocking a IdP call failure.
     *
     * @return initialized OIDC OAuth manager
     * @throws Exception initialization fails
     */
    private DcrProvider mockAthenticationProviderWithInitialIdPDown() throws Exception {
        DcrProvider dcrProvider = PowerMockito.mock(DcrProvider.class);
        Whitebox.setInternalState(dcrProvider, "clientId", (String) null);
        Whitebox.setInternalState(dcrProvider, "clientSecret", (String) null);

        Mockito.when(dcrProvider.getClientId()).thenCallRealMethod();
        Mockito.when(dcrProvider.getClientSecret()).thenCallRealMethod();
        return dcrProvider;
    }
}
