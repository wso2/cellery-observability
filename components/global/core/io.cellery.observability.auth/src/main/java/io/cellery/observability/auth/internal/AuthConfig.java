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
package io.cellery.observability.auth.internal;

import io.cellery.observability.auth.CelleryLocalAuthProvider;
import io.cellery.observability.auth.exception.AuthProviderException;
import org.apache.commons.lang3.StringUtils;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.annotation.Configuration;
import org.wso2.carbon.config.annotation.Element;

import java.util.Objects;

/**
 * This bean class is used to read cellery auth config.
 */
@Configuration(
        namespace = "cellery.observability.auth",
        description = "Cellery Auth Configuration"
)
public class AuthConfig {

    private static volatile AuthConfig authConfig;

    @Element(description = "idpUrl")
    private String idpUrl = StringUtils.EMPTY;

    @Element(description = "idpUsername")
    private String idpUsername = StringUtils.EMPTY;

    @Element(description = "idpPassword")
    private String idpPassword = StringUtils.EMPTY;

    @Element(description = "portalHomeUrl")
    private String portalHomeUrl = StringUtils.EMPTY;

    @Element(description = "dcrClientId")
    private String dcrClientId = StringUtils.EMPTY;

    @Element(description = "dcrClientName")
    private String dcrClientName = StringUtils.EMPTY;

    @Element(description = "idpDcrRegisterEndpoint")
    private String idpDcrRegisterEndpoint = StringUtils.EMPTY;

    @Element(description = "idpOidcIntrospectEndpoint")
    private String idpOidcIntrospectEndpoint = StringUtils.EMPTY;

    @Element(description = "idpOidcTokenEndpoint")
    private String idpOidcTokenEndpoint = StringUtils.EMPTY;

    @Element(description = "authProvider")
    private String authProvider = CelleryLocalAuthProvider.class.getName();

    @Element(description = "defaultLocalAuthProviderToken")
    private String defaultLocalAuthProviderToken = "";

    public String getIdpUrl() {
        return idpUrl;
    }

    public String getIdpUsername() {
        return idpUsername;
    }

    public String getIdpPassword() {
        return idpPassword;
    }

    public String getPortalHomeUrl() {
        return portalHomeUrl;
    }

    public String getDcrClientId() {
        return dcrClientId;
    }

    public String getDcrClientName() {
        return dcrClientName;
    }

    public String getIdpDcrRegisterEndpoint() {
        return idpDcrRegisterEndpoint;
    }

    public String getIdpOidcIntrospectEndpoint() {
        return idpOidcIntrospectEndpoint;
    }

    public String getIdpOidcTokenEndpoint() {
        return idpOidcTokenEndpoint;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public String getDefaultLocalAuthProviderToken() {
        return defaultLocalAuthProviderToken;
    }

    public static synchronized AuthConfig getInstance() throws ConfigurationException, AuthProviderException {
        if (authConfig == null) {
            authConfig = ServiceHolder.getConfigProvider().getConfigurationObject(AuthConfig.class);
            authConfig.validate();
        }
        return authConfig;
    }

    /**
     * Validate whether the configuration is valid.
     */
    private void validate() throws AuthProviderException {
        if (StringUtils.isEmpty(this.idpUrl)) {
            throw new AuthProviderException("IdP URL provided is empty, expected a proper URL");
        }
        if (StringUtils.isEmpty(this.idpUsername)) {
            throw new AuthProviderException("IdP Username provided is empty, expected a proper username");
        }
        if (StringUtils.isEmpty(this.idpPassword)) {
            throw new AuthProviderException("IdP Password provided is empty, expected a proper password");
        }
        if (StringUtils.isEmpty(this.portalHomeUrl)) {
            throw new AuthProviderException("OIDC callback URL is empty, " +
                    "expected the proper URL used in the auth flows");
        }
        if (StringUtils.isEmpty(this.dcrClientId)) {
            throw new AuthProviderException("DCR Client ID is empty, expected a proper Client ID");
        }
        if (StringUtils.isEmpty(this.dcrClientName)) {
            throw new AuthProviderException("DCR Client Name is empty, expected a proper Client Name");
        }
        if (StringUtils.isEmpty(this.idpDcrRegisterEndpoint)) {
            throw new AuthProviderException("IdP DCR registration endpoint is empty, expected the proper endpoint");
        }
        if (StringUtils.isEmpty(this.idpOidcTokenEndpoint)) {
            throw new AuthProviderException("IdP DCR token endpoint is empty, expected the proper endpoint");
        }
        if (StringUtils.isEmpty(this.idpOidcIntrospectEndpoint)) {
            throw new AuthProviderException("IdP OIDC introspect endpoint is empty, expected the proper endpoint");
        }

        if (Objects.equals(this.authProvider, CelleryLocalAuthProvider.class.getName())) {
            // Validating Cellery Local Auth Provider related configurations
            if (StringUtils.isEmpty(this.defaultLocalAuthProviderToken)) {
                throw new AuthProviderException("Default Local Auth Provider Token is empty, expected a proper token");
            }
        }
    }
}
