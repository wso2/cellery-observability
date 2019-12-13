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

package io.cellery.observability.api.exception;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.log4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Base exception mapper class capable of generating responses for exceptions.
 *
 * @param <E> The exception to be handled by the mapper
 */
abstract class BaseExceptionMapper<E extends Throwable> implements ExceptionMapper<E> {

    private static final Logger log = Logger.getLogger(BaseExceptionMapper.class);
    private Gson gson = new Gson();

    private static final String STATUS = "status";
    private static final String STATUS_ERROR = "Error";
    private static final String MESSAGE = "message";

    private Response.Status defaultStatus;

    BaseExceptionMapper(Response.Status defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    /**
     * Generate a proper response for an exception.
     *
     * @param exception The exception to be handled
     * @return Proper response for the exception
     */
    Response generateResponse(E exception) {
        log.error("Error in Observability Portal API", exception);

        JsonObject errorResponseJsonObject = new JsonObject();
        errorResponseJsonObject.add(STATUS, new JsonPrimitive(STATUS_ERROR));
        errorResponseJsonObject.add(MESSAGE, new JsonPrimitive(exception.getMessage()));

        return Response.status(defaultStatus)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .entity(gson.toJson(errorResponseJsonObject))
                .build();
    }

}
