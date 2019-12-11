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
 * This bean class is used to read cellery auth config.
 */
@Configuration(
        namespace = "cellery.observability.auth",
        description = "Cellery Auth Configuration"
)
public class AuthConfig {

    private static volatile AuthConfig authConfig;

    @Element(description = "callbackUrl")
    private String callbackUrl = "";

    @Element(description = "idpUrl")
    private String idpUrl = "";

    @Element(description = "idpUsername")
    private String idpUsername = "";

    @Element(description = "idpPassword")
    private String idpPassword = "";

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getIdpUrl() {
        return idpUrl;
    }

    public String getIdpUsername() {
        return idpUsername;
    }

    public String getIdpPassword() {
        return idpPassword;
    }

    public static AuthConfig getInstance() throws ConfigurationException {
        if (authConfig == null) {
            authConfig = ServiceHolder.getConfigProvider().getConfigurationObject(AuthConfig.class);
        }
        return authConfig;
    }

}
