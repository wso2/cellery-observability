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

import io.cellery.observability.auth.AuthProvider;
import io.cellery.observability.auth.AuthUtils;
import io.cellery.observability.auth.CelleryLocalAuthProvider;
import io.cellery.observability.auth.DcrProvider;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.config.provider.ConfigProvider;

import java.lang.reflect.Constructor;

/**
 * This class acts as a Service Component which specifies the services that is required by the component.
 */
@Component(
        service = AuthServiceComponent.class,
        immediate = true
)
public class AuthServiceComponent {
    private static final Logger logger = Logger.getLogger(AuthServiceComponent.class);

    @Activate
    protected void start(BundleContext bundleContext) throws Exception {
        try {
            AuthUtils.disableSSLVerification();

            DcrProvider dcrProvider = new DcrProvider();
            ServiceHolder.setDcrProvider(dcrProvider);

            Class<?> authProviderClass = Class.forName(AuthConfig.getInstance().getAuthProvider());
            Constructor<?> authProviderConstructor = authProviderClass.getConstructor();
            Object authProviderObject = authProviderConstructor.newInstance();
            AuthProvider authProvider;
            if (authProviderObject instanceof AuthProvider) {
                authProvider = (AuthProvider) authProviderObject;
                logger.info("Using " + authProviderClass.getName() + " as the Auth Provider");
            } else {
                authProvider = new CelleryLocalAuthProvider();
                logger.warn("Using default Cellery Auth Provider since " + authProviderClass.getName()
                        + " is not an instance of " + AuthProvider.class);
            }

            bundleContext.registerService(DcrProvider.class.getName(), dcrProvider, null);
            bundleContext.registerService(AuthProvider.class.getName(), authProvider, null);
        } catch (Throwable throwable) {
            logger.error("Error occurred while activating the model generation bundle", throwable);
            throw throwable;
        }
    }

    @Reference(
            name = "carbon.config.provider",
            service = ConfigProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetConfigProvider"
    )
    protected void setConfigProvider(ConfigProvider configProvider) {
        ServiceHolder.setConfigProvider(configProvider);
    }

    protected void unsetConfigProvider(ConfigProvider configProvider) {
        ServiceHolder.setConfigProvider(null);
    }
}
