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

import io.cellery.observability.k8s.client.crds.cell.Cell;
import io.cellery.observability.k8s.client.crds.composite.Composite;
import io.cellery.observability.k8s.client.crds.gateway.GatewayTemplateSpec;
import io.cellery.observability.k8s.client.crds.service.ServicesTemplate;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    static String getComponentName(Pod pod) {
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

    /**
     * Get the map of ingress types in each component in a Cell.
     *
     * @param cell The cell resource from Kubernetes
     * @return The map of ingress types with the component name as the value
     */
    static Map<String, List<String>> getComponentIngressTypes(Cell cell) {
        Map<String, List<String>> componentIngressTypes = new HashMap<>();
        GatewayTemplateSpec gatewaySpec = cell.getSpec().getGatewayTemplate().getSpec();
        for (ServicesTemplate serviceTemplate : cell.getSpec().getServicesTemplates()) {
            List<String> ingressTypes = new ArrayList<>();
            String componentName = serviceTemplate.getMetadata().getName();
            if (gatewaySpec.getHttp() != null) {
                boolean isHttp = gatewaySpec.getHttp().stream()
                        .anyMatch(httpObj -> httpObj.getBackend().equals(componentName));
                if (isHttp) {
                    boolean isWebCell = StringUtils.isNotEmpty(gatewaySpec.getHost());
                    // Check for Web ingresses
                    if (isWebCell) {
                        ingressTypes.add(Constants.IngressType.WEB);
                    } else {
                        ingressTypes.add(Constants.IngressType.HTTP);
                    }
                }
            }
            if (gatewaySpec.getTcp() != null) {
                boolean isTcp = gatewaySpec.getTcp().stream()
                        .anyMatch(tcpObject -> tcpObject.getBackendHost().equals(componentName));
                if (isTcp) {
                    ingressTypes.add(Constants.IngressType.TCP);
                }
            }
            if (gatewaySpec.getGrpc() != null) {
                boolean isGrpc = gatewaySpec.getGrpc().stream().anyMatch(grpcObject -> grpcObject.getBackendHost()
                        .equals(componentName));
                if (isGrpc) {
                    ingressTypes.add(Constants.IngressType.GRPC);
                }
            }
            componentIngressTypes.put(componentName, ingressTypes);
        }
        return componentIngressTypes;
    }

    /**
     * Get the map of ingress types in each component in a Composite.
     *
     * @param composite The composite resource from Kubernetes
     * @return The map of ingress types with the component name as the value
     */
    static Map<String, List<String>> getComponentIngressTypes(Composite composite) {
        Map<String, List<String>> componentIngressTypes = new HashMap<>();
        for (ServicesTemplate serviceTemplate : composite.getSpec().getServicesTemplates()) {
            List<String> ingressTypes = new ArrayList<>();
            String componentName = serviceTemplate.getMetadata().getName();
            String protocol = serviceTemplate.getSpec().getProtocol();
            if (protocol != null) {
                String sanitizedProtocol = protocol.toUpperCase(Locale.US);
                switch (sanitizedProtocol) {
                    case Constants.IngressType.HTTP:
                    case Constants.IngressType.TCP:
                    case Constants.IngressType.GRPC:
                        ingressTypes.add(sanitizedProtocol);
                        break;
                    default:
                }
            }
            componentIngressTypes.put(componentName, ingressTypes);
        }
        return componentIngressTypes;
    }

    private Utils() {   // Prevent initialization
    }
}
