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

import io.cellery.observability.auth.AuthorizationProvider;
import io.cellery.observability.auth.CelleryLocalAuthProvider;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * This class acts as a Service Component which specifies the services that is required by the component.
 */
@Component(
        service = AuthServiceComponent.class,
        immediate = true
)
public class AuthServiceComponent {
    private static final Logger log = Logger.getLogger(AuthServiceComponent.class);

    @Activate
    protected void start(BundleContext bundleContext) {
        try {
            bundleContext.registerService(AuthorizationProvider.class.getName(), new CelleryLocalAuthProvider(), null);
        } catch (Throwable throwable) {
            log.error("Error occurred while activating the model generation bundle", throwable);
            throw throwable;
        }
    }
}
