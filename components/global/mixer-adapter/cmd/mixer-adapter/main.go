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
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/adapter"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/publisher"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/writer"
)

const (
	grpcAdapterCertificatePath string = "GRPC_ADAPTER_CERTIFICATE_PATH"
	grpcAdapterPrivateKeyPath  string = "GRPC_ADAPTER_PRIVATE_KEY_PATH"
	caCertificatePath          string = "CA_CERTIFICATE_PATH"
	spServerUrlPath            string = "SP_SERVER_URL"
	directory                  string = "DIRECTORY"
	shouldPersist              string = "SHOULD_PERSIST"
	waitingTimeSecEnv          string = "WAITING_TIME_SEC"
	queueLengthEnv             string = "QUEUE_LENGTH"
	tickerSecEnv               string = "TICKER_SEC"
)

func main() {
	port := adapter.DefaultAdapterPort //Pre defined port for the adaptor.
	persist := true

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

	if len(os.Args) > 1 {
		port, err = strconv.Atoi(os.Args[1])
		if err != nil {
			logger.Errorf("Could not convert the port number from string to int : %s", err.Error())
		}
	}

	// Mutual TLS feature to secure connection between workloads. This is optional.
	adapterCertificate := os.Getenv(grpcAdapterCertificatePath) // adapter.crt
	adapterprivateKey := os.Getenv(grpcAdapterPrivateKeyPath)   // adapter.key
	caCertificate := os.Getenv(caCertificatePath)               // ca.pem
	spServerUrl := os.Getenv(spServerUrlPath)
	directory := os.Getenv(directory)

	waitingSec, _ := strconv.Atoi(os.Getenv(waitingTimeSecEnv))
	queueLength, _ := strconv.Atoi(os.Getenv(queueLengthEnv))
	tickerSec, _ := strconv.Atoi(os.Getenv(tickerSecEnv))

	persist, err = strconv.ParseBool(os.Getenv(shouldPersist)) // this should be a string contains a boolean, "true" or "false"
	if err != nil {
		logger.Warnf("Could not convert env of persisting : %s", err.Error())
		persist = true
	}

	logger.Infof("Sp server url : %s", spServerUrl)
	logger.Infof("Directory to store files : %s", directory)
	logger.Infof("Should persist : %s", persist)

	client := &http.Client{}
	spPublisher := adapter.SPMetricsPublisher{}

	var serverOption grpc.ServerOption = nil

	if adapterCertificate != "" {
		serverOption, err = getServerTLSOption(adapterCertificate, adapterprivateKey, caCertificate)
		if err != nil {
			logger.Warn("Server option could not be fetched, Connection will not be encrypted")
		}
	}

	buffer := make(chan string, queueLength*10)

	spAdapter, err := adapter.New(port, logger, client, spPublisher, serverOption, spServerUrl, buffer, persist)
	if err != nil {
		logger.Fatal("unable to start server: ", err.Error())
	}

	shutdown := make(chan error, 1)
	go func() {
		spAdapter.Run(shutdown)
	}()

	if persist {

		mWriter := writer.New(waitingSec, queueLength, logger, buffer, directory)
		go func() {
			mWriter.Run(shutdown)
		}()

		ticker := time.NewTicker(time.Duration(tickerSec) * time.Second)

		mPublisher := publisher.New(ticker, logger, directory, spServerUrl, client)
		go func() {
			mPublisher.Run(shutdown)
		}()
	}

	err = <-shutdown
	if err != nil {
		logger.Fatal(err.Error())
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
