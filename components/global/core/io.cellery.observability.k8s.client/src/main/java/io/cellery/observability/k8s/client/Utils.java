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

package io.cellery.observability.k8s.client;

import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Cellery Utilities.
 */
public class Utils {

    /**
     * Get the actual component name of a pod.
     *
     * @param pod The kubernetes pod of which the component name should be retrieved
     * @return The actual component name
     */
    public static String getComponentName(Pod pod) {
        String fullyQualifiedName = null;
        Map<String, String> labels = pod.getMetadata().getLabels();
        if (labels.containsKey(Constants.COMPONENT_NAME_LABEL)) {
            fullyQualifiedName = labels.get(Constants.COMPONENT_NAME_LABEL);
        } else if (labels.containsKey(Constants.GATEWAY_NAME_LABEL)) {
            fullyQualifiedName = labels.get(Constants.GATEWAY_NAME_LABEL);
        }

        String componentName;
        if (StringUtils.isNotEmpty(fullyQualifiedName) && fullyQualifiedName.contains("--")) {
            componentName = fullyQualifiedName.split("--")[1];
        } else {
            componentName = "";
        }
        return componentName;
    }

    private Utils() {   // Prevent initialization
    }
}
