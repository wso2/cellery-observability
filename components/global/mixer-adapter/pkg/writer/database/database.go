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
	"time"

	"go.uber.org/zap"
)

type (
	Writer struct {
		WaitingTimeSec int
		WaitingSize    int
		Logger         *zap.SugaredLogger
		Buffer         chan string
		StartTime      time.Time
		Db             *sql.DB
	}

	Transaction interface {
		Exec(query string, args ...interface{}) (sql.Result, error)
		Query(query string, args ...interface{}) (*sql.Rows, error)
	}
)

func (writer *Writer) Run(shutdown chan error) {
	writer.Logger.Info("Database writer started")
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

func (writer *Writer) doTransaction(fn func(Transaction) error) (err error) {
	tx, err := writer.Db.Begin()
	if err != nil {
		writer.Logger.Warnf("Could not begin the transaction : %s", err.Error())
		return err
	}

	defer func() {
		if p := recover(); p != nil {
			e := tx.Rollback()
			if e != nil {
				writer.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
				err = e
			}
		} else if err != nil {
			e := tx.Rollback()
			if e != nil {
				writer.Logger.Warnf("Could not rollback the transaction : %s", e.Error())
				err = e
			}
		} else {
			e := tx.Commit()
			if e != nil {
				//writer.Logger.Warnf("Could not commit the transaction : %s", e.Error())
				err = e
			}
		}
	}()

	err = fn(tx)
	return err
}

func (writer *Writer) write() {

	for i := 0; i < writer.WaitingSize; i++ {
		element := <-writer.Buffer
		if element == "" {
			if len(writer.Buffer) == 0 {
				break
			}
			continue
		}
		err := writer.doTransaction(func(tx Transaction) error {
			//CREATE TABLE IF NOT EXISTS `persistence` (`id` INT AUTO_INCREMENT PRIMARY KEY, `json` LONGTEXT);
			_, err := tx.Exec("INSERT INTO persistence(json) VALUES (?)", element)
			if err != nil {
				writer.Logger.Warnf("Could not insert the metric to the database : %s", err.Error())
				return err
			}

			return nil
		})
		if err != nil {
			writer.Logger.Debug("Could not store the metric in the database, restoring...")
			writer.Buffer <- element
		}
		if len(writer.Buffer) == 0 {
			break
		}
	}

}
