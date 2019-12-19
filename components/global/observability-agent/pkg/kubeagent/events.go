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
	"github.com/google/go-cmp/cmp"
	"github.com/google/go-cmp/cmp/cmpopts"
	"k8s.io/client-go/tools/cache"
)

type Event string

const (
	EventAdd    Event = "Add"
	EventUpdate Event = "Update"
	EventDelete Event = "Delete"
)

type WriterFunc func(attribute Attributes)
type LoggerFunc func(format string, args ...interface{})

func HandleAll(mfn MapperFunc, wfn WriterFunc, logf LoggerFunc) cache.ResourceEventHandler {
	return cache.ResourceEventHandlerFuncs{
		AddFunc:    PassNew(HandleEvent(EventAdd, mfn, wfn, logf)),
		UpdateFunc: HandleEvent(EventUpdate, mfn, wfn, logf),
		DeleteFunc: PassNew(HandleEvent(EventDelete, mfn, wfn, logf)),
	}
}

func HandleEvent(e Event, mfn MapperFunc, wfn WriterFunc, logf LoggerFunc) func(interface{}, interface{}) {
	return func(old interface{}, new interface{}) {
		newAttr, err := mfn(new)
		if err != nil {
			logf("Fail to handle new event %s: %v", e, err)
			return
		}
		var oldAttr Attributes
		if old != nil {
			attr, err := mfn(old)
			if err != nil {
				logf("Fail to handle old event %s: %v", e, err)
				return
			}
			oldAttr = attr
		}
		if diff := cmp.Diff(newAttr, oldAttr, cmpopts.IgnoreMapEntries(ignoreCurrentTimestampAttributeFunc)); old == nil || diff != "" {
			newAttr[AttributeAction] = e
			wfn(newAttr)
		}
	}
}

func PassNew(f func(interface{}, interface{})) func(interface{}) {
	return func(new interface{}) {
		f(nil, new)
	}
}
