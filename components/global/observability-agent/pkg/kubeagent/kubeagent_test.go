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
	"testing"
	"time"

	meshv1alpha2 "cellery.io/cellery-controller/pkg/apis/mesh/v1alpha2"
	meshfake "cellery.io/cellery-controller/pkg/generated/clientset/versioned/fake"
	meshinformers "cellery.io/cellery-controller/pkg/generated/informers/externalversions"
	"cellery.io/cellery-controller/pkg/meta"
	"cellery.io/cellery-controller/pkg/ptr"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	kubeinformers "k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/logging"
)

func TestNewResourceWatcher(t *testing.T) {
	kf := kubefake.NewSimpleClientset()
	mf := meshfake.NewSimpleClientset()

	ki := kubeinformers.NewSharedInformerFactory(kf, 0)
	mi := meshinformers.NewSharedInformerFactory(mf, 0)

	buffer := make(chan string, 2) // only need to add two events to test each informer
	logger, _ := logging.NewLogger()

	podInformer := ki.Core().V1().Pods()
	componentInformer := mi.Mesh().V1alpha2().Components()
	cellInformer := mi.Mesh().V1alpha2().Cells()
	compositeInformer := mi.Mesh().V1alpha2().Composites()

	rw := New(podInformer, componentInformer, cellInformer, compositeInformer, buffer, logger)
	if rw == nil {
		t.Errorf("New resource watcher cannot be nil")
		return
	}

	// Start all fake informers and the watcher
	stopCh := make(chan struct{})
	ki.Start(stopCh)
	mi.Start(stopCh)
	err := rw.Run(stopCh)
	if err != nil {
		t.Errorf("Run watcher return with an error: %v", err)
		return
	}

	// Inject events into the fake kube client.
	_, err = kf.CoreV1().Pods("ns1").Create(&corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name: "some-pod",
			Labels: map[string]string{
				meta.ObservabilityInstanceLabelKey: "some-instance",
			},
		},
	})
	if err != nil {
		t.Fatalf("error while creating the pod: %v", err)
	}

	_, err = mf.MeshV1alpha2().Cells("ns1").Create(&meshv1alpha2.Cell{
		ObjectMeta: metav1.ObjectMeta{
			Name: "some-cell",
		},
	})
	if err != nil {
		t.Fatalf("error while creating the cell: %v", err)
	}

	_, err = mf.MeshV1alpha2().Components("ns1").Create(&meshv1alpha2.Component{
		ObjectMeta: metav1.ObjectMeta{
			Name: "some-component",
			OwnerReferences: []metav1.OwnerReference{
				{
					Name:       "some-cell",
					Controller: ptr.Bool(true),
					Kind:       "Cell",
				},
			},
		},
	})
	if err != nil {
		t.Fatalf("error while creating the component: %v", err)
	}

	// wait till the events get added to the buffer
	for i := 0; i < 2; i++ {
		select {
		case data := <-buffer:
			t.Logf("Got data from channel: %s", data)
		case <-time.After(time.Second * 30):
			t.Error("Informer did not receive the required number of events")
			return
		}
	}

}
