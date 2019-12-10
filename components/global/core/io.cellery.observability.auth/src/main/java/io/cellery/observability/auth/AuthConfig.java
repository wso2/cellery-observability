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

import io.cellery.observability.auth.internal.ServiceHolder;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.annotation.Configuration;
import org.wso2.carbon.config.annotation.Element;

/**
 * This bean class is used to read cellery config.
 */
@Configuration(namespace = "celleryObservabilityPortal", description = "Cellery Configuration Parameters")
public class AuthConfig {

    private static volatile AuthConfig authConfig;

    @Element(description = "dashboardURL")
    private String dashboardURL = "";

    @Element(description = "idpURL")
    private String idpURL = "";

    @Element(description = "idpAdminUsername")
    private String idpAdminUsername = "";

    @Element(description = "idpAdminPassword")
    private String idpAdminPassword = "";

    public String getDashboardURL() {
        return dashboardURL;
    }

    public String getIdpURL() {
        return idpURL;
    }

    public String getIdpAdminUsername() {
        return idpAdminUsername;
    }

    public String getIdpAdminPassword() {
        return idpAdminPassword;
    }

    public static AuthConfig getInstance() throws ConfigurationException {
        if (authConfig == null) {
            authConfig = ServiceHolder.getConfigProvider().getConfigurationObject(AuthConfig.class);
        }
        return authConfig;
    }

}
