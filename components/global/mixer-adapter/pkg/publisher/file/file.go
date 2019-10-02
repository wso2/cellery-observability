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

package file

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
		Ticker      *time.Ticker
		Logger      *zap.SugaredLogger
		Directory   string
		SpServerUrl string
		HttpClient  *http.Client
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
			publisher.readDirectory()
		}
	}
}

func (publisher *Publisher) readDirectory() {
	files, err := retrier.Retry(5, 1, "READ DIRECTORY", func() (files interface{}, err error) {
		files, err = filepath.Glob(publisher.Directory + "/*.txt")
		return
	})
	if err != nil {
		publisher.Logger.Warn(err.Error())
		return
	}
	for _, fname := range files.([]string) {
		statesCode := publisher.publish(fname)
		if statesCode == 200 {
			err = os.Remove(fname)
			if err != nil {
				publisher.Logger.Warn(err.Error())
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
		publisher.Logger.Debug("File is locked")
		publisher.unlock(fileLock)
		return 500
	}
	data, err := ioutil.ReadFile(fname)

	if data == nil || string(data) == "" {
		publisher.unlock(fileLock)
		return 200 // Just delete the empty file
	}

	if err != nil {
		publisher.Logger.Warnf("Could not read the file : %s", err.Error())
		publisher.unlock(fileLock)
		return 500
	}

	var buf bytes.Buffer
	g := gzip.NewWriter(&buf)
	if _, err = g.Write(data); err != nil {
		publisher.Logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}
	if err = g.Close(); err != nil {
		publisher.Logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}
	req, err := http.NewRequest("POST", publisher.SpServerUrl, &buf)

	if err != nil {
		publisher.Logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}

	client := &http.Client{}

	req.Header.Set("Content-Type", "text/plain")
	req.Header.Set("Content-Encoding", "gzip")
	resp, err := client.Do(req)

	if err != nil {
		publisher.Logger.Debug(err.Error())
		publisher.unlock(fileLock)
		return 500
	}

	statusCode := resp.StatusCode

	return statusCode
}

func (publisher *Publisher) unlock(flock *flock.Flock) {
	err := flock.Unlock()
	if err != nil {
		publisher.Logger.Warn("Could not unlock the file")
	}
}
