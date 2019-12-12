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
import io.cellery.observability.auth.Permission;
import io.cellery.observability.auth.exception.AuthProviderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;
import org.wso2.msf4j.interceptor.RequestInterceptor;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

/**
 * This class is used for securing backend APIs with Access Token.
 */
public class AuthInterceptor implements RequestInterceptor {
    private static final Logger logger = Logger.getLogger(AuthInterceptor.class);

    public static final Pattern API_URI_PATTERN = Pattern.compile("^/api/runtimes/([^/:?]+)(?:/namespaces/([^/:?]+))?");

    @Override
    public boolean interceptRequest(Request request, Response response) {
        if (!HttpMethod.OPTIONS.equalsIgnoreCase(request.getHttpMethod())) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            Cookie oAuthCookie = request.getHeaders().getCookies().get(Constants.HTTP_ONLY_SESSION_COOKIE);
            if (StringUtils.isNotEmpty(header) && oAuthCookie != null
                    && StringUtils.isNotEmpty(oAuthCookie.getValue())) {
                String accessToken = header.split(" ")[1] + oAuthCookie.getValue();
                request.setProperty(Constants.REQUEST_PROPERTY_ACCESS_TOKEN, accessToken);
                if (!this.isOpenApi(request)) {
                    try {
                        Permission requiredPermission = this.getRequiredPermission(request);
                        if (!ServiceHolder.getAuthProvider().isTokenValid(accessToken, requiredPermission)) {
                            response.setStatus(401);
                            return false;
                        }
                    } catch (AuthProviderException e) {
                        logger.debug("Error occurred while authenticating the access token", e);
                        response.setStatus(401);
                        return false;
                    }
                }
            } else if (!this.isOpenApi(request)) {
                response.setStatus(401);
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a request is going to an Open API.
     * Open APIs can be accessed without proper authentication
     *
     * @param request The request to check
     * @return True if the API can be accessed without authentication
     */
    private boolean isOpenApi(Request request) {
        return StringUtils.isNotEmpty(request.getUri()) && request.getUri().startsWith("/api/auth");
    }

    /**
     * Get the required permission for a given request.
     *
     * @param request The request for which the required permissions should be identified
     * @return The required permission
     */
    private Permission getRequiredPermission(Request request) {
        Matcher matcher = API_URI_PATTERN.matcher(request.getUri());
        String runtime = StringUtils.EMPTY;
        String namespace = StringUtils.EMPTY;
        if (matcher.find()) {
            if (matcher.groupCount() >= 1) {
                runtime = matcher.group(0);
            }
            if (matcher.groupCount() >= 2) {
                namespace = matcher.group(1);
            }
        }
        return new Permission(runtime, namespace, Collections.singletonList(Permission.Action.API_GET));
    }
}
