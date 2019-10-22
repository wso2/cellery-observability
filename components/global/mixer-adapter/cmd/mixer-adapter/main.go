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

package main

import (
	"log"
	"net/http"
	"os"
	"sync"
	"time"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/adapter"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/config"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/publisher"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/signals"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store/database"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store/file"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store/memory"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/writer"
)

const (
	configFilePathEnv string = "CONFIG_FILE_PATH"
)

func main() {
	port := adapter.DefaultAdapterPort

	// Initialize a channel to handle interruptions from external sources like Kubernetes. This channel will handle signals like os.Interrupt, syscall.SIGTERM
	stopCh := signals.SetupSignalHandler()
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %s", err.Error())
	}
	defer func() {
		err := logger.Sync()
		if err != nil {
			log.Fatalf("Error syncing logger: %s", err.Error())
		}
	}()

	configuration, err := config.New(os.Getenv(configFilePathEnv))
	if err != nil {
		logger.Fatalf("Could not get the configuration : %s", err.Error())
	}
	if configuration == nil {
		logger.Fatalf("Configuration struct is nil")
		return
	}

	advancedConfig := configuration.Advanced
	bufferTimeoutSeconds := advancedConfig.MaxRecordsForSingleWrite
	maxRecordsForSingleWrite := advancedConfig.MaxRecordsForSingleWrite
	tickerSec := configuration.SpEndpoint.SendIntervalSeconds

	client := &http.Client{}
	buffer := make(chan string, maxRecordsForSingleWrite*configuration.Advanced.BufferSizeFactor)
	errCh := make(chan error, 1)
	spAdapter, err := adapter.New(port, logger, client, buffer, configuration)
	if err != nil {
		logger.Fatalf("unable to start the server: ", err.Error())
	}
	if spAdapter == nil {
		logger.Fatal("adapter.New returned a null struct")
		return
	}
	go spAdapter.Run(errCh)

	var ps store.Persister
	wrt := &writer.Writer{
		WaitingTimeSec: bufferTimeoutSeconds,
		WaitingSize:    maxRecordsForSingleWrite,
		Logger:         logger,
		Buffer:         buffer,
		StartTime:      time.Now(),
	}
	ticker := time.NewTicker(time.Duration(tickerSec) * time.Second)
	pub := &publisher.Publisher{
		Ticker:      ticker,
		Logger:      logger,
		SpServerUrl: configuration.SpEndpoint.URL,
		HttpClient:  &http.Client{},
	}

	metricsStore := configuration.Store
	// Check the config map to initialize the correct persistence
	if metricsStore.FileStorage.Path != "" {
		// File storage will be used for persistence. Priority will be given to the file system
		logger.Info("Enabling file persistence")
		err = file.New(configuration)
		if err != nil {
			logger.Fatalf("could not make the directory in the given path %s : error => %s", configuration.Store.FileStorage.Path, err.Error())
		}
		ps = &file.Persister{
			Logger:    logger,
			Directory: metricsStore.FileStorage.Path,
		}
	} else if metricsStore.Database.Host != "" {
		// Db will be used for persistence
		logger.Info("Enabling database persistence")
		db, err := database.New(configuration)
		if err != nil {
			logger.Fatalf("Could not connect to the MySQL database : %s", err.Error())
		} else {
			ps = &database.Persister{
				Logger: logger,
				Db:     db,
			}
		}
	} else {
		// In memory persistence
		logger.Info("Enabling in memory persistence")
		inMemoryBuffer := make(chan string, maxRecordsForSingleWrite*advancedConfig.BufferSizeFactor)
		ps = &memory.Persister{
			Logger: logger,
			Buffer: inMemoryBuffer,
		}
	}

	var waitGroup sync.WaitGroup
	wrt.Persister = ps
	pub.Persister = ps
	go func() {
		waitGroup.Add(1)
		defer waitGroup.Done()
		wrt.Run(stopCh)
	}()
	go func() {
		waitGroup.Add(1)
		defer waitGroup.Done()
		pub.Run(stopCh)
	}()

	select {
	case <-stopCh:
		// This will wait for publisher and writer
		// If any interruption happens, this will give some time to clear in memory buffers by persisting them to prevent data losses.
		waitGroup.Wait()
	case err = <-errCh:
		if err != nil {
			logger.Fatal(err.Error())
		}
	}
}
