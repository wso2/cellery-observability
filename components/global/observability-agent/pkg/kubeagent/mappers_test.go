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

package kubeagent

import (
	"fmt"
	"testing"
	"time"

	meshv1alpha2 "cellery.io/cellery-controller/pkg/apis/mesh/v1alpha2"
	meshfake "cellery.io/cellery-controller/pkg/generated/clientset/versioned/fake"
	meshinformers "cellery.io/cellery-controller/pkg/generated/informers/externalversions"
	"cellery.io/cellery-controller/pkg/meta"
	"cellery.io/cellery-controller/pkg/ptr"
	"github.com/google/go-cmp/cmp"
	"github.com/google/go-cmp/cmp/cmpopts"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestMappers(t *testing.T) {
	mf := meshfake.NewSimpleClientset()

	mi := meshinformers.NewSharedInformerFactory(mf, 0)

	cellInformer := mi.Mesh().V1alpha2().Cells()
	compositeInformer := mi.Mesh().V1alpha2().Composites()

	err := cellInformer.Informer().GetIndexer().Add(&meshv1alpha2.Cell{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "some-cell",
			Namespace: "component-namespace",
		},
		Spec: meshv1alpha2.CellSpec{
			Gateway: meshv1alpha2.Gateway{
				Spec: meshv1alpha2.GatewaySpec{
					Ingress: meshv1alpha2.Ingress{
						HTTPRoutes: []meshv1alpha2.HTTPRoute{
							{
								Destination: meshv1alpha2.Destination{
									Host: "component-name",
								},
							},
						},
						GRPCRoutes: []meshv1alpha2.GRPCRoute{
							{
								Destination: meshv1alpha2.Destination{
									Host: "component-name",
								},
							},
						},
						TCPRoutes: []meshv1alpha2.TCPRoute{
							{
								Destination: meshv1alpha2.Destination{
									Host: "component-name",
								},
							},
						},
					},
				},
			},
		},
	})

	if err != nil {
		t.Errorf("Error while adding owner cell to the indexer: %v", err)
		return
	}

	err = compositeInformer.Informer().GetIndexer().Add(&meshv1alpha2.Composite{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "some-composite",
			Namespace: "component-namespace",
		},
	})

	if err != nil {
		t.Errorf("Error while adding owner cell to the indexer: %v", err)
		return
	}

	cellLister := cellInformer.Lister()
	compositeLister := compositeInformer.Lister()

	timeZero := metav1.NewTime(time.Unix(0, 0))
	tests := []struct {
		name     string
		obj      interface{}
		mapperFn MapperFunc
		want     Attributes
		wantErr  error
	}{
		{
			name: "pod with data",
			obj: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Name:              "pod-name",
					Namespace:         "pod-namespace",
					CreationTimestamp: timeZero,
					DeletionTimestamp: &timeZero,
					Labels: map[string]string{
						meta.ObservabilityInstanceLabelKey:     "instance-label-value",
						meta.ObservabilityInstanceKindLabelKey: "instance-kind-label-value",
						meta.ObservabilityComponentLabelKey:    "component-name",
					},
				},
				Spec: corev1.PodSpec{
					NodeName: "node1",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			mapperFn: PodMapper,
			want: Attributes{
				"component":         "component-name",
				"creationTimestamp": int64(0),
				"currentTimestamp":  int64(0),
				"deletionTimestamp": int64(0),
				"instance":          "instance-label-value",
				"instanceKind":      "instance-kind-label-value",
				"name":              "pod-name",
				"namespace":         "pod-namespace",
				"nodeName":          "node1",
				"resourceKind":      "Pod",
				"status":            corev1.PodRunning,
			},
			wantErr: nil,
		},
		{
			name:     "pod mapper with incorrect object",
			obj:      &corev1.Container{},
			mapperFn: PodMapper,
			want:     nil,
			wantErr:  fmt.Errorf("cannot cast *v1.Container to *v1.Pod"),
		},
		{
			name: "component data with cell owner",
			obj: &meshv1alpha2.Component{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "component-name",
					Namespace: "component-namespace",
					OwnerReferences: []metav1.OwnerReference{
						{
							Name:       "some-cell",
							Controller: ptr.Bool(true),
							Kind:       "Cell",
						},
					},
					CreationTimestamp: timeZero,
					DeletionTimestamp: &timeZero,
					Labels: map[string]string{
						meta.ObservabilityInstanceLabelKey:     "instance-label-value",
						meta.ObservabilityInstanceKindLabelKey: "instance-kind-label-value",
						meta.ObservabilityComponentLabelKey:    "component-name",
					},
				},
			},
			mapperFn: ComponentMapper(cellLister, compositeLister),
			want: Attributes{
				"component":         "component-name",
				"creationTimestamp": int64(0),
				"currentTimestamp":  int64(0),
				"deletionTimestamp": int64(0),
				"instance":          "instance-label-value",
				"instanceKind":      "instance-kind-label-value",
				"name":              "component-name",
				"namespace":         "component-namespace",
				"resourceKind":      "Component",
				"ingressTypes":      "HTTP,GRPC,TCP",
			},
			wantErr: nil,
		},
		{
			name: "component data with composite owner",
			obj: &meshv1alpha2.Component{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "component-name",
					Namespace: "component-namespace",
					OwnerReferences: []metav1.OwnerReference{
						{
							Name:       "some-composite",
							Controller: ptr.Bool(true),
							Kind:       "Composite",
						},
					},
					CreationTimestamp: timeZero,
					DeletionTimestamp: &timeZero,
					Labels: map[string]string{
						meta.ObservabilityInstanceLabelKey:     "instance-label-value",
						meta.ObservabilityInstanceKindLabelKey: "instance-kind-label-value",
						meta.ObservabilityComponentLabelKey:    "component-name",
					},
				},
				Spec: meshv1alpha2.ComponentSpec{
					Ports: []meshv1alpha2.PortMapping{
						{
							Protocol: "HTTP",
						},
						{
							Protocol: "GRPC",
						},
						{
							Protocol: "TCP",
						},
					},
				},
			},
			mapperFn: ComponentMapper(cellLister, compositeLister),
			want: Attributes{
				"component":         "component-name",
				"creationTimestamp": int64(0),
				"currentTimestamp":  int64(0),
				"deletionTimestamp": int64(0),
				"instance":          "instance-label-value",
				"instanceKind":      "instance-kind-label-value",
				"name":              "component-name",
				"namespace":         "component-namespace",
				"resourceKind":      "Component",
				"ingressTypes":      "HTTP,GRPC,TCP",
			},
			wantErr: nil,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, gotErr := test.mapperFn(test.obj)
			if test.wantErr != nil {
				if gotErr != nil {
					if diff := cmp.Diff(test.wantErr.Error(), gotErr.Error()); diff != "" {
						t.Errorf("Invalid error (-want, +got)\n%v", diff)
					}
				} else {
					t.Errorf("Expected error %v but got nil", test.wantErr)
				}
			}
			if diff := cmp.Diff(test.want, got, cmpopts.IgnoreMapEntries(ignoreCurrentTimestampAttributeFunc)); diff != "" {
				t.Errorf("Invalid attribute mapping (-want, +got)\n%v", diff)
			}

		})
	}
}

func ignoreCurrentTimestampAttributeFunc(k, v interface{}) bool {
	key, ok := k.(Attribute)
	if !ok {
		return false
	}
	if key == AttributeCurrentTimestamp {
		return true
	}
	return false
}
