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
	"encoding/json"
	"fmt"

	meshv1alpha2 "cellery.io/cellery-controller/pkg/generated/informers/externalversions/mesh/v1alpha2"
	meshv1alpha2listers "cellery.io/cellery-controller/pkg/generated/listers/mesh/v1alpha2"
	"go.uber.org/zap"
	corev1 "k8s.io/client-go/informers/core/v1"
	"k8s.io/client-go/tools/cache"
)

type ResourceWatcher struct {
	podInformer       corev1.PodInformer
	componentInformer meshv1alpha2.ComponentInformer
	cellLister        meshv1alpha2listers.CellLister
	compositeLister   meshv1alpha2listers.CompositeLister
	buffer            chan string
	logger            *zap.SugaredLogger
}

func New(
	podInformer corev1.PodInformer,
	componentInformer meshv1alpha2.ComponentInformer,
	cellInformer meshv1alpha2.CellInformer,
	compositeInformer meshv1alpha2.CompositeInformer,
	buffer chan string,
	logger *zap.SugaredLogger,
) *ResourceWatcher {
	rw := &ResourceWatcher{
		podInformer:       podInformer,
		componentInformer: componentInformer,
		cellLister:        cellInformer.Lister(),
		compositeLister:   compositeInformer.Lister(),
		buffer:            buffer,
		logger:            logger.Named("kubeagent"),
	}
	rw.podInformer.Informer().AddEventHandler(cache.FilteringResourceEventHandler{
		FilterFunc: FilterWithInstanceLabel(),
		Handler:    HandleAll(PodMapper, rw.write, rw.logger.Named("pod").With().Warnf),
	})
	rw.componentInformer.Informer().AddEventHandler(HandleAll(ComponentMapper(rw.cellLister, rw.compositeLister), rw.write, rw.logger.Named("cell").Warnf))
	return rw
}

func (rw *ResourceWatcher) Run(stopCh <-chan struct{}) error {
	if ok := cache.WaitForCacheSync(stopCh,
		//rw.podInformer.Informer().HasSynced,
		rw.componentInformer.Informer().HasSynced,
	); !ok {
		return fmt.Errorf("failed to wait for caches to sync")
	}
	rw.logger.Info("Resource watcher stared successfully")
	return nil
}

func (rw *ResourceWatcher) write(attr Attributes) {
	jsonBytes, err := json.Marshal(attr)
	if err != nil {
		rw.logger.Errorf("cannot marshal attributes to json: %v", err)
		return
	}
	dataStr := string(jsonBytes)
	rw.logger.Debugf("Writing event to buffer: %s", dataStr)
	rw.buffer <- dataStr
}
