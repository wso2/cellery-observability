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

import java.util.Map;

/**
 * Auth Provider interface used for authentication and authorization in Cellery.
 *
 * This can be implemented to override the auth behaviour of Cellery Observability.
 */
public interface AuthProvider {
    // TODO: Move all auth to this module and rethink this interface

    /**
     * Get a map of the authorized run-time namespaces for a user.
     * The keys of the map are runtimes while the value for each runtime contains an array of namespaces
     * the user has access to in the namespaces.
     *
     * @param accessToken The access token sent for the action
     * @return The map of authorized run-time namespaces
     */
    Map<String, String[]> getAuthorizedRuntimeNamespaces(String accessToken);
}
