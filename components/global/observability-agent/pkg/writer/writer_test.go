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

package writer

import (
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/cellery-io/mesh-observability/components/global/observability-agent/pkg/store"

	"github.com/cellery-io/mesh-observability/components/global/observability-agent/pkg/logging"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

type (
	MockPersister struct {
		expectedStr string
		Buffer      chan string
		actions     []string
	}
	MockPersisterErr struct{}
	MockTransaction  struct{}
)

func (mockTransaction *MockTransaction) Commit() error {
	return nil
}

func (mockTransaction *MockTransaction) Rollback() error {
	return nil
}

func (mockPersister *MockPersister) Write(str string) error {
	var v interface{}
	if err := json.Unmarshal([]byte(str), &v); err != nil {
		return err
	}
	if mockPersister.expectedStr != str {
		return fmt.Errorf("could not receive the expected string, expected string : %s, received string : %s",
			mockPersister.expectedStr, str)
	}
	mockPersister.actions = append(mockPersister.actions, str)
	return nil
}

func (mockPersister *MockPersister) Fetch() (string, store.Transaction, error) {
	return fmt.Sprintf("[%s]", testStr), &MockTransaction{}, nil
}

func (mockPersisterErr *MockPersisterErr) Write(str string) error {
	return fmt.Errorf("test error 1")
}

func (mockPersisterErr *MockPersisterErr) Fetch() (string, store.Transaction, error) {
	return "", &MockTransaction{}, fmt.Errorf("test error 2")
}

func TestWriteWithoutError(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	buffer <- testStr
	buffer <- testStr
	mockPersister := MockPersister{
		expectedStr: fmt.Sprintf("[%s,%s]", testStr, testStr),
		Buffer:      buffer,
	}
	writer := Writer{
		WaitingTimeSec:  5,
		WaitingSize:     2,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       &mockPersister,
	}
	err = writer.write()
	if err != nil {
		t.Errorf("Received an error while writing : %v", err)
	}
	if len(mockPersister.actions) != 1 {
		t.Errorf("Write function was called unexpected number of times, expected: 1, called: %d",
			len(mockPersister.actions))
	}
}

func TestWriteWithError(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	buffer <- testStr
	buffer <- testStr
	writer := Writer{
		WaitingTimeSec:  5,
		WaitingSize:     2,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       &MockPersisterErr{},
	}
	err = writer.write()
	expectedErr := "test error 1"
	if err == nil {
		t.Errorf("An error was not thrown, but expected : %s", expectedErr)
		return
	}
	if err.Error() != expectedErr {
		t.Errorf("Expected error was not thrown, received error : %v", err)
	}
}

func TestWriteWithEmptyString(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	buffer <- ""
	buffer <- testStr
	mockPersister := MockPersister{
		expectedStr: fmt.Sprintf("[%s]", testStr),
		Buffer:      buffer,
	}
	writer := Writer{
		WaitingTimeSec:  5,
		WaitingSize:     2,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       &mockPersister,
	}
	err = writer.write()
	if err != nil {
		t.Errorf("Unexpected error : %v", err)
	}
	if len(mockPersister.actions) != 1 {
		t.Errorf("Write function was called unexpected number of times, expected: 1, called: %d",
			len(mockPersister.actions))
	}
}

func TestShouldWriteWithFilledBuffer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	buffer <- testStr
	buffer <- testStr
	writer := Writer{
		WaitingTimeSec:  5,
		WaitingSize:     2,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       &MockPersister{},
	}
	result := writer.shouldWrite()
	if !result {
		t.Fail()
	}
}

func TestShouldWriteCheckTimout(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	buffer <- testStr
	writer := &Writer{
		WaitingTimeSec:  2,
		WaitingSize:     2,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       &MockPersister{},
	}
	time.Sleep(3 * time.Second)
	result := writer.shouldWrite()
	if !result {
		t.Fail()
	}
}

func TestShouldWriteWithoutValidConditions(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	buffer <- testStr
	writer := &Writer{
		WaitingTimeSec:  2,
		WaitingSize:     2,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       &MockPersister{},
	}
	result := writer.shouldWrite()
	if result {
		t.Fail()
	}
}

func TestFlushBuffer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	buffer := make(chan string, 10)
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	mockPersister := MockPersister{
		expectedStr: fmt.Sprintf("[%s,%s]", testStr, testStr),
		Buffer:      buffer,
	}
	writer := &Writer{
		WaitingTimeSec:  2,
		WaitingSize:     2,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       &mockPersister,
	}
	writer.flushBuffer()
	if len(buffer) != 0 {
		t.Error("Buffer has not been flushed by the method")
	}
	if len(mockPersister.actions) != 2 {
		t.Errorf("Write function was called unexpected number of times, expected: 2, called: %d",
			len(mockPersister.actions))
	}
}
