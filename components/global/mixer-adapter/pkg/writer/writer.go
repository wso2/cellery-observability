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
	"time"

	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/persister"
)

type (
	Writer struct {
		WaitingTimeSec int
		WaitingSize    int
		Logger         *zap.SugaredLogger
		Buffer         chan string
		StartTime      time.Time
		Persister      persister.Persister
	}
)

func (writer *Writer) Run(shutdown chan error) {
	writer.Logger.Info("writer started")
	for {
		select {
		case quit := <-shutdown:
			writer.Logger.Fatal(quit.Error())
			return
		default:
			if writer.shouldWrite() {
				writer.Persister.Write()
			}
		}
	}
}

func (writer *Writer) shouldWrite() bool {
	if (len(writer.Buffer) > 0) && (len(writer.Buffer) >= writer.WaitingSize || time.Since(writer.StartTime) > time.Duration(writer.WaitingTimeSec)*time.Second) {
		writer.Logger.Debugf("Time since the previous write : %s", time.Since(writer.StartTime))
		writer.StartTime = time.Now()
		return true
	} else {
		return false
	}
}
