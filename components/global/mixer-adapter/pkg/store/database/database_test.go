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
	testStr = "[{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}]"
)

func TestWriteWithTransactionFailure(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	mock.ExpectBegin().WillReturnError(fmt.Errorf("test error 1"))
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	err = persister.Write(testStr)
	if err != nil && err.Error() == "could not store the metrics in the database : could not begin the transaction : test error 1" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestWriteWithInsertionFailure(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(data)*").WillReturnError(fmt.Errorf("test error 2"))
	mock.ExpectRollback()
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	err = persister.Write(testStr)
	if err != nil && err.Error() == "could not store the metrics in the database : could not insert the metrics to the database : test error 2" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestWriteWithRollbackFailure(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(data)*").WillReturnError(fmt.Errorf("test error 3"))
	mock.ExpectRollback().WillReturnError(fmt.Errorf("test error 4"))
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	err = persister.Write(testStr)
	if err != nil && err.Error() == "could not store the metrics in the database : test error 4" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestWriteWithCommitFailure(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(data)*").WillReturnResult(sqlmock.NewResult(2, 2))
	mock.ExpectCommit().WillReturnError(fmt.Errorf("test error 5"))
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	err = persister.Write(testStr)
	if err != nil && err.Error() == "could not store the metrics in the database : test error 5" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestWriteWithSuccessfulTransaction(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	mock.ExpectBegin()
	mock.ExpectExec("^INSERT INTO persistence(data)*").WillReturnResult(sqlmock.NewResult(2, 2))
	mock.ExpectCommit()
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	err = persister.Write(testStr)
	if err != nil {
		t.Errorf("An unexpected error received : %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestFetchWithTransactionFailure(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	mock.ExpectBegin().WillReturnError(fmt.Errorf("test error 1"))
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	str, _, err := persister.Fetch()
	if err != nil && err.Error() == "could not begin the transaction : test error 1" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if str != "" {
		t.Errorf("Expected an empty string, but received : %s", str)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestFetchWithSelectionFailure(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnError(fmt.Errorf("test error 2"))
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	str, _, err := persister.Fetch()
	if err != nil && err.Error() == "could not fetch rows from the database : test error 2" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if str != "" {
		t.Errorf("Expected an empty string, but received : %s", str)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestFetchWithDeletionFailure(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	rows := sqlmock.NewRows([]string{"id", "data"}).
		AddRow(1, testStr).
		AddRow(2, testStr)
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnRows(rows)
	mock.ExpectExec("^DELETE FROM persistence*").
		WillReturnError(fmt.Errorf("test error 3"))
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	str, _, err := persister.Fetch()
	if err != nil && err.Error() == "could not delete the Rows : test error 3" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if str != "" {
		t.Errorf("Expected an empty string, but received : %s", str)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestFetchWithSuccessfulFetch(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	rows := sqlmock.NewRows([]string{"id", "data"}).
		AddRow(1, testStr)
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnRows(rows)
	mock.ExpectExec("^DELETE FROM persistence*").
		WillReturnResult(sqlmock.NewResult(2, 2))
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	str, _, err := persister.Fetch()
	if err != nil {
		t.Errorf("An unexpected error received : %v", err)
	}
	if str != testStr {
		t.Errorf("Expected an empty string, but received : %s", str)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestFetchWithEmptyRows(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	rows := sqlmock.NewRows([]string{"id", "data"})
	mock.ExpectBegin()
	mock.ExpectQuery("^SELECT (.+) FROM persistence*").
		WillReturnRows(rows)
	persister := &Persister{
		logger: logger,
		db:     db,
	}
	str, _, err := persister.Fetch()
	if err != nil {
		t.Errorf("An unexpected error received : %v", err)
	}
	if str != "" {
		t.Errorf("Expected an empty string, but received : %s", str)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestCommitWithError(t *testing.T) {
	db, mock, err := sqlmock.New()
	if db == nil || mock == nil || err != nil {
		t.Log("Could not initialize the database")
		return
	}
	mock.ExpectBegin()
	mock.ExpectCommit().WillReturnError(fmt.Errorf("test error 1"))
	tx, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %v", err)
		return
	}
	transaction := &Transaction{Tx: tx}
	err = transaction.Commit()
	if err != nil && err.Error() == "could not commit the sql transaction : test error 1" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestCommitWithoutError(t *testing.T) {
	db, mock, err := sqlmock.New()
	if db == nil || mock == nil || err != nil {
		t.Log("Could not initialize the database")
		return
	}
	mock.ExpectBegin()
	mock.ExpectCommit()
	tx, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %v", err)
		return
	}
	transaction := &Transaction{Tx: tx}
	err = transaction.Commit()
	if err != nil {
		t.Errorf("An unexpected error received : %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestRollbackWithError(t *testing.T) {
	db, mock, err := sqlmock.New()
	if db == nil || mock == nil || err != nil {
		t.Log("Could not initialize the database")
		return
	}
	mock.ExpectBegin()
	mock.ExpectRollback().WillReturnError(fmt.Errorf("test error 1"))
	tx, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %v", err)
		return
	}
	transaction := &Transaction{Tx: tx}
	err = transaction.Rollback()
	if err != nil && err.Error() == "could not rollback the sql transaction : test error 1" {
		t.Log("Exact error received")
	} else {
		t.Error("Expected error has not been received")
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}

func TestRollbackWithoutError(t *testing.T) {
	db, mock, err := sqlmock.New()
	if db == nil || mock == nil || err != nil {
		t.Log("Could not initialize the database")
		return
	}
	mock.ExpectBegin()
	mock.ExpectRollback()
	tx, err := db.Begin()
	if err != nil {
		t.Logf("could not initialize the database %v", err)
		return
	}
	transaction := &Transaction{Tx: tx}
	err = transaction.Rollback()
	if err != nil {
		t.Errorf("An unexpected error received : %v", err)
	}
	if err := mock.ExpectationsWereMet(); err != nil {
		t.Errorf("there were unfulfilled expectations: %v", err)
	} else {
		t.Log("Test passed")
	}
}
