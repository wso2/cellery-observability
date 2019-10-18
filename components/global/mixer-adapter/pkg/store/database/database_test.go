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
	"fmt"
	"log"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

func TestPersister_Write(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %s", err.Error())
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}

	// Test for a transaction failure
	mock.ExpectBegin().WillReturnError(fmt.Errorf("test error 1"))

	// Test for a insertion failure
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnError(fmt.Errorf("test error 2"))
	mock.ExpectRollback()

	// Test for a rollback failure
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnError(fmt.Errorf("test error 3"))
	mock.ExpectRollback().WillReturnError(fmt.Errorf("test error 4"))

	// Test for a commit failure
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnResult(sqlmock.NewResult(2, 2))
	mock.ExpectCommit().WillReturnError(fmt.Errorf("test error 5"))

	// Test for a successful transaction
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(json)*").WillReturnResult(sqlmock.NewResult(2, 2))
	mock.ExpectCommit()

	persister := &Persister{
		Logger: logger,
		Db:     db,
	}
	_ = persister.Write(testStr)
	_ = persister.Write(testStr)
	_ = persister.Write(testStr)
	_ = persister.Write(testStr)
	_ = persister.Write(testStr)
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	} else {
		t.Log("Test passed")
	}
}

func TestPersister_Fetch(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %s", err.Error())
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	rows := sqlmock.NewRows([]string{"id", "json"}).
		AddRow(1, testStr).
		AddRow(2, testStr)

	// Test for transaction failure
	mock.ExpectBegin().WillReturnError(fmt.Errorf("test error 1"))

	// Test for a selection failure
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnError(fmt.Errorf("test error 2"))

	// Test for a deletion failure
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnRows(rows)
	mock.ExpectExec("^DELETE FROM persistence*").
		WillReturnError(fmt.Errorf("test error 3"))

	// Defining rows
	rows = sqlmock.NewRows([]string{"id", "json"}).
		AddRow(1, testStr).
		AddRow(2, testStr)

	// Test for a successful fetch
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnRows(rows)
	mock.ExpectExec("^DELETE FROM persistence*").
		WillReturnResult(sqlmock.NewResult(2, 2))

	// Test for fetching empty rows
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnRows(rows)

	persister := &Persister{
		Logger: logger,
		Db:     db,
	}
	_, _, _ = persister.Fetch()
	_, _, _ = persister.Fetch()
	_, _, _ = persister.Fetch()
	_, _, _ = persister.Fetch()
	_, _, _ = persister.Fetch()
	_, _, _ = persister.Fetch()
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	} else {
		t.Log("Test passed")
	}
}

func TestTransaction_Commit(t *testing.T) {
	db, mock, err := sqlmock.New()
	if db == nil || mock == nil || err != nil {
		t.Log("Could not initialize the database")
		return
	}
	mock.ExpectBegin()
	mock.ExpectCommit().WillReturnError(fmt.Errorf("test error 1"))
	tx, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %s", err.Error())
		return
	}
	transaction := &Transaction{Tx: tx}
	_ = transaction.Commit()

	tx2, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %s", err.Error())
		return
	}
	transaction2 := &Transaction{Tx: tx2}
	_ = transaction2.Commit()
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	} else {
		t.Log("Test passed")
	}
}

func TestTransaction_Rollback(t *testing.T) {
	db, mock, err := sqlmock.New()
	if db == nil || mock == nil || err != nil {
		t.Log("Could not initialize the database")
		return
	}
	mock.ExpectBegin()
	mock.ExpectRollback().WillReturnError(fmt.Errorf("test error 1"))
	tx, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %s", err.Error())
		return
	}
	transaction := &Transaction{Tx: tx}
	_ = transaction.Rollback()

	tx2, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %s", err.Error())
		return
	}
	transaction2 := &Transaction{Tx: tx2}
	_ = transaction2.Rollback()
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %s", err)
	} else {
		t.Log("Test passed")
	}
}
