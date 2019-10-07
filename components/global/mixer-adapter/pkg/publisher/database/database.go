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
	"bytes"
	"compress/gzip"
	"database/sql"
	"fmt"
	"net/http"
	"strings"
	"time"

	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/retrier"
)

type (
	Publisher struct {
		Ticker      *time.Ticker
		Logger      *zap.SugaredLogger
		Db          *sql.DB
		SpServerUrl string
		HttpClient  *http.Client
		WaitingSize int
	}

	Transaction interface {
		Exec(query string, args ...interface{}) (sql.Result, error)
		Query(query string, args ...interface{}) (*sql.Rows, error)
	}
)

func (publisher *Publisher) Run(shutdown chan error) {
	publisher.Logger.Info("Database publisher started")
	for {
		select {
		case quit := <-shutdown:
			publisher.Logger.Fatal(quit.Error())
			return
		case _ = <-publisher.Ticker.C:
			publisher.execute()
		}
	}
}

func (publisher *Publisher) doTransaction(fn func(Transaction) error) (err error) {
	tx, err := publisher.Db.Begin()
	if err != nil {
		return
	}

	defer func() {
		if p := recover(); p != nil {
			_, err = retrier.Retry(10, 2*time.Second, "ROLLBACK", func() (locked interface{}, err error) {
				err = tx.Rollback()
				return
			})
			if err != nil {
				publisher.Logger.Warnf("Could not rollback the transaction : %s", err.Error())
			}
		} else if err != nil {
			_, err = retrier.Retry(10, 2*time.Second, "ROLLBACK", func() (locked interface{}, err error) {
				err = tx.Rollback()
				return
			})
			if err != nil {
				publisher.Logger.Warnf("Could not rollback the transaction : %s", err.Error())
			}
		} else {
			err = tx.Commit()
			if err != nil {
				publisher.Logger.Warnf("Could not commit : %s", err.Error())
			}
		}
	}()

	err = fn(tx)
	return err
}

func (publisher *Publisher) execute() {

	run := make(chan bool, 1)
	for {
		select {
		case _ = <-run:
			return
		default:
			publisher.read(run)
		}
	}

}

func (publisher *Publisher) read(run chan bool) {

	err := publisher.doTransaction(func(tx Transaction) error {

		rows, err := tx.Query("SELECT * FROM persistence LIMIT ? FOR UPDATE", publisher.WaitingSize)
		if err != nil {
			return err
		}
		defer func() {
			err = rows.Close()
			if err != nil {
				publisher.Logger.Warnf("Could not close the Rows : %s", err.Error())
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
			run <- false
		}

		_, err = tx.Exec("DELETE FROM persistence WHERE id IN" + idArr)
		if err != nil {
			return err
		}

		statusCode := publisher.publish(jsonArr)
		if statusCode != 200 {
			return fmt.Errorf("sp server responce : %d", statusCode)
		}

		return nil
	})

	if err != nil {
		publisher.Logger.Warnf("Could not read : %s", err.Error())
	}
}

func (publisher *Publisher) publish(jsonArr string) int {

	var buf bytes.Buffer
	g := gzip.NewWriter(&buf)
	if _, err := g.Write([]byte(jsonArr)); err != nil {
		publisher.Logger.Debug(err.Error())
		return 500
	}
	if err := g.Close(); err != nil {
		publisher.Logger.Debug(err.Error())
		return 500
	}
	req, err := http.NewRequest("POST", publisher.SpServerUrl, &buf)

	if err != nil {
		publisher.Logger.Debug(err.Error())
		return 500
	}

	client := &http.Client{}

	req.Header.Set("Content-Type", "text/plain")
	req.Header.Set("Content-Encoding", "gzip")
	resp, err := client.Do(req)

	if err != nil {
		publisher.Logger.Debug(err.Error())
		return 500
	}

	statusCode := resp.StatusCode

	return statusCode
}
