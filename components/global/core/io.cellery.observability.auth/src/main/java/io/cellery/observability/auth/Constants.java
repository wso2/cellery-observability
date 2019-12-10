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

/**
 * Constants related to Cellery Auth.
 */
public class Constants {
    public static final String CELLERY_APPLICATION_NAME = "cellery-observability-portal";
    public static final String CELLERY_CLIENT_ID = "celleryobs_0001";

    public static final String OIDC_CLIENT_ID_KEY = "client_id";
    public static final String OIDC_CLIENT_SECRET_KEY = "client_secret";
    public static final String OIDC_AUTHORIZATION_CODE_KEY = "authorization_code";
    public static final String OIDC_CALLBACK_URL_KEY = "redirect_uris";
    public static final String OIDC_CLIENT_NAME_KEY = "client_name";
    public static final String OIDC_GRANT_TYPES_KEY = "grant_types";
    public static final String OIDC_EXT_PARAM_CLIENT_ID_KEY = "ext_param_client_id";

    public static final String OIDC_REGISTER_ENDPOINT = "/api/identity/oauth2/dcr/v1.1/register";
    public static final String OIDC_INTROSPECT_ENDPOINT = "/oauth2/introspect";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    private Constants() {   // Prevent initialization
    }
}
