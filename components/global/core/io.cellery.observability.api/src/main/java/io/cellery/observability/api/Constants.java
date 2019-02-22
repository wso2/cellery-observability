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
package io.cellery.observability.api;

/**
 * This class defines the constants that are used by the observability API component.
 */
public class Constants {
    private Constants() {
    }

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String MAX_AGE = "3600";

    public static final String ALL_ORIGIN = "*";
    public static final String ORIGIN = "Origin";

    public static final String CLIENT_REGISTERATION_ENDPOINT = "https://gateway.cellery-system:9443" +
            "/client-registration/v0.14/register";
    public static final String CALL_BACK_URL = "callbackUrl";
    public static final String OBSERVABILITY_DASHBOARD_URL = "http://cellery-dashboard";
    public static final String CLIENT_NAME = "clientName";
    public static final String APPLICATION_NAME = "Cellery-Observability-Portal";
    public static final String OWNER = "owner";
    public static final String ADMIN = "admin";
    public static final String GRANT_TYPE = "grantType";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String SAAS_APP = "saasApp";
    public static final String TRUE = "true";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BASIC_ADMIN_AUTH = "Basic YWRtaW46YWRtaW4=";
    public static final String CONTENT_TYPE = "content-type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String INTERNAL_TOKEN_LOCATION = "https://gateway.cellery-system:9443/oauth2/token?";
    public static final String ID_TOKEN = "id_token";

}
