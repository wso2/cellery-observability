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

package config

import (
	"io/ioutil"
	"os"
	"path/filepath"
	"testing"
)

var (
	testStr = "{\r\n  \"mixer\": {\r\n    \"tls\": {\r\n      \"certificate\": \"\",\r\n      \"privateKey\": \"\",\r\n      \"caCertificate\": \"\"\r\n    }\r\n  },\r\n  \"spEndpoint\": {\r\n    \"url\": \"http://wso2sp-worker.cellery-system.svc.cluster.local:9091\",\r\n    \"sendIntervalSeconds\": 60\r\n  },\r\n  \"store\": {\r\n    \"fileStorage\": {\r\n      \"path\": \"/mnt/observability-metrics\"\r\n    },\r\n    \"database\": {\r\n      \"host\": \"wso2apim-with-analytics-rdbms-service.cellery-system.svc.cluster.local\",\r\n      \"port\": 3306,\r\n      \"protocol\": \"tcp\",\r\n      \"username\": \"root\",\r\n      \"password\": \"root\",\r\n      \"name\": \"PERSISTENCE\"\r\n    },\r\n    \"inMemory\": {}\r\n  },\r\n  \"advanced\": {\r\n    \"maxRecordsForSingleWrite\": 100,\r\n    \"bufferSizeFactor\": 100,\r\n    \"bufferTimeoutSeconds\": 60\r\n  }\r\n}"
)

func TestNewWithCorrectFile(t *testing.T) {
	_ = ioutil.WriteFile("./config.json", []byte(testStr), 0644)
	_, err := New("./config.json")
	if err != nil {
		t.Log(err)
	}
	files, err := filepath.Glob("./*.json")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestNewWithEmptyFile(t *testing.T) {
	_ = ioutil.WriteFile("./config.json", []byte(""), 0644)
	_, err := New("./config.json")
	if err != nil {
		t.Log(err)
	}
	files, err := filepath.Glob("./*.json")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestNewWithNoFile(t *testing.T) {
	_, err := New("./config.json")
	if err != nil {
		t.Log(err)
	}
}
