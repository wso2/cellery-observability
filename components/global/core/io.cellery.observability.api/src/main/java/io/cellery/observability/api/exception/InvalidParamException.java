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

import javax.ws.rs.core.Response;

/**
 * Exception thrown when invalid parameters (Query Params, Path Params, etc. are provided)
 */
public class InvalidParamException extends RuntimeException {
    public InvalidParamException(String parameter, String expected, Object received, Exception e) {
        super("Invalid parameter " + parameter + " provided. Expected " + expected
                + ", received " + received.toString(), e);
    }

    public InvalidParamException(String parameter, String expected, Object received) {
        super("Invalid parameter " + parameter + " provided. Expected " + expected
                + ", received " + received.toString());
    }

    /**
     * Exception mapper for InvalidParamException.
     */
    public static class Mapper extends BaseExceptionMapper<InvalidParamException> {

        public Mapper() {
            super(Response.Status.PRECONDITION_FAILED);
        }

        @Override
        public Response toResponse(InvalidParamException exception) {
            return generateResponse(exception);
        }
    }
}
