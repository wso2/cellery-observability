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
import io.cellery.observability.k8s.client.crds.cell.CellImpl;
import io.cellery.observability.k8s.client.crds.cell.CellSpec;
import io.cellery.observability.k8s.client.crds.composite.Composite;
import io.cellery.observability.k8s.client.crds.composite.CompositeImpl;
import io.cellery.observability.k8s.client.crds.composite.CompositeSpec;
import io.cellery.observability.k8s.client.crds.gateway.ClusterIngress;
import io.cellery.observability.k8s.client.crds.gateway.Extensions;
import io.cellery.observability.k8s.client.crds.gateway.GRPC;
import io.cellery.observability.k8s.client.crds.gateway.Gateway;
import io.cellery.observability.k8s.client.crds.gateway.GatewaySpec;
import io.cellery.observability.k8s.client.crds.gateway.HTTP;
import io.cellery.observability.k8s.client.crds.gateway.Ingress;
import io.cellery.observability.k8s.client.crds.gateway.TCP;
import io.cellery.observability.k8s.client.crds.service.Component;
import io.cellery.observability.k8s.client.crds.service.ComponentSpec;
import io.cellery.observability.k8s.client.crds.service.Port;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class BaseTestCase {

    private static final String TEST_LABEL = "cellery-observability-test";

    static final String NODE_NAME = "node1";
    private static final String CREATION_TIMESTAMP_STRING = "2019-04-30T13:21:22Z";
    final long creationTimestamp;

    BaseTestCase() throws Exception {
        creationTimestamp = new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US)
                .parse(CREATION_TIMESTAMP_STRING).getTime();
    }

    /**
     * Generate a K8s Cell object.
     * The returned Cell can be used as one of the returned Cells in K8s Mock Server in expectation mode.
     *
     * @param cellName The name of the Cell
     * @param gateway The gateway template to be used
     * @param components The list of service templates
     * @return The generated Cell
     */
    Cell generateCell(String cellName, Gateway gateway, List<Component> components) {
        CellSpec cellSpec = new CellSpec();
        cellSpec.setGateway(gateway);
        cellSpec.setComponents(components);

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
     * @param components The list of service templates
     * @return The generated Composite
     */
    Composite generateComposite(String compositeName, List<Component> components) {
        CompositeSpec compositeSpec = new CompositeSpec();
        compositeSpec.setComponents(components);

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
     * @param host The host added when used with a Web Ingress
     * @param httpIngresses The HTTP ingresses used by the gateway
     * @param tcpIngresses The TCP ingresses used by the gateway
     * @param grpcIngresses The gRPC ingresses
     * @return The generated Gateway Template
     */
    Gateway generateGatewayTemplate(String host, List<HTTP> httpIngresses,
                                    List<TCP> tcpIngresses, List<GRPC> grpcIngresses) {
        ClusterIngress clusterIngress = new ClusterIngress();
        clusterIngress.setHost(host);
        Extensions gatewayExtensions = new Extensions();
        gatewayExtensions.setClusterIngress(clusterIngress);

        Ingress gatewayIngress = new Ingress();
        gatewayIngress.setExtensions(gatewayExtensions);
        gatewayIngress.setTcp(tcpIngresses);
        gatewayIngress.setHttp(httpIngresses);
        gatewayIngress.setGrpc(grpcIngresses);

        GatewaySpec gatewaySpec = new GatewaySpec();
        gatewaySpec.setIngress(gatewayIngress);

        Gateway gateway = new Gateway();
        gateway.setSpec(gatewaySpec);
        return gateway;
    }

    /**
     * Generate a K8s Service Template Object.
     *
     * @param serviceName The name of the service
     * @param protocol    The protocol used by the service
     * @return The generated Service Template
     */
    Component generateServicesTemplate(String serviceName, String protocol) {
        Port port = new Port();
        port.setProtocol(protocol);

        ComponentSpec componentSpec = new ComponentSpec();
        componentSpec.setPorts(Collections.singletonList(port));

        Component component = new Component();
        component.setMetadata(new ObjectMetaBuilder()
                .withName(serviceName)
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .build());
        component.setSpec(componentSpec);
        return component;
    }

    /**
     * Generate a Cellery Cell Component K8s pod object.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cell      The Cell the Pod belongs to
     * @param component The component of the Cell the pod belongs to
     * @return The generated pod
     */
    Pod generateCelleryCellComponentPod(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_LABEL, cell);
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_KIND_LABEL, Constants.CELL_KIND);
        labels.put(Constants.CELLERY_OBSERVABILITY_COMPONENT_NAME_LABEL, component);

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
    Pod generateCelleryCellGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_LABEL, cell);
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_KIND_LABEL, Constants.CELL_KIND);
        labels.put(Constants.CELLERY_OBSERVABILITY_GATEWAY_NAME_LABEL, "gateway");

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
    Pod generateCelleryCompositeComponentPod(String composite, String component) {
        String podName = composite + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_LABEL, composite);
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_KIND_LABEL, Constants.COMPOSITE_KIND);
        labels.put(Constants.CELLERY_OBSERVABILITY_COMPONENT_NAME_LABEL, component);

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
    Pod generateFailingCelleryCellComponentPod(String cell, String component) {
        String podName = cell + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_LABEL, cell);
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_KIND_LABEL, Constants.CELL_KIND);
        labels.put(Constants.CELLERY_OBSERVABILITY_COMPONENT_NAME_LABEL, component);

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
    Pod generateFailingCelleryCellGatewayPod(String cell) {
        String podName = cell + "--gateway";

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_LABEL, cell);
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_KIND_LABEL, Constants.CELL_KIND);
        labels.put(Constants.CELLERY_OBSERVABILITY_COMPONENT_NAME_LABEL, "gateway");

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
    Pod generateFailingCelleryCompositeComponentPod(String composite, String component) {
        String podName = composite + "--" + component;

        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_LABEL, composite);
        labels.put(Constants.CELLERY_OBSERVABILITY_INSTANCE_KIND_LABEL, Constants.COMPOSITE_KIND);
        labels.put(Constants.CELLERY_OBSERVABILITY_COMPONENT_NAME_LABEL, component);

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
