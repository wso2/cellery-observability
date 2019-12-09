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
	"compress/gzip"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"testing"
	"time"

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/logging"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49." +
		"istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", " +
		"\"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", " +
		"\"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--" +
		"controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":" +
		"\"b55a0f7f20d36e49f8612bac4311791d\"}"
	metricsCounter int
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
	metricsCounter--
	return nil
}

func (mockTransaction *MockTransaction) Rollback() error {
	return nil
}

func (mockPersister *MockPersister) Write(str string) error {
	return nil
}
func (mockPersister *MockPersister) Fetch() (string, store.Transaction, error) {
	if metricsCounter > 0 {
		return fmt.Sprintf("[%s]", testStr), &MockTransaction{}, nil
	} else {
		return "", &MockTransaction{}, nil
	}
}

func (mockPersister *MockPersisterError) Write(str string) error {
	return fmt.Errorf("test error in writing")
}
func (mockPersister *MockPersisterError) Fetch() (string, store.Transaction, error) {
	return "", &MockTransaction{}, fmt.Errorf("test error 1")
}

func TestFetchWithMockPersister(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	metricsCounter = 1
	if err != nil {
		t.Errorf("could not write the file : %v", err)
	}
	client := NewTestClient(func(req *http.Request) *http.Response {
		if req.Method != http.MethodPost {
			t.Errorf("Expected a POST method. but received : %s", req.Method)
		}
		if req.URL.Host != "example.com" {
			t.Errorf("Requested url is wrong, requested : %s, expected : %s", req.URL.Host, "example.com")
		}
		var buf bytes.Buffer
		bytesArr, err := ioutil.ReadAll(req.Body)
		if err != nil {
			t.Errorf("Could not read the body of the request : %v", err)
		}
		err = decodeGzip(&buf, bytesArr)
		if err != nil {
			t.Errorf("Error when decoding gzip : %v", err)
		}
		expectedStr := fmt.Sprintf("[%s]", testStr)
		if buf.String() != expectedStr {
			t.Errorf("Expected error has not been received, expected : %s, received : %s", expectedStr, buf.String())
		}
		return &http.Response{
			StatusCode: 200,
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
	err = publisher.execute()
	if err != nil {
		t.Errorf("Unexpected error occured : %v", err)
	}
}

func decodeGzip(w io.Writer, data []byte) error {
	gr, err := gzip.NewReader(bytes.NewBuffer(data))
	defer gr.Close()
	data, err = ioutil.ReadAll(gr)
	if err != nil {
		return err
	}
	_, err = w.Write(data)
	return err
}

func TestFetchWithMockPersisterError(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	metricsCounter = 1
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
	err = publisher.execute()
	expectedErr := "failed to fetch the metrics : test error 1"
	if err == nil {
		t.Errorf("An error was not thrown, but expected : %s", expectedErr)
		return
	}
	if err.Error() != expectedErr {
		t.Errorf("Expected error was not thrown, received error : %v", err)
	}
}

func TestFetchWithErrorFromServer(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	metricsCounter = 1
	ticker := time.NewTicker(time.Duration(2) * time.Second)
	client := NewTestClient(func(req *http.Request) *http.Response {
		return &http.Response{
			StatusCode: 500,
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
	err = publisher.execute()
	expectedErr := "failed to publish the metrics : received a bad response code from the server, received response code : 500"
	if err == nil {
		t.Errorf("An error was not thrown, but expected : %s", expectedErr)
		return
	}
	if err.Error() != expectedErr {
		t.Errorf("Expected error was not thrown, received error : %v", err)
	}
}
