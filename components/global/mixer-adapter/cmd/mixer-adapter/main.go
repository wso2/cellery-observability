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
	"crypto/tls"
	"crypto/x509"
	"database/sql"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"sync"
	"time"

	"github.com/go-sql-driver/mysql"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/adapter"
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

type (
	Config struct {
		Mixer struct {
			TLS struct {
				Certificate   string `json:"certificate"`
				PrivateKey    string `json:"privateKey"`
				CaCertificate string `json:"caCertificate"`
			} `json:"tls"`
		} `json:"mixer"`
		SpEndpoint struct {
			URL                 string `json:"url"`
			SendIntervalSeconds int    `json:"sendIntervalSeconds"`
		} `json:"spEndpoint"`
		Store struct {
			FileStorage struct {
				Path string `json:"path"`
			} `json:"fileStorage"`
			Database struct {
				Host     string `json:"host"`
				Port     int    `json:"port"`
				Protocol string `json:"protocol"`
				Username string `json:"username"`
				Password string `json:"password"`
				Name     string `json:"name"`
			} `json:"database"`
			InMemory struct {
			} `json:"inMemory"`
		} `json:"store"`
		Advanced struct {
			BufferSize             int `json:"bufferSize"`
			BufferSizeFactor       int `json:"bufferSizeFactor"`
			BufferTimeoutInSeconds int `json:"bufferTimeoutInSeconds"`
		} `json:"advanced"`
	}
)

func main() {
	port := adapter.DefaultAdapterPort
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

	data, err := ioutil.ReadFile(os.Getenv(configFilePathEnv))
	if err != nil {
		logger.Fatal("Could not read the config file")
	}
	config := &Config{}
	err = json.Unmarshal(data, &config)
	if err != nil {
		logger.Fatal("Could not unmarshal the data in config file")
	}

	// Mutual TLS feature to secure connection between workloads. This is optional.
	adapterCertificate := config.Mixer.TLS.Certificate // adapter.crt
	adapterPrivateKey := config.Mixer.TLS.PrivateKey   // adapter.key
	caCertificate := config.Mixer.TLS.CaCertificate    // ca.pem
	// Initialize the variables from the config map
	spServerUrl := config.SpEndpoint.URL
	filePath := config.Store.FileStorage.Path
	bufferTimeoutSeconds := config.Advanced.BufferTimeoutInSeconds
	minBufferSize := config.Advanced.BufferSize
	tickerSec := config.SpEndpoint.SendIntervalSeconds

	client := &http.Client{}
	var serverOption grpc.ServerOption = nil
	if adapterCertificate != "" {
		serverOption, err = getServerTLSOption(adapterCertificate, adapterPrivateKey, caCertificate)
		if err != nil {
			logger.Warn("Server option could not be fetched, Connection will not be encrypted")
		}
	}
	buffer := make(chan string, minBufferSize*config.Advanced.BufferSizeFactor)
	errCh := make(chan error, 1)
	spAdapter, err := adapter.New(port, logger, client, serverOption, spServerUrl, buffer)
	if err != nil {
		logger.Fatalf("unable to start the server: ", err.Error())
	}
	go func() {
		spAdapter.Run(errCh)
	}()
	var ps store.Persister
	wrt := &writer.Writer{
		WaitingTimeSec: bufferTimeoutSeconds,
		WaitingSize:    minBufferSize,
		Logger:         logger,
		Buffer:         buffer,
		StartTime:      time.Now(),
	}
	ticker := time.NewTicker(time.Duration(tickerSec) * time.Second)
	pub := &publisher.Publisher{
		Ticker:      ticker,
		Logger:      logger,
		SpServerUrl: spServerUrl,
		HttpClient:  &http.Client{},
	}

	var source = config.Store
	// Check the config map to initialize the correct persistence
	if source.FileStorage.Path != "" {
		// File storage will be used for persistence. Priority will be given to the file system
		logger.Info("Enabling file persistence")
		ps = &file.Persister{
			Logger:    logger,
			Directory: filePath,
		}
		wrt.Persister = ps
	} else if (source.Database.Host != "") && (source.Database.Username != "") {
		// Db will be used for persistence
		logger.Info("Enabling database persistence")
		dsn := (&mysql.Config{
			User:                 source.Database.Username,
			Passwd:               source.Database.Password,
			Net:                  source.Database.Protocol,
			Addr:                 fmt.Sprintf("%s:%d", source.Database.Host, source.Database.Port),
			DBName:               source.Database.Name,
			AllowNativePasswords: true,
			MaxAllowedPacket:     4 << 20,
		}).FormatDSN()
		db, err := sql.Open("mysql", dsn)
		if err != nil {
			logger.Fatalf("Could not connect to the MySQL database : %s", err.Error())
		} else {
			ps = &database.Persister{
				Logger: logger,
				Db:     db,
			}
			wrt.Persister = ps
		}
	} else {
		// In memory persistence
		logger.Info("Enabling in memory persistence")
		inMemoryBuffer := make(chan string, minBufferSize*config.Advanced.BufferSizeFactor)
		ps = &memory.Persister{
			Logger: logger,
			Buffer: inMemoryBuffer,
		}
		wrt.Persister = ps
	}

	var wg sync.WaitGroup
	go func() {
		wg.Add(1)
		defer wg.Done()
		wrt.Run(stopCh)
	}()
	pub.Persister = ps
	go func() {
		wg.Add(1)
		defer wg.Done()
		pub.Run(stopCh)
	}()

	select {
	case <-stopCh:
		wg.Wait()
	case err = <-errCh:
		if err != nil {
			logger.Fatal(err.Error())
		}
	}
}

func getServerTLSOption(adapterCertificate, adapterPrivateKey, caCertificate string) (grpc.ServerOption, error) {
	certificate, err := tls.LoadX509KeyPair(
		adapterCertificate,
		adapterPrivateKey,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to load key cert pair")
	}
	certPool := x509.NewCertPool()
	bytesArray, err := ioutil.ReadFile(caCertificate)
	if err != nil {
		return nil, fmt.Errorf("failed to read client ca cert: %s", err)
	}

	ok := certPool.AppendCertsFromPEM(bytesArray)
	if !ok {
		return nil, fmt.Errorf("failed to append client certs")
	}

	tlsConfig := &tls.Config{
		Certificates: []tls.Certificate{certificate},
		ClientCAs:    certPool,
	}
	tlsConfig.ClientAuth = tls.RequireAndVerifyClientCert

	return grpc.Creds(credentials.NewTLS(tlsConfig)), nil
}
