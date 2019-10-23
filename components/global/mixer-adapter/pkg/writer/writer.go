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
	"strings"
	"time"

	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store"
)

type (
	Writer struct {
		WaitingTimeSec  int
		WaitingSize     int
		Logger          *zap.SugaredLogger
		Buffer          chan string
		LastWrittenTime time.Time
		Persister       store.Persister
	}
)

func (writer *Writer) Run(stopCh <-chan struct{}) {
	writer.Logger.Info("Writer started")
	for {
		select {
		case <-stopCh:
			writer.cleanBuffer()
			return
		default:
			if writer.shouldWrite() {
				writer.write()
			} else {
				time.Sleep(5 * time.Second)
			}
		}
	}
}

func (writer *Writer) write() {
	elements := writer.getElements()
	str := fmt.Sprintf("[%s]", strings.Join(elements, ","))
	err := writer.Persister.Write(str)
	if err != nil {
		writer.Logger.Warn(err.Error())
		writer.restore(elements)
	}
}

func (writer *Writer) cleanBuffer() {
	for {
		if len(writer.Buffer) == 0 {
			return
		}
		elements := writer.getElements()
		str := fmt.Sprintf("[%s]", strings.Join(elements, ","))
		err := writer.Persister.Write(str)
		if err != nil {
			writer.Logger.Warn(err.Error())
			writer.restore(elements)
		}
	}
}

func (writer *Writer) shouldWrite() bool {
	if (len(writer.Buffer) > 0) && (len(writer.Buffer) >= writer.WaitingSize || time.Since(writer.LastWrittenTime) >
		time.Duration(writer.WaitingTimeSec)*time.Second) {
		writer.Logger.Debugf("Time since the previous write : %s", time.Since(writer.LastWrittenTime))
		writer.LastWrittenTime = time.Now()
		return true
	} else {
		return false
	}
}

func (writer *Writer) getElements() []string {
	var elements []string
	for i := 0; i < writer.WaitingSize; i++ {
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

func (writer *Writer) restore(elements []string) {
	for _, element := range elements {
		writer.Buffer <- element
	}
}
