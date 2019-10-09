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
	"strings"
	"time"

	"github.com/gofrs/flock"
	"github.com/rs/xid"
	"go.uber.org/zap"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/retrier"
)

type (
	Persister struct {
		WaitingSize int
		Logger      *zap.SugaredLogger
		Buffer      chan string
		Directory   string
	}
)

var fname string

func (persister *Persister) Write() {

	fileLock := persister.createFile()
	persister.Logger.Debugf("Created a new file : %s", fileLock.String())
	_, err := retrier.Retry(5, 2*time.Second, "LOCK", func() (locked interface{}, err error) {
		locked, err = fileLock.TryLock()
		return
	})

	if err != nil {
		persister.Logger.Warnf("Could not lock the created file : %s", err.Error())
		return
	}

	elements := persister.getElements()
	str := fmt.Sprintf("[%s]", strings.Join(elements, ","))

	bytesArr := []byte(str)
	_, err = retrier.Retry(10, 2*time.Second, "WRITE", func() (locked interface{}, err error) {
		err = ioutil.WriteFile(fileLock.String(), bytesArr, 0644)
		return
	})

	if err != nil {
		persister.Logger.Warnf("Could not write to the file, restoring... => error: %s,", err.Error())
		persister.restore(elements)
	}

	_, err = retrier.Retry(10, 2*time.Second, "UNLOCK", func() (locked interface{}, err error) {
		err = fileLock.Unlock()
		return
	})

	if err != nil {
		persister.Logger.Debugf("Could not unlock the file after writing : %s", err.Error())
	}

}

func (persister *Persister) createFile() *flock.Flock {
	uuid := xid.New().String()
	fileLock := flock.New(fmt.Sprintf("%s/%s.txt", persister.Directory, uuid))
	return fileLock
}

func (persister *Persister) restore(elements []string) {
	for _, element := range elements {
		persister.Buffer <- element
	}
}

func (persister *Persister) getElements() []string {
	var elements []string
	for i := 0; i < persister.WaitingSize; i++ {
		element := <-persister.Buffer
		if element == "" {
			if len(persister.Buffer) == 0 {
				break
			}
			continue
		}
		elements = append(elements, element)
		if len(persister.Buffer) == 0 {
			break
		}
	}
	return elements
}

func (persister *Persister) unlock(flock *flock.Flock) {
	err := flock.Unlock()
	if err != nil {
		persister.Logger.Warn("Could not unlock the file")
	}
}

func (persister *Persister) Fetch(run chan bool) (string, error) {
	files, err := retrier.Retry(5, 1, "READ DIRECTORY", func() (files interface{}, err error) {
		files, err = filepath.Glob(persister.Directory + "/*.txt")
		return
	})
	if err != nil {
		persister.Logger.Warnf("Could not read the given directory %s : %s", persister.Directory, err.Error())
		return "", err
	}
	persister.Logger.Debugf("%s", files.([]string))
	if len(files.([]string)) > 0 {
		fname = files.([]string)[0]
		return persister.read(fname)
	} else {
		persister.Logger.Debug("No files in the directory")
		run <- false
		return "", fmt.Errorf("no files in the directory")
	}
}

func (persister *Persister) Clean(err error) {
	fileLock := flock.New(fname)
	if err == nil {
		_, err = retrier.Retry(10, 2*time.Second, "DELETE FILE", func() (i interface{}, e error) {
			e = os.Remove(fname)
			return i, e
		})
		if err != nil {
			persister.Logger.Warnf("Could not delete the published file : %s", err.Error())
		}
	} else {
		persister.unlock(fileLock)
	}
}

func (persister *Persister) read(fname string) (string, error) {
	fileLock := flock.New(fname)
	locked, err := fileLock.TryLock()

	if err != nil {
		persister.Logger.Debugf("Could not lock the file : %s", err.Error())
		return "", fmt.Errorf("could not lock the file")
	}

	if !locked {
		persister.Logger.Debug("Could not achieve the lock")
		return "", fmt.Errorf("could not achieve the lock")
	}

	data, err := ioutil.ReadFile(fname)
	if err != nil {
		persister.Logger.Warnf("Could not read the file : %s", err.Error())
		persister.unlock(fileLock)
		return "", fmt.Errorf("could not read the file")
	}

	if data == nil || string(data) == "" {
		persister.unlock(fileLock)
		_ = os.Remove(fname)
		return "", fmt.Errorf("file is empty") // Just delete the empty file
	}

	return string(data), nil
}
