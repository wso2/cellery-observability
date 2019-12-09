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

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store"
)

type (
	Publisher struct {
		Ticker      *time.Ticker
		Logger      *zap.SugaredLogger
		SpServerUrl string
		HttpClient  *http.Client
		Persister   store.Persister
		RuntimeId   string
	}

	SpEndpoint struct {
		URL                 string `json:"url"`
		SendIntervalSeconds int    `json:"sendIntervalSeconds"`
	}
)

func (publisher *Publisher) Run(stopCh <-chan struct{}) {
	publisher.Logger.Info("Publisher started")
	for {
		select {
		case <-stopCh:
			return
		case <-publisher.Ticker.C:
			err := publisher.execute()
			if err != nil {
				publisher.Logger.Errorf("Error when executing : %v", err)
			}
		}
	}
}

func (publisher *Publisher) execute() error {
	for {
		str, transaction, err := publisher.Persister.Fetch()
		if err != nil {
			rollbackErr := transaction.Rollback()
			if rollbackErr != nil {
				publisher.Logger.Debugf("Could not rollback the transaction : %v", rollbackErr)
			}
			return fmt.Errorf("failed to fetch the metrics : %v", err)
		}
		if str != "" {
			err = publisher.publish(str)
			if err != nil {
				rollbackErr := transaction.Rollback()
				if rollbackErr != nil {
					publisher.Logger.Errorf("Failed to rollback the transaction : %v", rollbackErr)
				}
				return fmt.Errorf("failed to publish the metrics : %v", err)
			} else {
				err = transaction.Commit()
				if err != nil {
					publisher.Logger.Errorf("Failed to commit the transaction : %v", err)
				}
			}
		} else {
			err = transaction.Rollback()
			if err != nil {
				publisher.Logger.Debugf("Could not rollback the transaction : %v", err)
			}
			return nil
		}
	}
}

func (publisher *Publisher) publish(jsonArr string) error {
	jsonPayload := fmt.Sprintf("{\"runtime\":\"%s\",\"data\":%s}", publisher.RuntimeId, jsonArr)
	var buf bytes.Buffer
	g := gzip.NewWriter(&buf)
	if _, err := g.Write([]byte(jsonPayload)); err != nil {
		return fmt.Errorf("could not write to buffer : %v", err)
	}
	if err := g.Close(); err != nil {
		return fmt.Errorf("could not close the gzip writer : %v", err)
	}
	req, err := http.NewRequest("POST", publisher.SpServerUrl, &buf)
	if err != nil {
		return fmt.Errorf("could not make a new request : %v", err)
	}

	client := publisher.HttpClient
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Content-Encoding", "gzip")
	res, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("could not receive a response from the server : %v", err)
	}
	if res != nil && res.StatusCode != 200 {
		return fmt.Errorf("received a bad response code from the server, received response code : %d",
			res.StatusCode)
	}
	return nil
}
