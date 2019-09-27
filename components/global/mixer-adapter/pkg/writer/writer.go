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
	"fmt"
	"io/ioutil"
	"time"

	"github.com/gofrs/flock"
	"github.com/rs/xid"
	"go.uber.org/zap"
)

type (
	Writer struct {
		waitingTimeSec int
		waitingSize    int
		logger         *zap.SugaredLogger
		buffer         chan string
		startTime      time.Time
	}
)

func New(waitingTimeSec int, waitingSize int, logger *zap.SugaredLogger, buffer chan string) Writer {
	writer := Writer{
		waitingTimeSec,
		waitingSize,
		logger,
		buffer,
		time.Now(),
	}
	return writer
}

func (writer *Writer) Run(shutdown chan error) {
	writer.logger.Info("Writer started")
	for {
		select {
		case quit := <-shutdown:
			writer.logger.Error(quit.Error())
			return
		default:
			if writer.shouldWrite() {
				writer.WriteToFile()
			}
		}
	}
}

func (writer *Writer) shouldWrite() bool {
	if (len(writer.buffer) > 0) && (len(writer.buffer) >= writer.waitingSize || time.Since(writer.startTime) > time.Duration(writer.waitingTimeSec)*time.Second) {
		writer.logger.Info(time.Since(writer.startTime))
		return true
	} else {
		return false
	}
}

//ToDo : after writing reset timer

func createFile() *flock.Flock {
	uuid := xid.New().String()
	fileLock := flock.New("./" + uuid + ".txt")
	return fileLock
}

func (writer *Writer) WriteToFile() bool {
	fileLock := createFile()
	writer.logger.Debug(fileLock.String())
	locked, err := retry(5, 2*time.Second, "LOCK", func() (locked bool, err error) {
		locked, err = fileLock.TryLock()
		return
	})
	if err != nil {
		writer.logger.Warn(err.Error())
		return false
	} else {
		str := "["
		for i := 0; i < writer.waitingSize; i++ {
			element := <-writer.buffer
			if i == 0 {
				str += element
			} else {
				str += "," + element
			}
		}
		str += "]"
		bytes := []byte(str)
		writer.logger.Debug(str)
		_, err := retry(5, 2*time.Second, "LOCK", func() (locked bool, err error) {
			err = ioutil.WriteFile(fileLock.String(), bytes, 0644)
			return
		})
		if err != nil {
			writer.logger.Warnf("Could not write to the file, error: %s, missed metrics : %s", err.Error(), str)
		}
		if locked {
			locked, err = retry(5, 2*time.Second, "UNLOCK", func() (locked bool, err error) {
				err = fileLock.Unlock()
				return
			})
			writer.startTime = time.Now()
		}
		return true
	}
}

func retry(attempts int, sleep time.Duration, action string, f func() (bool, error)) (locked bool, err error) {
	for i := 0; ; i++ {
		locked, err = f()
		if err == nil {
			return
		}
		if i >= (attempts - 1) {
			break
		}
		time.Sleep(sleep)
	}
	return locked, fmt.Errorf("tried %d times to %s, last error: %s", attempts, action, err)
}
