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
import io.cellery.observability.auth.exception.AuthProviderException;
import io.cellery.observability.auth.internal.AuthConfig;
import io.cellery.observability.auth.internal.ServiceHolder;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.datasource.core.api.DataSourceService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

@PrepareForTest({AuthUtils.class, CelleryLocalAuthProvider.class, EntityUtils.class})
@PowerMockIgnore({"org.apache.log4j.*", "sun.security.ssl.*"})
public class CelleryLocalAuthProviderTestCase {
    private static final String DATASOURCE_NAME = "CELLERY_OBSERVABILITY_DB";
    private static final String CALLBACK_URL = "http://cellery-dashboard";
    private static final String IDP_URL = "http://idp.cellery-system:9443";
    private static final String IDP_USERNAME = "testadminuser";
    private static final String IDP_PASSWORD = "testadminpass";
    private static final String AUTH_PROVIDER = CelleryLocalAuthProvider.class.getName();

    private Connection mockConnection;
    private Permission mockPermission;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @BeforeClass
    public void initTestCase() {
        mockPermission = Mockito.mock(Permission.class);

        AuthConfig authConfig = new AuthConfig();
        Whitebox.setInternalState(authConfig, "portalHomeUrl", CALLBACK_URL);
        Whitebox.setInternalState(authConfig, "idpUrl", IDP_URL);
        Whitebox.setInternalState(authConfig, "idpUsername", IDP_USERNAME);
        Whitebox.setInternalState(authConfig, "idpPassword", IDP_PASSWORD);
        Whitebox.setInternalState(authConfig, "authProvider", AUTH_PROVIDER);
        Whitebox.setInternalState(AuthConfig.class, "authConfig", authConfig);
    }

    @BeforeMethod
    public void init() throws Exception {
        mockConnection = Mockito.mock(Connection.class);

        DataSource mockDataSource = Mockito.mock(DataSource.class);
        Mockito.when(mockDataSource.getConnection()).thenReturn(mockConnection);

        DataSourceService mockDataSourceService = Mockito.mock(DataSourceService.class);
        Mockito.when(mockDataSourceService.getDataSource(DATASOURCE_NAME)).thenReturn(mockDataSource);
        ServiceHolder.setDataSourceService(mockDataSourceService);
    }

    @AfterMethod
    public void cleanupBaseSiddhiExtensionTestCase() {
        ServiceHolder.setDataSourceService(null);
    }

    @Test
    public void testValidateToken() throws Exception {
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    HttpUriRequest request = invocation.getArgumentAt(0, HttpUriRequest.class);
                    Assert.assertEquals(request.getMethod(), "POST");
                    Assert.assertEquals(request.getURI().getRawPath(),
                            AuthConfig.getInstance().getIdpOidcIntrospectEndpoint());
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(validationHttpClient);
        Mockito.when(AuthUtils.generateBasicAuthHeaderValue(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Assert.assertTrue(celleryLocalAuthProvider.isTokenValid("test token 1", mockPermission));
    }

    @Test
    public void testValidateTokenWithInvalidToken() throws Exception {
        String clientId = "testClientId4";
        String clientSecret = "sdf345fsdf432";
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("active", false);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(validationHttpClient);

        Assert.assertFalse(celleryLocalAuthProvider.isTokenValid("test token 2", mockPermission));
    }

    @Test
    public void testValidateTokenWith1xxStatusCode() throws Exception {
        String clientId = "testClientId5";
        String clientSecret = "4ngdjrk4j432";
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();

        int[] statusCodes = new int[]{100, 102};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        return TestUtils.generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(AuthUtils.class);
            Mockito.when(AuthUtils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(celleryLocalAuthProvider.isTokenValid("test token 3-" + i, mockPermission));
        }
    }

    @Test
    public void testValidateTokenWithSuccessStatusCode() throws Exception {
        String clientId = "testClientId6";
        String clientSecret = "i4nsdfm4fn44";
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();

        int[] statusCodes = new int[]{200, 202, 302, 304};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        responseJson.addProperty("active", false);
                        return TestUtils.generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(AuthUtils.class);
            Mockito.when(AuthUtils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(celleryLocalAuthProvider.isTokenValid("test token 4-" + i, mockPermission));
        }
    }

    @Test
    public void testValidateTokenWithErrorStatusCode() throws Exception {
        String clientId = "testClientId7";
        String clientSecret = "unkjvnfwervsd";
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        HttpClient createClientHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(createClientHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty(Constants.OIDC_CLIENT_ID_KEY, clientId);
                    responseJson.addProperty(Constants.OIDC_CLIENT_SECRET_KEY, clientSecret);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();

        int[] statusCodes = new int[]{400, 404, 500, 502};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        return TestUtils.generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(AuthUtils.class);
            Mockito.when(AuthUtils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(celleryLocalAuthProvider.isTokenValid("test token 5-" + i, mockPermission));
        }
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithHttpRequestThrowingIOException() throws Exception {
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(validationHttpClient);
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();
        celleryLocalAuthProvider.isTokenValid("test token 6", mockPermission);
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithTrustAllClientThrowingKeyManagementException() throws Exception {
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenThrow(new KeyManagementException("Test Exception"));
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();
        celleryLocalAuthProvider.isTokenValid("test token 7", mockPermission);
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithTrustAllClientThrowingNoSuchAlgorithmException() throws Exception {
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenThrow(new NoSuchAlgorithmException("Test Exception"));
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();
        celleryLocalAuthProvider.isTokenValid("test token 8", mockPermission);
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithGetContentThrowingIOException() throws Exception {
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

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

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(getExistingCredentialsHttpClient);
        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();
        celleryLocalAuthProvider.isTokenValid("test token 9", mockPermission);
    }

    @Test(expectedExceptions = AuthProviderException.class)
    public void testValidateTokenWithEntityUtilsThrowingParseException() throws Exception {
        expectAndReturnNamespaces("test-namespace-a", "test-namespace-b");

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

        PowerMockito.mockStatic(AuthUtils.class);
        Mockito.when(AuthUtils.getTrustAllClient())
                .thenReturn(getExistingCredentialsHttpClient);

        CelleryLocalAuthProvider celleryLocalAuthProvider = new CelleryLocalAuthProvider();
        celleryLocalAuthProvider.isTokenValid("test token 10", mockPermission);
    }

    /**
     * Expect a get namespaces call and return namespaces list.
     *
     * @param namespaces The namespaces to return
     */
    private void expectAndReturnNamespaces(String ...namespaces) throws Exception {
        ResultSet mockResultSet = Mockito.mock(ResultSet.class);
        final int[] invocationCount = {0};
        Mockito.when(mockResultSet.next()).then(invocationOnMock -> invocationCount[0] < namespaces.length);
        Mockito.when(mockResultSet.getString(1)).then(invocationOnMock -> {
            String namespace = namespaces[invocationCount[0]];
            invocationCount[0]++;
            return namespace;
        });

        PreparedStatement mockStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        Mockito.when(mockConnection.prepareStatement(Mockito.anyString())).thenReturn(mockStatement);
    }
}
