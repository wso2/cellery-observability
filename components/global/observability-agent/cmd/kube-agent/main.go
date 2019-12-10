/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
	"flag"
	"log"
	"net/http"
	"os"
	"sync"
	"time"

	"cellery.io/cellery-controller/pkg/clients"
	meshinformers "cellery.io/cellery-controller/pkg/generated/informers/externalversions"
	kubeinformers "k8s.io/client-go/informers"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/klog"

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/config"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/kubeagent"
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

var (
	masterURL  string
	kubeconfig string
)

func main() {
	klog.InitFlags(nil)
	flag.Parse()

	stopCh := signals.SetupSignalHandler()

	logger, err := logging.NewLogger()
	if err != nil {
		log.Fatalf("Error building logger: %s", err.Error())
	}
	defer func() {
		err := logger.Sync()
		if err != nil {
			log.Fatalf("Error syncing logger: %v", err)
		}
	}()

	kcfg, err := clientcmd.BuildConfigFromFlags(masterURL, kubeconfig)
	if err != nil {
		logger.Fatalf("Error building kubeconfig: %s", err.Error())
	}

	clientset, err := clients.NewFromConfig(kcfg)

	if err != nil {
		logger.Fatalf("Error building clients: %v", err)
	}
	resync := time.Minute * 5
	kubeInformerFactory := kubeinformers.NewSharedInformerFactory(clientset.Kubernetes(), resync)
	meshInformerFactory := meshinformers.NewSharedInformerFactory(clientset.Mesh(), resync)

	podInformer := kubeInformerFactory.Core().V1().Pods()
	componentInformer := meshInformerFactory.Mesh().V1alpha2().Components()
	cellInformer := meshInformerFactory.Mesh().V1alpha2().Cells()
	compositeInformer := meshInformerFactory.Mesh().V1alpha2().Composites()

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

	buffer := make(chan string, maxMetricsCount*bufferSizeFactor)

	rw := kubeagent.New(podInformer, componentInformer, cellInformer, compositeInformer, buffer, logger)

	kubeInformerFactory.Start(stopCh)
	meshInformerFactory.Start(stopCh)
	err = rw.Run(stopCh)
	if err != nil {
		logger.Fatalf("Cannot start resource watcher : %v", err)
	}

	var ps store.Persister
	storeCfg := configuration.Store
	// Check the config map to initialize the correct persistence mode
	if storeCfg.File != nil {
		// File storage will be used for persistence. Priority will be given to the file system
		logger.Info("Enabling file persistence")
		if storeCfg.File.Path == "" {
			logger.Fatal("Given file path is empty")
		}
		ps, err = file.NewPersister(storeCfg.File, logger)
		if err != nil {
			logger.Fatalf("Could not get the persister from the file package : error %v",
				storeCfg.File.Path, err)
		}
	} else if storeCfg.Database != nil {
		// Database will be used for persistence
		logger.Info("Enabling database persistence")
		ps, err = database.NewPersister(storeCfg.Database, logger)
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

	<-stopCh
	// This will wait for publisher and writer
	// If any interruption happens, this will give some time to clear in memory buffers by persisting them to
	// prevent data losses.
	waitGroup.Wait()

}

func init() {
	flag.StringVar(&kubeconfig, "kubeconfig", "", "Path to a kubeconfig. Only required if out-of-cluster.")
	flag.StringVar(&masterURL, "master", "", "The address of the Kubernetes API server. Overrides any value in kubeconfig. Only required if out-of-cluster.")
}
