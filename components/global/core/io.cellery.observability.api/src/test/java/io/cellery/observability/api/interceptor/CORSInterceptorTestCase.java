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
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

/**
 * Test Cases for CORS interceptor.
 */
public class CORSInterceptorTestCase {

    @Test
    public void testInterception() {
        CORSInterceptor corsInterceptor = new CORSInterceptor();
        Request request = Mockito.mock(Request.class);
        Response response = Mockito.mock(Response.class);

        boolean interceptionResult = corsInterceptor.interceptRequest(request, response);

        Assert.assertTrue(interceptionResult);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.GET + "," + HttpMethod.POST +
                        "," + HttpMethod.PUT + "," + HttpMethod.DELETE);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_MAX_AGE, Constants.MAX_AGE);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_HEADERS,
                        HttpHeaders.CONTENT_TYPE + "," + HttpHeaders.AUTHORIZATION);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_ORIGIN, Constants.ALL_ORIGIN);
    }

    @Test
    public void testInterceptionWithRequestOriginHeader() {
        String origin = "test.example.com";
        CORSInterceptor corsInterceptor = new CORSInterceptor();
        Request request = Mockito.mock(Request.class);
        Response response = Mockito.mock(Response.class);
        Mockito.when(request.getHeader(Constants.ORIGIN)).thenReturn(origin);

        boolean interceptionResult = corsInterceptor.interceptRequest(request, response);

        Assert.assertTrue(interceptionResult);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.GET + "," + HttpMethod.POST +
                        "," + HttpMethod.PUT + "," + HttpMethod.DELETE);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_MAX_AGE, Constants.MAX_AGE);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_HEADERS,
                        HttpHeaders.CONTENT_TYPE + "," + HttpHeaders.AUTHORIZATION);
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        Mockito.verify(response, Mockito.times(1))
                .setHeader(Constants.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
    }
}
