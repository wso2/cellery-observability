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

package database

import (
	"database/sql"
	"fmt"
	"testing"
	"time"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/writer"

	"github.com/DATA-DOG/go-sqlmock"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

func TestPublisher_Run(t *testing.T) {

	db, mock, err := sqlmock.New()

	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}

	mock.ExpectBegin().WillReturnError(fmt.Errorf("test error 1"))

	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnError(fmt.Errorf("test error 2"))
	mock.ExpectRollback()

	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnError(fmt.Errorf("test error 3"))
	mock.ExpectRollback().WillReturnError(fmt.Errorf("test error 4"))

	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnResult(sqlmock.NewResult(2, 2))
	mock.ExpectCommit().WillReturnError(fmt.Errorf("test error 4"))

	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnResult(sqlmock.NewResult(2, 2))
	mock.ExpectCommit()

	buffer := make(chan string, 20)

	Run(db, t, buffer)

	buffer <- testStr
	buffer <- ""

	time.Sleep(30 * time.Second)
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	} else {
		t.Log("Test passed")
	}
}

func Run(db *sql.DB, t *testing.T, buffer chan string) {

	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}
	shutdown := make(chan error, 1)

	var p writer.Writer
	p = &Writer{
		WaitingTimeSec: 5,
		Logger:         logger,
		WaitingSize:    2,
		Db:             db,
		Buffer:         buffer,
		StartTime:      time.Now(),
	}

	go func() {
		p.Run(shutdown)
	}()

}
