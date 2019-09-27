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
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/retrier"
	"github.com/gofrs/flock"
	"io/ioutil"
	"os"
	"path/filepath"
	"time"

	"go.uber.org/zap"
)

const (
	TickerSec int = 20
)

type (
	Publisher struct {
		ticker    *time.Ticker
		logger    *zap.SugaredLogger
		directory string
	}
)

func New(ticker *time.Ticker, logger *zap.SugaredLogger, directory string) Publisher {
	publisher := Publisher{
		ticker,
		logger,
		directory,
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
	files, err := retrier.Retry(10, 1, "READ DIRECTORY", func() (files interface{}, err error){
		files, err = filepath.Glob("./*.txt")
		return
	})
	if err != nil {
		publisher.logger.Warn(err.Error())
		return
	}
	for _, fname := range files.([]string) {
		fileLock := flock.New(publisher.directory + fname)
		locked, err := fileLock.TryLock()
		if err != nil { continue }
		if !locked {
			publisher.logger.Debug("File is locked")
			continue
		}
		data, err := ioutil.ReadFile(publisher.directory + fname)
		if err != nil {
			publisher.logger.Warnf("Could not read the file : %s", err.Error())
			continue
		}
		publisher.logger.Info(string(data))
		err = os.Remove(publisher.directory + fname)
		if err != nil {
			publisher.logger.Warn(err.Error())
			return
		}
	}
}
