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
	"fmt"
	"testing"
	"time"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

type (
	MockPersister struct {
		Buffer chan string
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
	_ = <-mockPersister.Buffer
	_ = <-mockPersister.Buffer
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

func TestWriter_Run(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}
	stopCh := make(chan struct{})
	buffer := make(chan string, 10)
	buffer <- testStr
	buffer <- testStr
	writer := Writer{
		WaitingTimeSec: 5,
		WaitingSize:    2,
		Logger:         logger,
		Buffer:         buffer,
		StartTime:      time.Now(),
		Persister:      &MockPersister{Buffer: buffer},
	}
	go writer.Run(stopCh)
	time.Sleep(10 * time.Second)
	close(stopCh)
	time.Sleep(10 * time.Second)

	stopCh2 := make(chan struct{})
	buffer2 := make(chan string, 10)
	buffer2 <- testStr
	buffer2 <- testStr
	writer2 := Writer{
		WaitingTimeSec: 5,
		WaitingSize:    2,
		Logger:         logger,
		Buffer:         buffer2,
		StartTime:      time.Now(),
		Persister:      &MockPersisterErr{},
	}
	go writer2.Run(stopCh2)
	time.Sleep(10 * time.Second)
	close(stopCh2)
	time.Sleep(10 * time.Second)
}
