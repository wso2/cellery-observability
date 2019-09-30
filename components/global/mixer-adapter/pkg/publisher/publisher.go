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
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"time"

	"github.com/gofrs/flock"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/retrier"

	"go.uber.org/zap"
)

type (
	Publisher struct {
		ticker      *time.Ticker
		logger      *zap.SugaredLogger
		directory   string
		spServerUrl string
		httpClient  *http.Client
	}
)

func New(ticker *time.Ticker, logger *zap.SugaredLogger, directory string, spServerUrl string, httpClient *http.Client) *Publisher {
	publisher := &Publisher{
		ticker,
		logger,
		directory,
		spServerUrl,
		httpClient,
	}
	return publisher
}

func (publisher *Publisher) Run(shutdown chan error) {
	publisher.logger.Info("Publisher started")
	for {
		select {
		case quit := <-shutdown:
			publisher.logger.Fatal(quit.Error())
			return
		case _ = <-publisher.ticker.C:
			publisher.readDirectory()
		}
	}
}

func (publisher *Publisher) readDirectory() {
	files, err := retrier.Retry(10, 1, "READ DIRECTORY", func() (files interface{}, err error) {
		files, err = filepath.Glob(publisher.directory + "/*.txt")
		return
	})
	if err != nil {
		publisher.logger.Warn(err.Error())
		return
	}
	for _, fname := range files.([]string) {
		statesCode := publisher.publish(fname)
		if statesCode == 200 {
			err = os.Remove(fname)
			if err != nil {
				publisher.logger.Warn(err.Error())
				continue
			}
		}
	}
}

func (publisher *Publisher) publish(fname string) int {

	fileLock := flock.New(fname)
	locked, err := fileLock.TryLock()
	if err != nil {
		return 500
	}
	if !locked {
		publisher.logger.Debug("File is locked")
		publisher.unlock(fileLock)
		return 500
	}
	data, err := ioutil.ReadFile(fname)
	if err != nil {
		publisher.logger.Warnf("Could not read the file : %s", err.Error())
		publisher.unlock(fileLock)
		return 500
	}

	var buf bytes.Buffer
	g := gzip.NewWriter(&buf)
	if _, err = g.Write(data); err != nil {
		publisher.logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}
	if err = g.Close(); err != nil {
		publisher.logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}
	req, err := http.NewRequest("POST", publisher.spServerUrl, &buf)

	if err != nil {
		publisher.logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}

	client := &http.Client{}

	req.Header.Set("Content-Type", "text/plain")
	req.Header.Set("Content-Encoding", "gzip")
	resp, err := client.Do(req)

	if err != nil {
		publisher.logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}

	statusCode := resp.StatusCode

	return statusCode
}

func (publisher *Publisher) unlock(flock *flock.Flock) {
	err := flock.Unlock()
	if err != nil {
		publisher.logger.Warn("Could not unlock the file")
	}
}
