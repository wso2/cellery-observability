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

package io.cellery.observability.api.interceptor;

import io.cellery.observability.api.Constants;
import io.cellery.observability.api.internal.ServiceHolder;
import io.cellery.observability.auth.AuthenticationProvider;
import io.cellery.observability.auth.exception.AuthProviderException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

/**
 * Test Cases for Auth Interceptor.
 */
public class AuthInterceptorTestCase {

    private static final String BEARER_PREFIX = "Bearer ";

    @Test
    public void testInterception() throws Exception {
        String tokenFirstPart = "token-first-part";
        String tokenSecondPart = "token-second-part";
        AuthInterceptor authInterceptor = new AuthInterceptor();
        Request request = mockRequest(tokenFirstPart, tokenSecondPart);
        Response response = Mockito.mock(Response.class);

        AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        Mockito.when(authenticationProvider.validateToken(tokenFirstPart + tokenSecondPart)).thenReturn(true);
        ServiceHolder.setAuthenticationProvider(authenticationProvider);

        boolean interceptionResult = authInterceptor.interceptRequest(request, response);
        Assert.assertTrue(interceptionResult);
        Mockito.verify(request, Mockito.times(1))
                .setProperty(Constants.REQUEST_PROPERTY_ACCESS_TOKEN, tokenFirstPart + tokenSecondPart);
        ServiceHolder.setAuthenticationProvider(null);
    }

    @Test
    public void testInterceptionWithInvalidToken() throws Exception {
        String tokenFirstPart = "invalid-token-first-part";
        String tokenSecondPart = "invalid-token-second-part";
        AuthInterceptor authInterceptor = new AuthInterceptor();
        Request request = mockRequest(tokenFirstPart, tokenSecondPart);
        Response response = Mockito.mock(Response.class);

        AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        Mockito.when(authenticationProvider.validateToken(tokenFirstPart + tokenSecondPart)).thenReturn(false);
        ServiceHolder.setAuthenticationProvider(authenticationProvider);

        boolean interceptionResult = authInterceptor.interceptRequest(request, response);
        Assert.assertFalse(interceptionResult);
        Mockito.verify(request, Mockito.times(1))
                .setProperty(Constants.REQUEST_PROPERTY_ACCESS_TOKEN, tokenFirstPart + tokenSecondPart);
        ServiceHolder.setAuthenticationProvider(null);
    }

    @Test
    public void testInterceptionOptionsCall() {
        AuthInterceptor authInterceptor = new AuthInterceptor();
        Request request = Mockito.mock(Request.class);
        Response response = Mockito.mock(Response.class);

        Mockito.when(request.getHttpMethod()).thenReturn(HttpMethod.OPTIONS);

        boolean interceptionResult = authInterceptor.interceptRequest(request, response);
        Assert.assertTrue(interceptionResult);
        Mockito.verify(request, Mockito.times(0))
                .setProperty(Mockito.eq(Constants.REQUEST_PROPERTY_ACCESS_TOKEN), Mockito.any());
    }

    @Test
    public void testInterceptionWithoutAuthorizationHeader() {
        AuthInterceptor authInterceptor = new AuthInterceptor();
        Request request = mockRequest(null, "token-second-part");
        Response response = Mockito.mock(Response.class);

        boolean interceptionResult = authInterceptor.interceptRequest(request, response);
        Assert.assertFalse(interceptionResult);
        Mockito.verify(request, Mockito.times(0))
                .setProperty(Mockito.eq(Constants.REQUEST_PROPERTY_ACCESS_TOKEN), Mockito.any());
    }

    @Test
    public void testInterceptionWithoutOAuthCookie() {
        AuthInterceptor authInterceptor = new AuthInterceptor();
        Request request = Mockito.mock(Request.class);
        Response response = Mockito.mock(Response.class);

        Mockito.when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        Mockito.when(request.getHeaders()).thenReturn(httpHeaders);
        Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_PREFIX + "token-first-part");

        Map<String, Cookie> cookiesMap = new HashMap<>();
        Mockito.when(httpHeaders.getCookies()).thenReturn(cookiesMap);

        boolean interceptionResult = authInterceptor.interceptRequest(request, response);
        Assert.assertFalse(interceptionResult);
        Mockito.verify(request, Mockito.times(0))
                .setProperty(Mockito.eq(Constants.REQUEST_PROPERTY_ACCESS_TOKEN), Mockito.any());
    }

    @Test
    public void testInterceptionWithoutOAuthCookieValue() {
        AuthInterceptor authInterceptor = new AuthInterceptor();
        Request request = mockRequest("token-first-part", null);
        Response response = Mockito.mock(Response.class);

        boolean interceptionResult = authInterceptor.interceptRequest(request, response);
        Assert.assertFalse(interceptionResult);
        Mockito.verify(request, Mockito.times(0))
                .setProperty(Mockito.eq(Constants.REQUEST_PROPERTY_ACCESS_TOKEN), Mockito.any());
    }

    @Test
    public void testInterceptionWithOidcProviderException() throws Exception {
        String tokenFirstPart = "token-first-part";
        String tokenSecondPart = "token-second-part";
        AuthInterceptor authInterceptor = new AuthInterceptor();
        Request request = mockRequest(tokenFirstPart, tokenSecondPart);
        Response response = Mockito.mock(Response.class);

        AuthenticationProvider authenticationProvider = Mockito.mock(AuthenticationProvider.class);
        Mockito.when(authenticationProvider.validateToken(tokenFirstPart + tokenSecondPart))
                .thenThrow(new AuthProviderException("Test Exception"));
        ServiceHolder.setAuthenticationProvider(authenticationProvider);

        boolean interceptionResult = authInterceptor.interceptRequest(request, response);
        Assert.assertFalse(interceptionResult);
        Mockito.verify(request, Mockito.times(1))
                .setProperty(Constants.REQUEST_PROPERTY_ACCESS_TOKEN, tokenFirstPart + tokenSecondPart);
        ServiceHolder.setAuthenticationProvider(null);
    }

    /**
     * Mock a request for the interceptor.
     *
     * @param tokenFirstPart The first part of the partial token
     * @param tokenSecondPart The second part of the partial token
     * @return The mocked request
     */
    private Request mockRequest(String tokenFirstPart, String tokenSecondPart) {
        HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        Map<String, Cookie> cookiesMap = new HashMap<>();
        Cookie oAuthCookie = Mockito.mock(Cookie.class);
        if (tokenSecondPart != null) {
            Mockito.when(oAuthCookie.getValue()).thenReturn(tokenSecondPart);
        }
        cookiesMap.put(Constants.HTTP_ONLY_SESSION_COOKIE, oAuthCookie);
        Mockito.when(httpHeaders.getCookies()).thenReturn(cookiesMap);

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.getHttpMethod()).thenReturn(HttpMethod.GET);
        Mockito.when(request.getHeaders()).thenReturn(httpHeaders);
        if (tokenFirstPart != null) {
            Mockito.when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(BEARER_PREFIX + tokenFirstPart);
        }
        return request;
    }
}
