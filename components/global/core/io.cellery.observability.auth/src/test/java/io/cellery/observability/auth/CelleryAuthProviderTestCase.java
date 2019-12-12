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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class CelleryAuthProviderTestCase {
    private static final Permission mockPermission = Mockito.mock(Permission.class);

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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();

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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(validationHttpClient);

        Assert.assertTrue(celleryAuthProvider.isTokenValid("test token 1", mockPermission));
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenAnswer(invocation -> {
                    JsonObject responseJson = new JsonObject();
                    responseJson.addProperty("active", false);
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(validationHttpClient);

        Assert.assertFalse(celleryAuthProvider.isTokenValid("test token 2", mockPermission));
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();

        int[] statusCodes = new int[]{100, 102};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        return TestUtils.generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(celleryAuthProvider.isTokenValid("test token 3-" + i, mockPermission));
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();

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
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(celleryAuthProvider.isTokenValid("test token 4-" + i, mockPermission));
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });
        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient);
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();

        int[] statusCodes = new int[]{400, 404, 500, 502};
        for (int i = 0; i < statusCodes.length; i++) {
            int statusCode = statusCodes[i];
            HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
            Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                    .thenAnswer(invocation -> {
                        JsonObject responseJson = new JsonObject();
                        return TestUtils.generateHttpResponse(responseJson, statusCode);
                    });
            PowerMockito.mockStatic(Utils.class);
            Mockito.when(Utils.getTrustAllClient())
                    .thenReturn(validationHttpClient);
            Assert.assertFalse(celleryAuthProvider.isTokenValid("test token 5-" + i, mockPermission));
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        HttpClient validationHttpClient = Mockito.mock(HttpClient.class);
        Mockito.when(validationHttpClient.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new IOException("Test Exception"));

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(validationHttpClient);
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();
        celleryAuthProvider.isTokenValid("test token 6", mockPermission);
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new KeyManagementException("Test Exception"));
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();
        celleryAuthProvider.isTokenValid("test token 7", mockPermission);
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenThrow(new NoSuchAlgorithmException("Test Exception"));
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();
        celleryAuthProvider.isTokenValid("test token 8", mockPermission);
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
                    return TestUtils.generateHttpResponse(responseJson, 200);
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
        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();
        celleryAuthProvider.isTokenValid("test token 9", mockPermission);
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

                    StatusLine statusLine = Mockito.mock(StatusLine.class);
                    Mockito.when(statusLine.getStatusCode()).thenReturn(200);
                    Mockito.when(response.getStatusLine()).thenReturn(statusLine);
                    return response;
                });

        PowerMockito.mockStatic(Utils.class);
        Mockito.when(Utils.getTrustAllClient())
                .thenReturn(createClientHttpClient)
                .thenReturn(getExistingCredentialsHttpClient);

        CelleryAuthProvider celleryAuthProvider = new CelleryAuthProvider();
        celleryAuthProvider.isTokenValid("test token 10", mockPermission);
    }

}
