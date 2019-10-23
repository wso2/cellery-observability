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

package file

import (
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"testing"

	"github.com/gofrs/flock"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

func TestWriteWithoutErrors(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	persister := &Persister{
		Logger:    logger,
		Directory: ".",
	}
	err = persister.Write(fmt.Sprintf("[%s]", testStr))
	if err != nil {
		t.Error("Could not write")
	}
	files, err := filepath.Glob("./*.txt")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestWriteWithInvalidDirectory(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	persister := &Persister{
		Logger:    logger,
		Directory: "./wrong_directory",
	}
	err = persister.Write(fmt.Sprintf("[%s]", testStr))
	if err == nil {
		t.Error("Expected error was not thrown")
	}
	files, err := filepath.Glob("./*.txt")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestFetchWithoutErrors(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	persister := &Persister{
		Logger:    logger,
		Directory: "./",
	}
	_ = ioutil.WriteFile("./test.txt", []byte(testStr), 0644)
	str, _, _ := persister.Fetch()
	if str != testStr {
		t.Error("Contents are not equal")
	}
	files, err := filepath.Glob("./*.txt")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestFetchWithEmptyFile(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	persister := &Persister{
		Logger:    logger,
		Directory: "./",
	}
	_ = ioutil.WriteFile("./test.txt", []byte(""), 0644)
	_, _, err = persister.Fetch()
	if err == nil {
		t.Error("No error was thrown when reading an empty file")
	}
	files, err := filepath.Glob("./*.txt")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestFetchWithInvalidDirectory(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %v", err)
	}
	persister := &Persister{
		Logger:    logger,
		Directory: "./wrong_dir",
	}
	str, _, _ := persister.Fetch()
	if str != "" {
		t.Error("Unexpected behaviour from the function")
	}
}

func TestCommitWithoutErrors(t *testing.T) {
	_ = ioutil.WriteFile("./test.txt", []byte(testStr), 0644)
	transaction := &Transaction{
		Lock: flock.New("./test.txt"),
	}
	err := transaction.Commit()
	if err != nil {
		t.Error("Error when committing")
	}
	files, err := filepath.Glob("./*.txt")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestCommitWithError(t *testing.T) {
	transaction := &Transaction{
		Lock: flock.New("./test.txt"),
	}
	err := transaction.Commit()
	if err == nil {
		t.Error("No error was thrown when trying to delete an already deleted file")
	}
}

func TestRollback(t *testing.T) {
	_ = ioutil.WriteFile("./test.txt", []byte(testStr), 0644)
	transaction := &Transaction{
		Lock: flock.New("./test.txt"),
	}
	_, _ = transaction.Lock.TryLock()
	err := transaction.Rollback()
	if err != nil {
		t.Error("An error was thrown when unlocking the file")
	}
	files, err := filepath.Glob("./*.txt")
	for _, fname := range files {
		err = os.Remove(fname)
	}
}

func TestNewMethodWithoutDir(t *testing.T) {
	config := &File{Path: "./testDir"}
	_ = New(config)
	_ = os.RemoveAll(config.Path)
}

func TestNewMethodWithDir(t *testing.T) {
	config := &File{Path: "./testDir"}
	_ = os.MkdirAll(config.Path, os.ModePerm)
	_ = New(config)
	_ = os.RemoveAll(config.Path)
}
