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

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/adapter"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/config"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/logging"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/publisher"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/signals"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store/database"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store/file"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store/memory"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/writer"
)

const (
	configFilePathEnv     string = "CONFIG_FILE_PATH"
	defaultConfigFilePath string = "/etc/conf/config.json"
)

func main() {
	port := adapter.AdapterPort
	// Initialize a channel to handle interruptions from external sources like Kubernetes. This channel will handle
	// signals like os.Interrupt, syscall.SIGTERM
	stopCh := signals.SetupSignalHandler()
	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %v", err)
	}
	defer func() {
		err := logger.Sync()
		if err != nil {
			log.Fatalf("Error syncing logger: %v", err)
		}
	}()

	configFilePath := os.Getenv(configFilePathEnv)
	var configuration *config.Config
	if configFilePath != "" {
		configuration, err = config.New(os.Getenv(configFilePathEnv))
		if err != nil {
			logger.Fatalf("Could not get configurations from the config file path : %v", err)
		}
	} else {
		logger.Info("Config file path is not given. Going for the default path.")
		configuration, err = config.New(defaultConfigFilePath)
		if err != nil {
			logger.Fatalf("Could not get configurations from the default config file path : %v", err)
		}
	}

	// Initializing variables from the config file
	advancedConfig := configuration.Advanced
	bufferTimeoutSeconds := advancedConfig.BufferTimeoutSeconds
	maxMetricsCount := advancedConfig.MaxRecordsForSingleWrite
	bufferSizeFactor := advancedConfig.BufferSizeFactor
	tickerSec := configuration.SpEndpoint.SendIntervalSeconds

	client := &http.Client{}
	buffer := make(chan string, maxMetricsCount*bufferSizeFactor)
	errCh := make(chan error, 1)
	spAdapter, err := adapter.New(port, logger, client, buffer, &configuration.Mixer)
	if err != nil {
		logger.Fatalf("unable to start the server: %v", err)
	}
	go spAdapter.Run(errCh)

	var ps store.Persister
	metricsStore := configuration.Store
	// Check the config map to initialize the correct persistence mode
	if metricsStore.File != nil {
		// File storage will be used for persistence. Priority will be given to the file system
		logger.Info("Enabling file persistence")
		if metricsStore.File.Path == "" {
			logger.Fatal("Given file path is empty")
		}
		ps, err = file.NewPersister(configuration.Store.File, logger)
		if err != nil {
			logger.Fatalf("Could not get the persister from the file package : error %v",
				configuration.Store.File.Path, err)
		}
	} else if metricsStore.Database != nil {
		// Database will be used for persistence
		logger.Info("Enabling database persistence")
		ps, err = database.NewPersister(configuration.Store.Database, logger)
		if err != nil {
			logger.Fatalf("Could not get the persister from the database package : %v", err)
		}
	} else {
		// In memory persistence
		logger.Info("Enabling in memory persistence")
		ps, err = memory.NewPersister(maxMetricsCount, bufferSizeFactor, logger)
		if err != nil {
			logger.Fatalf("Could not get the persister from the memory package : %v", err)
		}
	}

	var waitGroup sync.WaitGroup
	wrt := &writer.Writer{
		WaitingTimeSec:  bufferTimeoutSeconds,
		WaitingSize:     maxMetricsCount,
		Logger:          logger,
		Buffer:          buffer,
		LastWrittenTime: time.Now(),
		Persister:       ps,
	}
	ticker := time.NewTicker(time.Duration(tickerSec) * time.Second)
	pub := &publisher.Publisher{
		Ticker:      ticker,
		Logger:      logger,
		SpServerUrl: configuration.SpEndpoint.URL,
		HttpClient:  &http.Client{},
		Persister:   ps,
		RuntimeId:   configuration.Advanced.RuntimeId,
		AccessToken: configuration.Advanced.AccessToken,
	}
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
		// If any interruption happens, this will give some time to clear in memory buffers by persisting them to
		// prevent data losses.
		waitGroup.Wait()
	case err = <-errCh:
		if err != nil {
			logger.Fatalf("Something went wrong when initializing the adapter : %v", err)
		}
	}
}
