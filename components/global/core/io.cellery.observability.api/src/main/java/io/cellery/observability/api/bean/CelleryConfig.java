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
 * This bean class used to read cellery config.
 */
@Configuration(namespace = "celleryObservabilityPortal", description = "Cellery Configuration Parameters")
public class CelleryConfig {

    private static ConfigProvider configProvider;
    private static CelleryConfig celleryConfig;

    @Element(description = "dashboardURL")
    private String dashboardURL = "";

    @Element(description = "tokenEndpoint")
    private String tokenEndpoint = "";

    @Element(description = "dcrEnpoint")
    private String dcrEnpoint = "";

    @Element(description = "username")
    private String username = "";

    @Element(description = "password")
    private String password = "";

    public String getIntrospectEndpoint() {
        return introspectEndpoint;
    }

    public void setIntrospectEndpoint(String introspectEndpoint) {
        this.introspectEndpoint = introspectEndpoint;
    }

    @Element(description = "introspect-endpoint")
    private String introspectEndpoint = "";

    public CelleryConfig() {

    }

    public CelleryConfig(String dashboardURL, String tokenEndpoint,
                         String dcrEnpoint, String username, String password, String introspectEndpoint) {
        this.dashboardURL = dashboardURL;
        this.tokenEndpoint = tokenEndpoint;
        this.dcrEnpoint = dcrEnpoint;
        this.username = username;
        this.password = password;
        this.introspectEndpoint = introspectEndpoint;
    }

    public String getDashboardURL() {
        return dashboardURL;
    }

    public void setDashboardURL(String dashboardURL) {
        this.dashboardURL = dashboardURL;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getDcrEnpoint() {
        return dcrEnpoint;
    }

    public void setDcrEnpoint(String dcrEnpoint) {
        this.dcrEnpoint = dcrEnpoint;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private static ConfigProvider getConfigProvider() {
        return configProvider;
    }

    public static void setConfigProvider(ConfigProvider configProvider) {
        CelleryConfig.configProvider = configProvider;
    }

    public static synchronized CelleryConfig getInstance() throws ConfigurationException {
        if (celleryConfig == null) {
            celleryConfig = CelleryConfig.getConfigProvider().getConfigurationObject(CelleryConfig.class);
        }
        return celleryConfig;
    }

}
