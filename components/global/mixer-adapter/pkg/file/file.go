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
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/dal"
	"io/ioutil"
	"math/rand"
	"os"
	"path/filepath"

	"github.com/gofrs/flock"
	"github.com/rs/xid"
	"go.uber.org/zap"

)

type (
	Persister struct {
		WaitingSize int
		Logger      *zap.SugaredLogger
		Directory   string
	}
	Cleaner struct {
		Lock *flock.Flock
		Logger *zap.SugaredLogger
	}
)

func (cleaner *Cleaner) Commit() error {
	err := os.Remove(cleaner.Lock.String())
	if err != nil {
		return fmt.Errorf("could not delete the published file : %s", err.Error())
	}
	return nil
}

func (cleaner *Cleaner) Rollback() error {
	err := cleaner.Lock.Unlock()
	if err != nil {
		return fmt.Errorf("could not unlock the file")
	}
	return nil
}

func (persister *Persister) Write(str string) error {
	fileLock := persister.createFile()
	persister.Logger.Debugf("Created a new file : %s", fileLock.String())
	locked, err := fileLock.TryLock()
	if !locked {
		return fmt.Errorf("could not lock the created file")
	}
	if err != nil {
		return fmt.Errorf("could not lock the created file : %s", err.Error())
	}
	bytesArr := []byte(str)
	err = ioutil.WriteFile(fileLock.String(), bytesArr, 0644)
	if err != nil {
		persister.unlock(fileLock)
		return fmt.Errorf("could not write to the file, restoring... => error: %s", err.Error())
	}
	err = fileLock.Unlock()
	if err != nil {
		return fmt.Errorf("could not unlock the file after writing : %s", err.Error())
	}

	return nil
}

func (persister *Persister) createFile() *flock.Flock {
	uuid := xid.New().String()
	fileLock := flock.New(fmt.Sprintf("%s/%s.txt", persister.Directory, uuid))
	return fileLock
}

func (persister *Persister) unlock(flock *flock.Flock) {
	err := flock.Unlock()
	if err != nil {
		persister.Logger.Warn("Could not unlock the file")
	}
}

func (persister *Persister) Fetch() (string, dal.Transaction, error) {
	files, err := filepath.Glob(persister.Directory + "/*.txt")
	if err != nil {
		return "",  &Cleaner{}, fmt.Errorf("could not read the given directory %s : %s", persister.Directory, err.Error())
	}
	persister.Logger.Debugf("%s", files)
	if len(files) > 0 {
		cleaner := &Cleaner{
			Lock: flock.New(files[rand.Intn(len(files))]),
			Logger: persister.Logger,
		}
		return persister.read(cleaner)
	} else {
		return "", &Cleaner{}, fmt.Errorf("no files in the directory")
	}
}

func (persister *Persister) read(cleaner *Cleaner) (string, *Cleaner, error) {
	locked, err := cleaner.Lock.TryLock()
	if !locked {
		return "", cleaner, fmt.Errorf("could not achieve the lock")
	}
	if err != nil {
		_ = cleaner.Lock.Unlock() //handle error
		return "", cleaner, fmt.Errorf("could not lock the file : %s", err.Error())
	}

	data, err := ioutil.ReadFile(cleaner.Lock.String())
	if err != nil {
		_ = cleaner.Lock.Unlock()
		return "", cleaner, fmt.Errorf("could not read the file : %s", err.Error())
	}
	if data == nil || string(data) == "" {
		_ = cleaner.Lock.Unlock()
		_ = os.Remove(cleaner.Lock.String())
		return "", cleaner, fmt.Errorf("file is empty") // Just delete the empty file
	}

	return string(data), cleaner, nil
}
