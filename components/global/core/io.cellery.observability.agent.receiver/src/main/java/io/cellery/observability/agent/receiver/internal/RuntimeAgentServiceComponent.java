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

package io.cellery.observability.agent.receiver.internal;

import io.cellery.observability.auth.AuthProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is the declarative service component of the observability Runtime Agent component,
 * which is responsible for listening on the required osgi services and exposing the services so that
 * other components can use them.
 */
@Component(
        service = RuntimeAgentServiceComponent.class,
        immediate = true
)
public class RuntimeAgentServiceComponent {

    @Reference(
            name = "io.cellery.observability.auth.AuthProvider",
            service = AuthProvider.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAuthProvider"
    )
    protected void setAuthProvider(AuthProvider authProvider) {
        ServiceHolder.setAuthProvider(authProvider);
    }

    protected void unsetAuthProvider(AuthProvider authProvider) {
        ServiceHolder.setAuthProvider(null);
    }
}
