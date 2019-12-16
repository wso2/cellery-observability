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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;
import org.wso2.msf4j.interceptor.RequestInterceptor;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

/**
 * Used for applying header for allowing cross origin requests.
 * <p>
 * This alone is not enough since the browsers send an OPTIONS HTTP call to the endpoint first to check
 * which methods are allowed. Therefore an endpoint with HTTP Method OPTIONS should be added to all services.
 */
public class CORSInterceptor implements RequestInterceptor {
    private static final Logger logger = Logger.getLogger(CORSInterceptor.class);

    @Override
    public boolean interceptRequest(Request request, Response response) {
        response.setHeader(Constants.HEADER_ACCESS_CONTROL_ALLOW_METHODS, HttpMethod.GET + "," + HttpMethod.POST +
                "," + HttpMethod.PUT + "," + HttpMethod.DELETE);
        response.setHeader(Constants.HEADER_ACCESS_CONTROL_MAX_AGE, Constants.MAX_AGE);
        response.setHeader(Constants.HEADER_ACCESS_CONTROL_ALLOW_HEADERS,
                HttpHeaders.CONTENT_TYPE + "," + HttpHeaders.AUTHORIZATION);
        response.setHeader(Constants.HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        if (StringUtils.isNotBlank(request.getHeader(Constants.HEADER_ORIGIN))) {
            response.setHeader(Constants.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                    request.getHeader(Constants.HEADER_ORIGIN));
        } else {
            response.setHeader(Constants.HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, Constants.ALL_ORIGINS);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Attached CORS Headers to API Call " + request.getHttpMethod() + " " + request.getUri());
        }
        return true;
    }
}
