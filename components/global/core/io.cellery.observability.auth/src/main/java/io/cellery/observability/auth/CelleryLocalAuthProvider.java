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

import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Cellery default local auth provider.
 * This assumes that the user has access to all the namespaces.
 */
public class CelleryLocalAuthProvider implements AuthProvider {
    private static final String LOCAL_RUNTIME_ID = "cellery-default";

    private final KubernetesClient k8sClient;

    public CelleryLocalAuthProvider() {
        k8sClient = new DefaultKubernetesClient();
    }

    @Override
    public Map<String, String[]> getAuthorizedRuntimeNamespaces(String accessToken) {
        // Getting all available namespaces
        String[] namespaces;
        NamespaceList namespaceList = k8sClient.namespaces().list();
        if (namespaceList != null) {
            namespaces = namespaceList.getItems()
                    .stream()
                    .map(namespace -> namespace.getMetadata().getName())
                    .toArray(String[]::new);
        } else {
            namespaces = new String[0];
        }

        Map<String, String[]> authorizedRuntimeNamespaces = new HashMap<>(1);
        authorizedRuntimeNamespaces.put(LOCAL_RUNTIME_ID, namespaces);
        return authorizedRuntimeNamespaces;
    }
}
