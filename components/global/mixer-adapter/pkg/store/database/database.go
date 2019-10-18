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

	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store"
)

type (
	Persister struct {
		Logger *zap.SugaredLogger
		Db     *sql.DB
	}
	Transaction struct {
		Tx *sql.Tx
	}
)

func (transaction *Transaction) Commit() error {
	e := transaction.Tx.Commit()
	if e != nil {
		return fmt.Errorf("could not commit the sql transaction : %s", e.Error())
	}
	return nil
}

func (transaction *Transaction) Rollback() error {
	e := transaction.Tx.Rollback()
	if e != nil {
		return fmt.Errorf("could not rollback the sql transaction : %s", e.Error())
	}
	return nil
}

func (persister *Persister) Write(str string) error {
	err := persister.doTransaction(func(tx *sql.Tx) error {
		_, err := tx.Exec("INSERT INTO persistence(json) VALUES (?)", str)
		if err != nil {
			return fmt.Errorf("could not insert the metrics to the database : %s", err.Error())
		}
		return nil
	})
	if err != nil {
		return fmt.Errorf("could not store the metrics in the database, restoring... => error : %s", err.Error())
	}
	return nil
}

func (persister *Persister) Fetch() (string, store.Transaction, error) {
	tx, err := persister.Db.Begin()
	defer persister.catchPanic(tx)
	if err != nil {
		return "", &Transaction{}, fmt.Errorf("could not begin the transaction : %s", err.Error())
	}
	rows, err := tx.Query("SELECT id,json FROM persistence LIMIT 1 FOR UPDATE")
	if err != nil {
		return "", &Transaction{}, fmt.Errorf("could not fetch rows from the database : %s", err.Error())
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			persister.Logger.Warnf("could not close the Rows : %s", err.Error())
		}
	}()
	jsonArr := ""
	id := ""
	for rows.Next() {
		err = rows.Scan(&id, &jsonArr)
	}
	if jsonArr == "" || jsonArr == "[]" {
		return "", &Transaction{}, nil
	}
	_, err = tx.Exec("DELETE FROM persistence WHERE id = ?", id)
	if err != nil {
		return "", &Transaction{}, fmt.Errorf("could not delete the Rows : %s", err.Error())
	}
	transaction := &Transaction{Tx: tx}
	return jsonArr, transaction, nil
}

func (persister *Persister) catchPanic(tx *sql.Tx) {
	if p := recover(); p != nil {
		persister.Logger.Infof("There was a panic in the process : %s", p)
		e := tx.Rollback()
		if e != nil {
			persister.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
		}
	}
}

func (persister *Persister) doTransaction(fn func(*sql.Tx) error) (err error) {
	tx, err := persister.Db.Begin()
	if err != nil {
		return fmt.Errorf("could not begin the transaction : %s", err.Error())
	}
	defer func() {
		if p := recover(); p != nil {
			e := tx.Rollback()
			if e != nil {
				persister.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
			}
			panic(p)
		} else if err != nil {
			e := tx.Rollback()
			if e != nil {
				persister.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
				err = e
			}
		} else {
			e := tx.Commit()
			if e != nil {
				persister.Logger.Warnf("Could not commit the transaction : %s", e.Error())
				err = e
			}
		}
	}()
	err = fn(tx)
	return err
}
