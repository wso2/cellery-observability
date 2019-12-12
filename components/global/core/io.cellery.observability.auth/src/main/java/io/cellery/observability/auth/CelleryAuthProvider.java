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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cellery.observability.auth.Permission.Action;
import io.cellery.observability.auth.exception.AuthProviderException;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.wso2.carbon.config.ConfigurationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cellery default local auth provider.
 * This assumes that the user has access to all the namespaces.
 */
public class CelleryAuthProvider implements AuthProvider {
    private static final Logger logger = Logger.getLogger(CelleryAuthProvider.class);
    private static final JsonParser jsonParser = new JsonParser();

    private static final String LOCAL_RUNTIME_ID = "cellery-default";
    private static final String ACTIVE_STATUS = "active";

    private static final List<Action> ALL_ACTIONS = Arrays.asList(
            Action.API_GET,
            Action.DATA_PUBLISH
    );

    private final KubernetesClient k8sClient;

    public CelleryAuthProvider() {
        k8sClient = new DefaultKubernetesClient();
    }

    @Override
    public boolean isTokenValid(String token, Permission requiredPermission) throws AuthProviderException {
        // The required permission is ignored as all actions are allowed by default by Cellery Auth Provider
        return this.isTokenValid(token);
    }

    @Override
    public Permission[] getAllAllowedPermissions(String accessToken) {
        // Granting all permissions to all the namespaces in the local runtime
        NamespaceList namespaceList = k8sClient.namespaces().list();
        Permission[] permissions;
        if (namespaceList != null) {
            permissions = namespaceList.getItems()
                    .stream()
                    .map(namespace -> new Permission(LOCAL_RUNTIME_ID, namespace.getMetadata().getName(), ALL_ACTIONS))
                    .toArray(Permission[]::new);
        } else {
            permissions = new Permission[0];
        }
        return permissions;
    }

    /**
     * Validate if a token is a valid token issued by the IdP.
     *
     * @param token The token to be validated
     * @return True if the token is valid
     * @throws AuthProviderException If validating fails
     */
    protected boolean isTokenValid(String token) throws AuthProviderException {
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", token));

            String introspectEP = AuthConfig.getInstance().getIdpUrl() + Constants.OIDC_INTROSPECT_ENDPOINT;
            HttpPost request = new HttpPost(introspectEP);
            request.setHeader(Constants.HEADER_AUTHORIZATION, Utils.generateBasicAuthHeaderValue(
                    AuthConfig.getInstance().getIdpUsername(), AuthConfig.getInstance().getIdpPassword()));
            request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8.name()));

            HttpClient client = Utils.getTrustAllClient();
            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 400) {
                JsonObject jsonObject = jsonParser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                if (!jsonObject.get(ACTIVE_STATUS).getAsBoolean()) {
                    return false;
                }
            } else {
                logger.error("Failed to connect to Introspect endpoint in Identity Provider server." +
                        " Exited with Status Code " + statusCode);
                return false;
            }
        } catch (IOException | ParseException | NoSuchAlgorithmException | KeyManagementException |
                ConfigurationException e) {
            throw new AuthProviderException("Error occurred while calling the introspect endpoint", e);
        }
        return true;
    }
}
