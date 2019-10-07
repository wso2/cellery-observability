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

package e2e

import (
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/publisher"
	filepublisher "github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/publisher/file"
)

func TestPublisher(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}

	_ = ioutil.WriteFile("test.txt", []byte(testStr), 0644)

	shutdown := make(chan error, 1)
	buffer := make(chan string, 5)

	var p publisher.Publisher

	testServer := httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		res.WriteHeader(200)
	}))
	defer testServer.Close()

	ticker := time.NewTicker(time.Duration(2) * time.Second)
	p = &filepublisher.Publisher{
		Ticker:      ticker,
		Logger:      logger,
		Directory:   "./",
		SpServerUrl: testServer.URL,
		HttpClient:  &http.Client{},
	}

	go func() {
		p.Run(shutdown)
	}()

	buffer <- testStr
	time.Sleep(2 * time.Second)

	buffer <- testStr
	buffer <- testStr
	time.Sleep(2 * time.Second)

	buffer <- testStr
	time.Sleep(10 * time.Second)
}
