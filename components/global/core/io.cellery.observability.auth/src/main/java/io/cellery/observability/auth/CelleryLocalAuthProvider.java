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
import io.cellery.observability.auth.internal.ServiceHolder;
import org.apache.commons.lang3.StringUtils;
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
import org.wso2.carbon.datasource.core.exception.DataSourceException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Cellery default local auth provider.
 * This assumes that the user has access to all the namespaces.
 */
public class CelleryLocalAuthProvider implements AuthProvider {
    private static final Logger logger = Logger.getLogger(CelleryLocalAuthProvider.class);
    private static final JsonParser jsonParser = new JsonParser();

    private final String localRuntimeId;
    private final DataSource dataSource;

    private static final String ACTIVE_STATUS = "active";
    private static final String TABLE_NAME = "K8sComponentInfoTable";
    private static final String DATASOURCE_NAME = "CELLERY_OBSERVABILITY_DB";
    private static final List<Action> ALL_ACTIONS = Arrays.asList(
            Action.API_GET,
            Action.DATA_PUBLISH
    );

    public CelleryLocalAuthProvider() throws AuthProviderException {
        try {
            dataSource = (DataSource) ServiceHolder.getDataSourceService().getDataSource(DATASOURCE_NAME);
            localRuntimeId = AuthConfig.getInstance().getDefaultLocalAuthProviderLocalRuntimeId();
        } catch (ConfigurationException | DataSourceException e) {
            throw new AuthProviderException("Failed to initialize Cellery Local Auth Provider", e);
        }
    }

    @Override
    public boolean isTokenValid(String token, Permission requiredPermission) throws AuthProviderException {
        if (StringUtils.isBlank(requiredPermission.getRuntime())
                || Objects.equals(requiredPermission.getRuntime(), localRuntimeId)) {
            List<Action> actions = requiredPermission.getActions();
            if (actions.size() == 1 && Objects.equals(actions.get(0), (Action.DATA_PUBLISH))) {
                boolean isAllowed;
                try {
                    isAllowed = Objects.equals(AuthConfig.getInstance().getDefaultLocalAuthProviderToken(), token);
                } catch (ConfigurationException e) {
                    logger.error("Failed to validate data publish request access token from runtime "
                            + requiredPermission.getRuntime(), e);
                    isAllowed = false;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug((isAllowed ? "Allowing" : "Blocking") + " data publish request from runtime "
                            + requiredPermission.getRuntime() + " since the token does not match the configured "
                            + "data publish token");
                }
                return isAllowed;
            } else {
                boolean isAllowed = this.isTokenValid(token);
                if (logger.isDebugEnabled()) {
                    logger.debug((isAllowed ? "Allowing " : "Blocking ") + requiredPermission.getActions().toString()
                            + " for runtime: " + requiredPermission.getRuntime() + ", namespace: "
                            + requiredPermission.getNamespace() + " since the token is invalid");
                }
                return isAllowed;
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Blocking " + requiredPermission.getActions().toString()
                        + " for runtime: " + requiredPermission.getRuntime() + ", namespace: "
                        + requiredPermission.getNamespace() + " since runtime ID does not match " + localRuntimeId);
            }
            return false;
        }
    }

    @Override
    public Permission[] getAllAllowedPermissions(String accessToken) {
        Permission[] permissions;
        List<String> namespaces;
        try {
            namespaces = this.getAllNamespaces();
        } catch (SQLException e) {
            namespaces = new ArrayList<>(0);
            logger.error("Providing no access to any namespace since failure occurred while getting " +
                    "all the namespaces in " + this.localRuntimeId, e);
        }
        if (namespaces.size() > 0) {
            permissions = namespaces.stream()
                    .map(namespace -> new Permission(localRuntimeId, namespace, ALL_ACTIONS))
                    .toArray(Permission[]::new);
            if (logger.isDebugEnabled()) {
                logger.debug("Providing all actions for all namespaces (" + namespaces.size()
                        + ") from " + localRuntimeId + " runtime as allowed permissions");
            }
        } else {
            permissions = new Permission[0];
            if (logger.isDebugEnabled()) {
                logger.debug("Providing no allowed permissions as no namespaces are present");
            }
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
                logger.error("Failed to validate whether the token is valid with status code " + statusCode);
                return false;
            }
        } catch (IOException | ParseException | NoSuchAlgorithmException | KeyManagementException |
                ConfigurationException e) {
            throw new AuthProviderException("Error occurred while calling the introspect endpoint", e);
        }
        return true;
    }

    /**
     * Get all the namespaces in the tables for the local runtime.
     *
     * @return The list of namespaces in the local runtime ID
     * @throws SQLException If fetching the namespaces list fails
     */
    private List<String> getAllNamespaces() throws SQLException {
        List<String> namespaces = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement("SELECT DISTINCT namespace FROM " +
                    TABLE_NAME + " WHERE runtime = ?");
            statement.setString(1, localRuntimeId);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String runtime = resultSet.getString(1);
                namespaces.add(runtime);
            }
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    logger.error("Error on closing resultSet " + e.getMessage(), e);
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    logger.error("Error on closing statement " + e.getMessage(), e);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error on closing connection " + e.getMessage(), e);
                }
            }
        }
        return namespaces;
    }
}
