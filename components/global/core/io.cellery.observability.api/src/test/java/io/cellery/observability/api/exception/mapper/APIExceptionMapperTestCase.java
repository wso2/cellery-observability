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

package io.cellery.observability.api.exception.mapper;

import io.cellery.observability.api.exception.APIInvocationException;
import io.cellery.observability.api.exception.InvalidParamException;
import org.json.JSONObject;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Test Cases for API Exception Mapper.
 */
public class APIExceptionMapperTestCase {

    @Test
    public void testMappingWithInvalidParamException() {
        String paramName = "paramName";
        String paramValue = "paramValue";
        APIExceptionMapper apiExceptionMapper = new APIExceptionMapper();
        Throwable throwable = new InvalidParamException(paramName, paramValue);

        Response response = apiExceptionMapper.toResponse(throwable);
        JSONObject responseBodyJson = new JSONObject(response.getEntity().toString());
        Map<String, List<String>> headersMap = Whitebox.getInternalState(response, "headers");
        List<String> contentTypeHeader = headersMap.get(HttpHeaders.CONTENT_TYPE);

        Assert.assertNotNull(response);
        Assert.assertEquals(responseBodyJson.getString("status"), "Error");
        Assert.assertEquals(responseBodyJson.getString("message"),
                "Invalid parameter " + paramName + " provided. Expected " + paramValue);
        Assert.assertEquals(response.getStatus(), Response.Status.PRECONDITION_FAILED.getStatusCode());
        Assert.assertEquals(contentTypeHeader.size(), 1);
        Assert.assertEquals(contentTypeHeader.get(0), MediaType.APPLICATION_JSON);
    }

    @Test
    public void testMappingWithAPIInvocationException() {
        APIExceptionMapper apiExceptionMapper = new APIExceptionMapper();
        Throwable throwable = new APIInvocationException("Test Exception", new Exception("Test Root Cause"));

        Response response = apiExceptionMapper.toResponse(throwable);
        JSONObject responseBodyJson = new JSONObject(response.getEntity().toString());
        Map<String, List<String>> headersMap = Whitebox.getInternalState(response, "headers");
        List<String> contentTypeHeader = headersMap.get(HttpHeaders.CONTENT_TYPE);

        Assert.assertNotNull(response);
        Assert.assertEquals(responseBodyJson.getString("status"), "Error");
        Assert.assertEquals(responseBodyJson.getString("message"), "Test Exception");
        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        Assert.assertEquals(contentTypeHeader.size(), 1);
        Assert.assertEquals(contentTypeHeader.get(0), MediaType.APPLICATION_JSON);
    }

    @Test
    public void testMappingWithUnknownException() {
        APIExceptionMapper apiExceptionMapper = new APIExceptionMapper();
        Throwable throwable = new IndexOutOfBoundsException("Test Exception");

        Response response = apiExceptionMapper.toResponse(throwable);
        JSONObject responseBodyJson = new JSONObject(response.getEntity().toString());
        Map<String, List<String>> headersMap = Whitebox.getInternalState(response, "headers");
        List<String> contentTypeHeader = headersMap.get(HttpHeaders.CONTENT_TYPE);

        Assert.assertNotNull(response);
        Assert.assertEquals(responseBodyJson.getString("status"), "Error");
        Assert.assertEquals(responseBodyJson.getString("message"), "Unknown Error Occurred");
        Assert.assertEquals(response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        Assert.assertEquals(contentTypeHeader.size(), 1);
        Assert.assertEquals(contentTypeHeader.get(0), MediaType.APPLICATION_JSON);
    }
}
