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

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/logging"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

func TestFetchWithDataInBuffer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	persister := &Persister{
		logger: logger,
		buffer: buffer,
	}
	buffer <- testStr
	str, tx, err := persister.Fetch()
	if len(buffer) != 0 {
		t.Error("Buffer has not been cleaned")
	}
	if str != testStr {
		t.Errorf("Expected string has not been received, expeced : %s, received : %s", testStr, str)
	}
	if err != nil {
		t.Errorf("Unexpected error : %v", err)
	}
	if tx == nil {
		t.Error("Received an empty transaction struct")
	}
}

func TestFetchWithoutDataInBuffer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	persister := &Persister{
		logger: logger,
		buffer: buffer,
	}
	str, _, err := persister.Fetch()
	if err != nil {
		t.Errorf("Unexpected error received : %v", err)
	}
	if str != "" {
		t.Errorf("Expected an empty string, but received : %s", str)
	}
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
	err = persister.Write(testStr)
	if len(buffer) != 1 {
		t.Error("String has not been written to the buffer.")
	}
	if err != nil {
		t.Errorf("Unexpected error received : %v", err)
	}
}

func TestRollback(t *testing.T) {
	buffer := make(chan string, 10)
	transaction := Transaction{
		Element: testStr,
		Buffer:  buffer,
	}
	err := transaction.Rollback()
	if len(buffer) != 1 {
		t.Error("Elements has not been recovered after the rollback")
	}
	if err != nil {
		t.Errorf("Unexpected error received : %v", err)
	}
	err = transaction.Commit()
	if err != nil {
		t.Errorf("Unexpected error received : %v", err)
	}
}

func TestNewPersister(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	persister, err := NewPersister(10, 100, logger)
	if persister == nil {
		t.Errorf("Method returned a null struct")
	}
	if err != nil {
		t.Errorf("Unexpected error received : %v", err)
	}
}
