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

package e2e

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/DATA-DOG/go-sqlmock"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	databasepublisher "github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/publisher/database"
)

func TestPublisher_Run(t *testing.T) {

	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}

	tickerSec := 3
	queueLength := 2

	db, mock, err := sqlmock.New()

	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}

	rows := sqlmock.NewRows([]string{"id", "json"}).
		AddRow(1, testStr).
		AddRow(2, testStr)
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnRows(rows)
	mock.ExpectExec("^DELETE FROM persistence*").
		WillReturnResult(sqlmock.NewResult(2, 2))
	mock.ExpectCommit()

	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").WillReturnRows(sqlmock.NewRows([]string{}))
	mock.ExpectExec("^DELETE FROM persistence*").
		WillReturnResult(sqlmock.NewResult(0, 0))
	mock.ExpectCommit()
	_ = mock.ExpectationsWereMet()

	ticker := time.NewTicker(time.Duration(tickerSec) * time.Second)

	testServer := httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		res.WriteHeader(200)
	}))
	defer testServer.Close()

	var p = &databasepublisher.Publisher{
		Ticker:      ticker,
		Logger:      logger,
		SpServerUrl: testServer.URL,
		HttpClient:  &http.Client{},
		Db:          db,
		WaitingSize: queueLength,
	}

	go p.Execute()

	time.Sleep(10 * time.Second)
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	} else {
		t.Log("Test passed")
	}
}
