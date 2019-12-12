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

import io.cellery.observability.auth.exception.AuthProviderException;

/**
 * Auth Provider interface used for authentication and authorization in Cellery.
 *
 * This can be implemented to override the auth behaviour of Cellery Observability.
 */
public interface AuthProvider {

    /**
     * Validate the token.
     *
     * @param token The token of which the validity should be checked
     * @param requiredPermission The permission required by the user
     * @return True if the token is valid
     * @throws AuthProviderException if validating the token fails
     */
    boolean isTokenValid(String token, Permission requiredPermission) throws AuthProviderException;

    /**
     * Get an array of all the permissions allowed for a user.
     *
     * @param accessToken The access token sent for the action
     * @return The map of authorized runtime namespaces
     * @throws AuthProviderException if checking the authorized runtime namespaces failed
     */
    Permission[] getAllAllowedPermissions(String accessToken) throws AuthProviderException;
}
