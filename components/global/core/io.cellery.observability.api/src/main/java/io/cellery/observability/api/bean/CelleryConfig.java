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
package io.cellery.observability.api.bean;

import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.annotation.Configuration;
import org.wso2.carbon.config.annotation.Element;
import org.wso2.carbon.config.provider.ConfigProvider;

/**
 * This bean class is used to read cellery config.
 */
@Configuration(namespace = "celleryObservabilityPortal", description = "Cellery Configuration Parameters")
public class CelleryConfig {

    private static ConfigProvider configProvider;
    private static volatile CelleryConfig celleryConfig;

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

    private static ConfigProvider getConfigProvider() {
        return configProvider;
    }

    public static void setConfigProvider(ConfigProvider configProvider) {
        CelleryConfig.configProvider = configProvider;
    }

    public static CelleryConfig getInstance() throws ConfigurationException {
        if (celleryConfig == null) {
            celleryConfig = CelleryConfig.getConfigProvider().getConfigurationObject(CelleryConfig.class);
        }
        return celleryConfig;
    }

}
