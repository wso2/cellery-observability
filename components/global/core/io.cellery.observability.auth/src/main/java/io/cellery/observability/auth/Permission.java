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

import java.util.List;

/**
 * Represents a permission granted to perform a list of {@link Action} on any resource
 * in a namespace belonging to a runtime.
 */
public class Permission {
    private String runtime;
    private String namespace;
    private List<Action> actions;

    public Permission(String runtime, String namespace, List<Action> actions) {
        this.runtime = runtime;
        this.namespace = namespace;
        this.actions = actions;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getNamespace() {
        return namespace;
    }

    public List<Action> getActions() {
        return actions;
    }

    /**
     * Action a user can perform on any resource from a particular namespace belonging to a runtime.
     */
    public enum Action {
        // Represents a user making a HTTP GET (Reading data) to the API
        API_GET,

        // Represents a user publishing data
        DATA_PUBLISH
    }
}
