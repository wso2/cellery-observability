// Copyright 2019 Istio Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package timedfn

import (
	"testing"
	"time"
)

func TestTimeout(t *testing.T) {
	err := WithTimeout(func() {
		time.Sleep(time.Hour * 256)
	}, time.Millisecond*10)

	if err == nil {
		t.Errorf("Expecting timeout, but didn't get one")
	}
}

func TestNoTimeout(t *testing.T) {
	err := WithTimeout(func() {
	}, time.Hour*256)

	if err != nil {
		t.Errorf("Expecting no timeout, but get one: %v", err)
	}
}
