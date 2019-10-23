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

package publisher

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49." +
		"istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", " +
		"\"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", " +
		"\"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--" +
		"controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":" +
		"\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

type (
	RoundTripFunc      func(req *http.Request) *http.Response
	MockPersister      struct{}
	MockPersisterError struct{}
	MockTransaction    struct{}
)

func (f RoundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return f(req), nil
}

func NewTestClient(fn RoundTripFunc) *http.Client {
	return &http.Client{
		Transport: fn,
	}
}

func (mockTransaction *MockTransaction) Commit() error {
	return nil
}

func (mockTransaction *MockTransaction) Rollback() error {
	return nil
}

func (mockPersister *MockPersister) Write(str string) error {
	return nil
}
func (mockPersister *MockPersister) Fetch() (string, store.Transaction, error) {
	return fmt.Sprintf("[%s]", testStr), &MockTransaction{}, nil
}

func (mockPersister *MockPersisterError) Write(str string) error {
	return fmt.Errorf("test error in writing")
}
func (mockPersister *MockPersisterError) Fetch() (string, store.Transaction, error) {
	return "", &MockTransaction{}, fmt.Errorf("test error 1")
}

func TestRunWithMockPersister(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	_ = ioutil.WriteFile("test.json", []byte(testStr), 0644)
	stopCh := make(chan struct{})
	client := NewTestClient(func(req *http.Request) *http.Response {
		return &http.Response{
			StatusCode: 200,
			Body:       ioutil.NopCloser(bytes.NewBufferString("OK")),
			Header:     make(http.Header),
		}
	})
	ticker := time.NewTicker(time.Duration(2) * time.Second)
	publisher := &Publisher{
		Ticker:      ticker,
		Logger:      logger,
		SpServerUrl: "http://example.com",
		HttpClient:  client,
		Persister:   &MockPersister{},
	}
	go publisher.Run(stopCh)
	time.Sleep(10 * time.Second)
	close(stopCh)

	files, err := filepath.Glob("./*.json")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestRunWithMockPersisterError(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	_ = ioutil.WriteFile("test.json", []byte(testStr), 0644)
	stopCh := make(chan struct{})
	client := NewTestClient(func(req *http.Request) *http.Response {
		return &http.Response{
			StatusCode: 200,
			Body:       ioutil.NopCloser(bytes.NewBufferString("OK")),
			Header:     make(http.Header),
		}
	})
	ticker := time.NewTicker(time.Duration(2) * time.Second)
	publisher := &Publisher{
		Ticker:      ticker,
		Logger:      logger,
		SpServerUrl: "http://example.com",
		HttpClient:  client,
		Persister:   &MockPersisterError{},
	}
	go publisher.Run(stopCh)
	time.Sleep(10 * time.Second)
	close(stopCh)

	files, err := filepath.Glob("./*.json")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestRunWithErrorFromServer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	_ = ioutil.WriteFile("test.json", []byte(testStr), 0644)
	stopCh := make(chan struct{})
	ticker := time.NewTicker(time.Duration(2) * time.Second)
	client := NewTestClient(func(req *http.Request) *http.Response {
		return &http.Response{
			StatusCode: 500,
			Body:       ioutil.NopCloser(bytes.NewBufferString("OK")),
			Header:     make(http.Header),
		}
	})
	publisher := &Publisher{
		Ticker:      ticker,
		Logger:      logger,
		SpServerUrl: "http://example.com",
		HttpClient:  client,
		Persister:   &MockPersister{},
	}
	go publisher.Run(stopCh)
	time.Sleep(10 * time.Second)
	close(stopCh)

	files, err := filepath.Glob("./*.json")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}
