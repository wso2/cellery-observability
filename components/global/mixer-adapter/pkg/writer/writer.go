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

package writer

import (
	"io/ioutil"
	"time"

	"github.com/gofrs/flock"
	"github.com/rs/xid"
	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/retrier"
)

type (
	Writer struct {
		waitingTimeSec int
		waitingSize    int
		logger         *zap.SugaredLogger
		buffer         chan string
		startTime      time.Time
		directory      string
	}
)

func New(waitingTimeSec int, waitingSize int, logger *zap.SugaredLogger, buffer chan string, directory string) *Writer {
	writer := &Writer{
		waitingTimeSec,
		waitingSize,
		logger,
		buffer,
		time.Now(),
		directory,
	}
	return writer
}

func (writer *Writer) Run(shutdown chan error) {
	writer.logger.Info("Writer started")
	for {
		select {
		case quit := <-shutdown:
			writer.logger.Fatal(quit.Error())
			return
		default:
			if writer.shouldWrite() {
				writer.writeToFile()
			}
		}
	}
}

func (writer *Writer) shouldWrite() bool {
	if (len(writer.buffer) > 0) && (len(writer.buffer) >= writer.waitingSize || time.Since(writer.startTime) > time.Duration(writer.waitingTimeSec)*time.Second) {
		writer.logger.Debugf("Time since the previous write : %s", time.Since(writer.startTime))
		return true
	} else {
		return false
	}
}

func (writer *Writer) createFile() *flock.Flock {
	uuid := xid.New().String()
	fileLock := flock.New(writer.directory + uuid + ".txt")
	return fileLock
}

func (writer *Writer) writeToFile() bool {
	fileLock := writer.createFile()
	writer.logger.Debugf("Created a new file : %s", fileLock.String())
	locked, err := retrier.Retry(5, 2*time.Second, "LOCK", func() (locked interface{}, err error) {
		locked, err = fileLock.TryLock()
		return
	})
	if err != nil {
		writer.logger.Warn(err.Error())
		return false
	}
	if !locked.(bool) {
		writer.unlock(fileLock)
		return false
	}
	str := "["
	for i := 0; i < writer.waitingSize; i++ {
		element := <-writer.buffer
		if element == "" {
			if len(writer.buffer) == 0 {
				break
			}
			continue
		}
		if i == 0 {
			str += element
		} else {
			str += "," + element
		}
		if len(writer.buffer) == 0 {
			break
		}
	}
	str += "]"
	bytes := []byte(str)
	_, err = retrier.Retry(10, 2*time.Second, "WRITE", func() (locked interface{}, err error) {
		err = ioutil.WriteFile(fileLock.String(), bytes, 0644)
		return
	})
	if err != nil {
		writer.logger.Warnf("Could not write to the file, error: %s, missed metrics : %s", err.Error(), str)
	}

	locked, err = retrier.Retry(10, 2*time.Second, "UNLOCK", func() (locked interface{}, err error) {
		err = fileLock.Unlock()
		return
	})
	writer.startTime = time.Now()

	return true
}

func (writer *Writer) unlock(flock *flock.Flock) {
	err := flock.Unlock()
	if err != nil {
		writer.logger.Warn("Could not unlock the file")
	}
}
