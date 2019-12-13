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
import io.cellery.observability.auth.internal.AuthConfig;
import io.cellery.observability.auth.internal.K8sClientHolder;
import io.fabric8.kubernetes.api.model.NamespaceList;
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
import java.util.Objects;

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

    @Override
    public boolean isTokenValid(String token, Permission requiredPermission) throws AuthProviderException {
        // The required permission is ignored as all actions are allowed by default by Cellery Auth Provider
        List<Action> actions = requiredPermission.getActions();
        if (actions.size() == 1 && Objects.equals(actions.get(0), (Action.DATA_PUBLISH))) {
            // Data publishing is unauthorized by default
            if (logger.isDebugEnabled()) {
                logger.debug("Allowing anonymous data publish request from runtime "
                        + requiredPermission.getRuntime());
            }
            return true;
        } else {
            boolean isValid = this.isTokenValid(token);
            if (logger.isDebugEnabled()) {
                logger.debug((isValid ? "Allowing " : "Blocking ") + requiredPermission.getActions().toString()
                        + " for runtime: " + requiredPermission.getRuntime() + ", namespace: "
                        + requiredPermission.getNamespace());
            }
            return isValid;
        }
    }

    @Override
    public Permission[] getAllAllowedPermissions(String accessToken) {
        // Granting all permissions to all the namespaces in the local runtime
        NamespaceList namespaceList = K8sClientHolder.getClient().namespaces().list();
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

            String introspectEP = AuthConfig.getInstance().getIdpUrl()
                    + AuthConfig.getInstance().getIdpOidcIntrospectEndpoint();
            HttpPost request = new HttpPost(introspectEP);
            request.setHeader(Constants.HEADER_AUTHORIZATION, AuthUtils.generateBasicAuthHeaderValue(
                    AuthConfig.getInstance().getIdpUsername(), AuthConfig.getInstance().getIdpPassword()));
            request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8.name()));

            HttpClient client = AuthUtils.getTrustAllClient();
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
