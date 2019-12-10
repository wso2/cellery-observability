/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package kubeagent

import (
	"fmt"
	"strings"

	meshv1alpha2 "cellery.io/cellery-controller/pkg/apis/mesh/v1alpha2"
	meshv1alpha2listers "cellery.io/cellery-controller/pkg/generated/listers/mesh/v1alpha2"
	"cellery.io/cellery-controller/pkg/meta"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type MapperFunc func(obj interface{}) (Attributes, error)

const (
	ingressTypeWeb  = "WEB"
	ingressTypeHTTP = "HTTP"
	ingressTypeGRPC = "GRPC"
	ingressTypeTCP  = "TCP"

	kindCell      = "Cell"
	kindComposite = "Composite"
	kindComponent = "Component"
	kindPod       = "Pod"
)

func PodMapper(obj interface{}) (Attributes, error) {
	pod, ok := obj.(*corev1.Pod)
	if !ok {
		return nil, makeCastError(obj, &corev1.Pod{})
	}
	attr := Attributes{}
	attr[AttributeResourceKind] = kindPod
	addAttributesFromMetadata(attr, pod)
	attr[AttributeInstance] = pod.Labels[meta.ObservabilityInstanceLabelKey]
	attr[AttributeInstanceKind] = pod.Labels[meta.ObservabilityInstanceKindLabelKey]
	attr[AttributeNodeName] = pod.Spec.NodeName
	attr[AttributeStatus] = pod.Status.Phase
	attr[AttributeComponent] = func() string {
		if name, ok := pod.Labels[meta.ObservabilityComponentLabelKey]; ok {
			return name
		}
		if name, ok := pod.Labels[meta.ObservabilityGatewayLabelKey]; ok {
			return name
		}
		return ""
	}()

	return attr, nil
}

func ComponentMapper(cellLister meshv1alpha2listers.CellLister, compositeLister meshv1alpha2listers.CompositeLister) MapperFunc {
	return func(obj interface{}) (Attributes, error) {
		component, ok := obj.(*meshv1alpha2.Component)
		if !ok {
			return nil, makeCastError(obj, &meshv1alpha2.Component{})
		}
		owner := metav1.GetControllerOf(component)
		if owner == nil {
			return nil, fmt.Errorf("component %q does not have an owner", component.Name)
		}

		attr := Attributes{}
		switch owner.Kind {
		case "Cell":
			cell, _ := cellLister.Cells(component.Namespace).Get(owner.Name)
			addAttributesFromCell(attr, component, cell)
		case "Composite":
			composite, _ := compositeLister.Composites(component.Namespace).Get(owner.Name)
			addAttributesFromComposite(attr, component, composite)
		default:
			return nil, fmt.Errorf("component %q has unknown owner %q", component.Name, owner.Kind)
		}
		attr[AttributeResourceKind] = kindComponent
		addAttributesFromMetadata(attr, component)
		return attr, nil
	}
}

func addAttributesFromMetadata(attr Attributes, objMeta metav1.Object) {
	currentTime := metav1.Now()
	creationTime := objMeta.GetCreationTimestamp()
	attr[AttributeName] = objMeta.GetName()
	attr[AttributeNamespace] = objMeta.GetNamespace()
	attr[AttributeCreationTimestamp] = unixTimestamp(&creationTime)
	attr[AttributeDeletionTimestamp] = unixTimestamp(objMeta.GetDeletionTimestamp())
	attr[AttributeCurrentTimestamp] = unixTimestamp(&currentTime)
}

func addAttributesFromCell(attr Attributes, component *meshv1alpha2.Component, cell *meshv1alpha2.Cell) {
	var ingressTypes []string

	attr[AttributeInstance] = component.Labels[meta.ObservabilityInstanceLabelKey]
	attr[AttributeInstanceKind] = component.Labels[meta.ObservabilityInstanceKindLabelKey]

	componentName, ok := component.Labels[meta.ObservabilityComponentLabelKey]
	attr[AttributeComponent] = componentName
	if !ok {
		return
	}
	if cell == nil {
		return
	}
	gatewayIngress := cell.Spec.Gateway.Spec.Ingress
	for _, httpRoute := range gatewayIngress.HTTPRoutes {
		if httpRoute.Destination.Host == componentName {
			if ci := gatewayIngress.IngressExtensions.ClusterIngress; ci != nil {
				if len(ci.Host) > 0 {
					ingressTypes = append(ingressTypes, ingressTypeWeb)
				}
			} else {
				ingressTypes = append(ingressTypes, ingressTypeHTTP)
			}
		}
	}
	for _, grpcRoute := range cell.Spec.Gateway.Spec.Ingress.GRPCRoutes {
		if grpcRoute.Destination.Host == componentName {
			ingressTypes = append(ingressTypes, ingressTypeGRPC)
		}
	}
	for _, tcpRoute := range cell.Spec.Gateway.Spec.Ingress.TCPRoutes {
		if tcpRoute.Destination.Host == componentName {
			ingressTypes = append(ingressTypes, ingressTypeTCP)
		}
	}
	attr[AttributeIngressTypes] = strings.Join(ingressTypes, ",")
}

func addAttributesFromComposite(attr Attributes, component *meshv1alpha2.Component, composite *meshv1alpha2.Composite) {
	var ingressTypes []string

	attr[AttributeInstance] = component.Labels[meta.ObservabilityInstanceLabelKey]
	attr[AttributeInstanceKind] = component.Labels[meta.ObservabilityInstanceKindLabelKey]

	componentName, ok := component.Labels[meta.ObservabilityComponentLabelKey]
	attr[AttributeComponent] = componentName
	if !ok {
		return
	}

	for _, port := range component.Spec.Ports {
		protocol := strings.ToUpper(string(port.Protocol))
		switch protocol {
		case ingressTypeHTTP:
			fallthrough
		case ingressTypeGRPC:
			fallthrough
		case ingressTypeTCP:
			ingressTypes = append(ingressTypes, protocol)
		}
	}
	attr[AttributeIngressTypes] = strings.Join(ingressTypes, ",")
}

func makeCastError(from, to interface{}) error {
	return fmt.Errorf("cannot cast %T to %T", from, to)
}

func unixTimestamp(t *metav1.Time) int64 {
	if t == nil {
		return -1
	}
	return t.Unix()
}
