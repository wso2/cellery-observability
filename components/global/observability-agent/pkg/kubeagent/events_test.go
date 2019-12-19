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

	"github.com/google/go-cmp/cmp"
)

type fakeObject struct {
	fakeFiled string
}

func TestHandleEvent(t *testing.T) {
	var gotWrite Attributes
	var gotLog string
	tests := []struct {
		name      string
		old       interface{}
		new       interface{}
		event     Event
		wantWrite Attributes
		wantLog   string
	}{
		{
			name:      "add event with empty object",
			old:       nil,
			new:       &fakeObject{},
			event:     EventAdd,
			wantWrite: Attributes{"action": EventAdd, "field": ""},
			wantLog:   "",
		},
		{
			name: "update event with some object with field",
			old: &fakeObject{
				fakeFiled: "some data",
			},
			new: &fakeObject{
				fakeFiled: "some data 1",
			},
			event:     EventUpdate,
			wantWrite: Attributes{"action": EventUpdate, "field": "some data 1"},
			wantLog:   "",
		},
		{
			name:      "delete event with incorrect object type",
			old:       nil,
			new:       fakeObject{},
			event:     EventDelete,
			wantWrite: nil,
			wantLog:   "Fail to handle new event Delete: some error",
		},
	}
	mfn := func(obj interface{}) (Attributes, error) {
		fake, ok := obj.(*fakeObject)
		if !ok {
			return nil, fmt.Errorf("some error")
		}
		attr := Attributes{}
		attr["field"] = fake.fakeFiled
		return attr, nil
	}
	wfn := func(attribute Attributes) {
		gotWrite = attribute
	}
	logf := func(format string, args ...interface{}) {
		gotLog = fmt.Sprintf(format, args...)
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			gotWrite = nil
			gotLog = ""
			HandleEvent(test.event, mfn, wfn, logf)(test.old, test.new)
			if diff := cmp.Diff(test.wantWrite, gotWrite); diff != "" {
				t.Errorf("Invalid write (-want, +got)\n%v", diff)
			}
			if diff := cmp.Diff(test.wantLog, gotLog); diff != "" {
				t.Errorf("Invalid log (-want, +got)\n%v", diff)
			}
		})
	}
}

func TestHandleAll(t *testing.T) {
	var gotWrites []Attributes
	var wantWrites []Attributes
	wantWrites = append(wantWrites, Attributes{"action": EventAdd})
	wantWrites = append(wantWrites, Attributes{"action": EventUpdate, "field": "some data"})
	wantWrites = append(wantWrites, Attributes{"action": EventDelete})
	mfn := func(obj interface{}) (Attributes, error) {
		fakeObj, ok := obj.(*fakeObject)
		if !ok {
			return nil, fmt.Errorf("some error")
		}
		attr := Attributes{}
		if fakeObj.fakeFiled != "" {
			attr["field"] = fakeObj.fakeFiled
		}
		return attr, nil
	}
	wfn := func(attribute Attributes) {
		gotWrites = append(gotWrites, attribute)
	}
	logf := func(format string, args ...interface{}) {}
	HandleAll(mfn, wfn, logf).OnAdd(&fakeObject{})
	HandleAll(mfn, wfn, logf).OnUpdate(&fakeObject{}, &fakeObject{fakeFiled: "some data"})
	HandleAll(mfn, wfn, logf).OnDelete(&fakeObject{})
	if diff := cmp.Diff(wantWrites, gotWrites); diff != "" {
		t.Errorf("Invalid writes (-want, +got)\n%v", diff)
	}
}
