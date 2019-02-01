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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.cellery.observability.api.AggregatedRequestsAPI;
import io.cellery.observability.api.exception.APIInvocationException;
import io.cellery.observability.api.exception.InvalidParamException;
import org.apache.log4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Exception Mapper for mapping Server Error Exceptions.
 */
public class APIExceptionMapper implements ExceptionMapper {
    private static final Logger log = Logger.getLogger(AggregatedRequestsAPI.class);
    private Gson gson = new Gson();

    private static final String STATUS = "status";
    private static final String STATUS_ERROR = "Error";
    private static final String MESSAGE = "message";

    @Override
    public Response toResponse(Throwable throwable) {
        Response.Status status;
        String message;

        if (throwable instanceof InvalidParamException) {
            status = Response.Status.PRECONDITION_FAILED;
            message = throwable.getMessage();
        } else if (throwable instanceof APIInvocationException) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            message = throwable.getMessage();
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            message = "Unknown Error Occurred";
        }
        log.error(message, throwable);

        JsonObject errorResponseJsonObject = new JsonObject();
        errorResponseJsonObject.add(STATUS, new JsonPrimitive(STATUS_ERROR));
        errorResponseJsonObject.add(MESSAGE, new JsonPrimitive(message));

        return Response.status(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(gson.toJson(errorResponseJsonObject))
                .build();
    }
}
