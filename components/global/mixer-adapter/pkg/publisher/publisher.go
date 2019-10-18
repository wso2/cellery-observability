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

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store"
)

type (
	Publisher struct {
		Ticker      *time.Ticker
		Logger      *zap.SugaredLogger
		SpServerUrl string
		HttpClient  *http.Client
		Persister   store.Persister
	}
)

func (publisher *Publisher) Run(stopCh <-chan struct{}) {
	publisher.Logger.Info("Publisher started")
	for {
		select {
		case <-stopCh:
			return
		case <-publisher.Ticker.C:
			publisher.execute()
		}
	}
}

func (publisher *Publisher) execute() {
	for {
		str, transaction, err := publisher.Persister.Fetch()
		if err != nil {
			publisher.Logger.Error(err.Error())
			return
		}
		if str != "" {
			err = publisher.publish(str)
			if err != nil {
				publisher.Logger.Error(err.Error())
				err = transaction.Rollback()
				if err != nil {
					publisher.Logger.Error(err.Error())
				}
			} else {
				err = transaction.Commit()
				if err != nil {
					publisher.Logger.Error(err.Error())
				}
			}
		}
	}
}

func (publisher *Publisher) publish(jsonArr string) error {
	var buf bytes.Buffer
	g := gzip.NewWriter(&buf)
	if _, err := g.Write([]byte(jsonArr)); err != nil {
		return fmt.Errorf("could not write to buffer : %s", err.Error())
	}
	if err := g.Close(); err != nil {
		return fmt.Errorf("could not close the gzip writer : %s", err.Error())
	}
	req, err := http.NewRequest("POST", publisher.SpServerUrl, &buf)
	if err != nil {
		return fmt.Errorf("could not make a new request : %s", err.Error())
	}

	client := &http.Client{}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Content-Encoding", "gzip")
	_, err = client.Do(req)
	if err != nil {
		return fmt.Errorf("could not receive a response from the server : %s", err.Error())
	}
	return nil
}
