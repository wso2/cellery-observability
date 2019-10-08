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
	"net/http"
	"time"

	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/persister"
)

type (
	Publisher struct {
		Ticker      *time.Ticker
		Logger      *zap.SugaredLogger
		SpServerUrl string
		HttpClient  *http.Client
		Persister   persister.Persister
	}
)

func (publisher *Publisher) Run(shutdown chan error) {
	publisher.Logger.Info("Publisher started")
	for {
		select {
		case quit := <-shutdown:
			publisher.Logger.Fatal(quit.Error())
			return
		case _ = <-publisher.Ticker.C:
			publisher.execute()
		}
	}
}

func (publisher *Publisher) execute() {

	run := make(chan bool, 1)
	for {
		select {
		case _ = <-run:
			return
		default:
			jsonArr, err := publisher.Persister.Fetch(run)
			if err == nil && jsonArr != "" {
				statusCode := publisher.publish(jsonArr)
				if statusCode != 200 {
					publisher.Persister.Clean(fmt.Errorf("bad response from the sp server : %d", statusCode))
				} else {
					publisher.Logger.Debugf("Response from the sp server : %d", statusCode)
					publisher.Persister.Clean(nil)
				}
			}
		}
	}

}

func (publisher *Publisher) publish(jsonArr string) int {

	var buf bytes.Buffer
	g := gzip.NewWriter(&buf)
	if _, err := g.Write([]byte(jsonArr)); err != nil {
		publisher.Logger.Debugf("Could not write to buffer : %s", err.Error())
		return 500
	}
	if err := g.Close(); err != nil {
		publisher.Logger.Debugf("Could not close the gzip writer : %s", err.Error())
		return 500
	}
	req, err := http.NewRequest("POST", publisher.SpServerUrl, &buf)

	if err != nil {
		publisher.Logger.Debug("Could not make a new request : %s", err.Error())
		return 500
	}

	client := &http.Client{}

	req.Header.Set("Content-Type", "text/plain")
	req.Header.Set("Content-Encoding", "gzip")
	resp, err := client.Do(req)

	if err != nil {
		publisher.Logger.Debug("Could not receive a response from the server : %s", err.Error())
		return 500
	}

	statusCode := resp.StatusCode

	return statusCode
}
