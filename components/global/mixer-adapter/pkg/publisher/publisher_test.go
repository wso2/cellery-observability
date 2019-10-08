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
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	"io/ioutil"
	"net/http"
	"testing"
	"time"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

type (
	RoundTripFunc func(req *http.Request) *http.Response
	MockCrr struct {}
	MockErr struct {}
)

func (f RoundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return f(req), nil
}

func NewTestClient(fn RoundTripFunc) *http.Client {
	return &http.Client{
		Transport: fn,
	}
}

func (mockPersister *MockCrr) Write() {}
func (mockPersister *MockCrr) Fetch(run chan bool) (string, error) {
	return fmt.Sprintf("[%s]", testStr), nil
}
func (mockPersister *MockCrr) Clean(err error) {}


func (mockPersister *MockErr) Write() {}
func (mockPersister *MockErr) Fetch(run chan bool) (string, error) {
	return "", fmt.Errorf("test error 1")
}
func (mockPersister *MockErr) Clean(err error) {}


func TestPublisher_Run(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}

	_ = ioutil.WriteFile("test.txt", []byte(testStr), 0644)

	shutdown := make(chan error, 1)

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
		Persister: &MockCrr{},
	}
	go publisher.Run(shutdown)
	time.Sleep(10 * time.Second)

	publisher.Persister = &MockErr{}
	go publisher.Run(shutdown)
	time.Sleep(10 * time.Second)

	client = NewTestClient(func(req *http.Request) *http.Response {
		return &http.Response{
			StatusCode: 500,
			Body:       ioutil.NopCloser(bytes.NewBufferString("OK")),
			Header:     make(http.Header),
		}
	})
	publisher.HttpClient = client
	go publisher.Run(shutdown)
	time.Sleep(10 * time.Second)

}
