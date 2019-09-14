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

import io.cellery.observability.k8s.client.crds.Cell;
import io.cellery.observability.k8s.client.crds.CellImpl;
import io.cellery.observability.k8s.client.crds.CellSpec;
import io.cellery.observability.k8s.client.crds.Composite;
import io.cellery.observability.k8s.client.crds.CompositeImpl;
import io.cellery.observability.k8s.client.crds.CompositeSpec;
import io.cellery.observability.k8s.client.crds.model.GRPC;
import io.cellery.observability.k8s.client.crds.model.GatewayTemplate;
import io.cellery.observability.k8s.client.crds.model.GatewayTemplateSpec;
import io.cellery.observability.k8s.client.crds.model.HTTP;
import io.cellery.observability.k8s.client.crds.model.ServicesTemplate;
import io.cellery.observability.k8s.client.crds.model.ServicesTemplateSpec;
import io.cellery.observability.k8s.client.crds.model.TCP;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BaseTestCase {

    protected static final String TEST_LABEL = "mesh-observability-test";

    protected static final String NODE_NAME = "node1";
    protected static final String CREATION_TIMESTAMP_STRING = "2019-04-30T13:21:22Z";
    protected final long creationTimestamp;

    BaseTestCase() throws Exception {
        creationTimestamp = new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US)
                .parse(CREATION_TIMESTAMP_STRING).getTime();
    }

    /**
     * Generate a K8s Cell object.
     * The returned Cell can be used as one of the returned Cells in K8s Mock Server in expectation mode.
     *
     * @param cellName The name of the Cell
     * @param gatewayTemplate The gateway template to be used
     * @param servicesTemplates The list of service templates
     * @return The generated Cell
     */
    protected Cell generateCell(String cellName, GatewayTemplate gatewayTemplate,
                              List<ServicesTemplate> servicesTemplates) {
        CellSpec cellSpec = new CellSpec();
        cellSpec.setGatewayTemplate(gatewayTemplate);
        cellSpec.setServicesTemplates(servicesTemplates);

        CellImpl cell = new CellImpl();
        cell.setMetadata(new ObjectMetaBuilder()
                .withName(cellName)
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .build());
        cell.setSpec(cellSpec);
        return cell;
    }

    /**
     * Generate a K8s Composite object.
     * The returned Composite can be used as one of the returned Composites in K8s Mock Server in expectation mode.
     *
     * @param compositeName The name of the Composite
     * @param servicesTemplates The list of service templates
     * @return The generated Composite
     */
    protected Composite generateComposite(String compositeName,
                                        List<ServicesTemplate> servicesTemplates) {
        CompositeSpec compositeSpec = new CompositeSpec();
        compositeSpec.setServicesTemplates(servicesTemplates);

        CompositeImpl composite = new CompositeImpl();
        composite.setMetadata(new ObjectMetaBuilder()
                .withName(compositeName)
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .build());
        composite.setSpec(compositeSpec);
        return composite;
    }

    /**
     * Generate a K8s Gateway Template Object.
     *
     * @param type The type of Gateway used
     * @param host The host added when used with a Web Ingress
     * @param httpIngresses The HTTP ingresses used by the gateway
     * @param tcpIngresses The TCP ingresses used by the gateway
     * @param grpcIngresses The gRPC ingresses
     * @return The generated Gateway Template
     */
    protected GatewayTemplate generateGatewayTemplate(String type, String host, List<HTTP> httpIngresses,
                                                      List<TCP> tcpIngresses, List<GRPC> grpcIngresses) {
        GatewayTemplateSpec gatewayTemplateSpec = new GatewayTemplateSpec();
        gatewayTemplateSpec.setType(type);
        gatewayTemplateSpec.setHost(host);
        gatewayTemplateSpec.setTcp(tcpIngresses);
        gatewayTemplateSpec.setHttp(httpIngresses);
        gatewayTemplateSpec.setGrpc(grpcIngresses);

        GatewayTemplate gatewayTemplate = new GatewayTemplate();
        gatewayTemplate.setMetadata(new ObjectMetaBuilder()
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .withName("gateway")
                .build());
        gatewayTemplate.setSpec(gatewayTemplateSpec);
        return gatewayTemplate;
    }

    /**
     * Generate a K8s Service Template Object.
     *
     * @param serviceName The name of the service
     * @param protocol    The protocol used by the service
     * @return The generated Service Template
     */
    protected ServicesTemplate generateServicesTemplate(String serviceName, String protocol) {
        ServicesTemplateSpec servicesTemplateSpec = new ServicesTemplateSpec();
        servicesTemplateSpec.setContainer(new ContainerBuilder()
                .withName("test-container")
                .withNewImage("busybox")
                .withNewImagePullPolicy("IfNotPresent")
                .withCommand("tail", "-f", "/dev/null")
                .build());
        servicesTemplateSpec.setProtocol(protocol);
        servicesTemplateSpec.setReplicas(10);
        servicesTemplateSpec.setServiceAccountName("cellery-service-account");
        servicesTemplateSpec.setServicePort(9000);

        ServicesTemplate servicesTemplate = new ServicesTemplate();
        servicesTemplate.setMetadata(new ObjectMetaBuilder()
                .withName(serviceName)
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .build());
        servicesTemplate.setSpec(servicesTemplateSpec);
        return servicesTemplate;
    }

    /**
     * Generate a Cellery Cell Component K8s pod object.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     * @return The generated pod
     */
    protected Pod generateCelleryCellComponentPod(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.COMPONENT_NAME_LABEL, cell + "--" + component);

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("Running")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Cell Gateway K8s pod object.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell The Cell the Pod belongs to
     * @return The generated pod
     */
    protected Pod generateCelleryCellGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.GATEWAY_NAME_LABEL, cell + "--gateway");

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("Running")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Composite Component K8s pod object.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param composite The Composite the Pod belongs to
     * @param component The component of the Composite the pod belongs to
     * @return The generated pod
     */
    protected Pod generateCelleryCompositeComponentPod(String composite, String component) {
        String podName = composite + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.COMPOSITE_NAME_LABEL, composite);
        labels.put(Constants.COMPONENT_NAME_LABEL, composite + "--" + component);

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("Running")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Cell Component K8s pod object with a failing state.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     * @return The generated pod
     */
    protected Pod generateFailingCelleryCellComponentPod(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.COMPONENT_NAME_LABEL, cell + "--" + component);

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("ErrImagePull")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Cell Gateway K8s pod object with a failing state.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell      The Cell the Pod belongs to
     * @return The generated pod
     */
    protected Pod generateFailingCelleryCellGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELL_NAME_LABEL, cell);
        labels.put(Constants.GATEWAY_NAME_LABEL, cell + "--gateway");

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("ErrImagePull")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a Cellery Composite Component K8s pod object with a failing state.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param composite The Composite the Pod belongs to
     * @param component The component of the Composite the pod belongs to
     * @return The generated pod
     */
    protected Pod generateFailingCelleryCompositeComponentPod(String composite, String component) {
        String podName = composite + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.COMPOSITE_NAME_LABEL, composite);
        labels.put(Constants.COMPONENT_NAME_LABEL, composite + "--" + component);

        PodStatus podStatus = new PodStatusBuilder()
                .withPhase("ErrImagePull")
                .build();
        return generatePod(podName, labels, podStatus);
    }

    /**
     * Generate a pod using the provided labels and container with the provided pod name.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param podName   The name of the new pod
     * @param labels    The set of labels to apply
     * @param status    The state
     * @return The generated pod
     */
    private Pod generatePod(String podName, Map<String, String> labels, PodStatus status) {
        labels.put(TEST_LABEL, "true");
        return new PodBuilder()
                .withNewMetadata()
                .withNamespace(Constants.NAMESPACE)
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .withName(podName)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withNodeName(NODE_NAME)
                .addNewContainer()
                .withName("test-container")
                .withNewImage("busybox")
                .withNewImagePullPolicy("IfNotPresent")
                .withCommand("tail", "-f", "/dev/null")
                .endContainer()
                .endSpec()
                .withStatus(status)
                .build();
    }

}
