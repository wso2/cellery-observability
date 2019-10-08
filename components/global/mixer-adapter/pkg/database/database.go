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
	"strings"

	"go.uber.org/zap"
)

type (
	Persister struct {
		Logger      *zap.SugaredLogger
		Db          *sql.DB
		WaitingSize int
		Buffer      chan string
	}

	Transaction interface {
		Exec(query string, args ...interface{}) (sql.Result, error)
		Query(query string, args ...interface{}) (*sql.Rows, error)
	}
)

var trans *sql.Tx

func (persister *Persister) Fetch(run chan bool) (string, error) {
	var err error
	trans, err = persister.Db.Begin()
	if err != nil {
		persister.Logger.Warnf("Could not begin the transaction : %s", err.Error())
		return "", err
	}
	rows, err := trans.Query("SELECT * FROM persistence LIMIT ? FOR UPDATE", persister.WaitingSize)
	if err != nil {
		persister.Logger.Warnf("Could not fetch rows from the database : %s", err.Error())
		return "", err
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			persister.Logger.Warnf("Could not close the Rows : %s", err.Error())
		}
	}()

	var jsons []string
	var ids []string

	for rows.Next() {
		var id, json string
		err = rows.Scan(&id, &json)
		jsons = append(jsons, json)
		ids = append(ids, id)
	}

	jsonArr := fmt.Sprintf("[%s]", strings.Join(jsons, ","))
	idArr := fmt.Sprintf("(%s)", strings.Join(ids, ","))

	if jsonArr == "[]" {
		persister.Clean(fmt.Errorf("received empty rows"))
		run <- false
		return "", fmt.Errorf("received empty rows")
	}

	_, err = trans.Exec("DELETE FROM persistence WHERE id IN" + idArr)
	if err != nil {
		persister.Logger.Warnf("Could not delete the Rows : %s", err.Error())
		return "", err
	}

	return jsonArr, nil
}

func (persister *Persister) Clean(err error) {

	if trans == nil {
		return
	}

	if p := recover(); p != nil {
		e := trans.Rollback()
		if e != nil {
			persister.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
			err = e
		}
	} else if err != nil {
		e := trans.Rollback()
		if e != nil {
			persister.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
			err = e
		}
	} else {
		e := trans.Commit()
		if e != nil {
			persister.Logger.Warnf("Could not commit the transaction : %s", e.Error())
			err = e
		}
	}
}

func (persister *Persister) Write() {
	for i := 0; i < persister.WaitingSize; i++ {
		element := <-persister.Buffer
		if element == "" {
			if len(persister.Buffer) == 0 {
				break
			}
			continue
		}
		err := persister.doTransaction(func(tx Transaction) error {
			//CREATE TABLE IF NOT EXISTS `persistence` (`id` INT AUTO_INCREMENT PRIMARY KEY, `json` LONGTEXT);
			_, err := tx.Exec("INSERT INTO persistence(json) VALUES (?)", element)
			if err != nil {
				persister.Logger.Warnf("Could not insert the metric to the database : %s", err.Error())
				return err
			}

			return nil
		})
		if err != nil {
			persister.Logger.Debug("Could not store the metric in the database, restoring...")
			persister.Buffer <- element
		}
		if len(persister.Buffer) == 0 {
			break
		}
	}
}

func (persister *Persister) doTransaction(fn func(Transaction) error) (err error) {
	tx, err := persister.Db.Begin()
	if err != nil {
		persister.Logger.Warnf("Could not begin the transaction : %s", err.Error())
		return err
	}

	defer func() {
		if p := recover(); p != nil {
			e := tx.Rollback()
			if e != nil {
				persister.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
				err = e
			}
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
