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
	"fmt"
	"io/ioutil"
	"strings"
	"time"

	"github.com/gofrs/flock"
	"github.com/rs/xid"
	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/retrier"
)

type (
	Writer struct {
		WaitingTimeSec int
		WaitingSize    int
		Logger         *zap.SugaredLogger
		Buffer         chan string
		StartTime      time.Time
		Directory      string
	}
)

func (writer *Writer) Run(shutdown chan error) {
	writer.Logger.Info("File writer started")
	for {
		select {
		case quit := <-shutdown:
			writer.Logger.Fatal(quit.Error())
			return
		default:
			if writer.shouldWrite() {
				writer.write()
			}
		}
	}
}

func (writer *Writer) shouldWrite() bool {
	if (len(writer.Buffer) > 0) && (len(writer.Buffer) >= writer.WaitingSize || time.Since(writer.StartTime) > time.Duration(writer.WaitingTimeSec)*time.Second) {
		writer.Logger.Debugf("Time since the previous write : %s", time.Since(writer.StartTime))
		return true
	} else {
		return false
	}
}

func (writer *Writer) createFile() *flock.Flock {
	uuid := xid.New().String()
	fileLock := flock.New(writer.Directory + uuid + ".txt")
	return fileLock
}

func (writer *Writer) write() bool {
	fileLock := writer.createFile()
	writer.Logger.Debugf("Created a new file : %s", fileLock.String())
	locked, err := retrier.Retry(5, 2*time.Second, "LOCK", func() (locked interface{}, err error) {
		locked, err = fileLock.TryLock()
		return
	})
	if err != nil {
		writer.Logger.Warn(err.Error())
		return false
	}
	if !locked.(bool) {
		writer.unlock(fileLock)
		return false
	}

	elements := writer.getElements()
	str := fmt.Sprintf("[%s]", strings.Join(elements, ","))

	bytes := []byte(str)
	_, err = retrier.Retry(10, 2*time.Second, "WRITE", func() (locked interface{}, err error) {
		err = ioutil.WriteFile(fileLock.String(), bytes, 0644)
		return
	})
	if err != nil {
		writer.Logger.Warnf("Could not write to the file, error: %s, missed metrics : %s", err.Error(), str)
	}

	locked, err = retrier.Retry(10, 2*time.Second, "UNLOCK", func() (locked interface{}, err error) {
		err = fileLock.Unlock()
		return
	})
	writer.StartTime = time.Now()

	return true
}

func (writer *Writer) getElements() []string {
	var elements []string
	for i := 0; i < writer.WaitingSize; i++ {
		num += 1
		writer.Logger.Infof("Writing count : %d", num)
		element := <-writer.Buffer
		if element == "" {
			if len(writer.Buffer) == 0 {
				break
			}
			continue
		}
		elements = append(elements, element)
		if len(writer.Buffer) == 0 {
			break
		}
	}
	return elements
}

func (writer *Writer) unlock(flock *flock.Flock) {
	err := flock.Unlock()
	if err != nil {
		writer.Logger.Warn("Could not unlock the file")
	}
}
