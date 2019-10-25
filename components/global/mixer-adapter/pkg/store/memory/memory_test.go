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

package memory

import (
	"testing"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

func TestFetchWithDataInBuffer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
		return
	}
	buffer := make(chan string, 10)
	persister := &Persister{
		logger: logger,
		buffer: buffer,
	}
	buffer <- testStr
	_, _, _ = persister.Fetch()
	if len(buffer) != 0 {
		t.Fail()
		return
	}
	t.Log("Test passed")
}

func TestFetchWithoutDataInBuffer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
		return
	}
	buffer := make(chan string, 10)
	persister := &Persister{
		logger: logger,
		buffer: buffer,
	}
	_, _, err = persister.Fetch()
	if err == nil {
		t.Fail()
		return
	}
	t.Log("Test passed")
}

func TestWrite(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	persister := &Persister{
		logger: logger,
		buffer: buffer,
	}
	_ = persister.Write(testStr)
	if len(buffer) == 1 {
		t.Log("Test passed")
		return
	}
	t.Fail()
}

func TestRollback(t *testing.T) {
	buffer := make(chan string, 10)
	transaction := Transaction{
		Element: testStr,
		Buffer:  buffer,
	}
	_ = transaction.Rollback()
	if len(buffer) != 1 {
		t.Fail()
		return
	}
	_ = transaction.Commit()
	t.Log("Test passed")
}

func TestNewPersister(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	_, _ = NewPersister(10, 100, logger)
}
