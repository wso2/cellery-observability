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
	"os"
	"strconv"

	"github.com/go-sql-driver/mysql"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store/database"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store/memory"

	//"database/sql"
	"fmt"
	//"github.com/go-sql-driver/mysql"
	"io/ioutil"
	"log"
	"net/http"
	"time"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store"

	//"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/retrier"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/adapter"
	//"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/database"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/publisher"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/store/file"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/writer"

	_ "github.com/go-sql-driver/mysql"
)

type (
	configMap struct {
		tls struct {
			cert string
			key  string
			ca   string
		}
		publisher struct {
			endpoint            string
			sendIntervalSeconds int
		}
		persistence struct {
			bufferSize           int
			bufferTimeoutSeconds int
			source               struct {
				directory struct {
					path string
				}
				db struct {
					host     string
					port     string
					username string
					password string
				}
			}
		}
	}
)

var (
	testStr = "{\"contextReporterKind\":\"inbound\", \"destinationUID\":\"kubernetes://istio-policy-74d6c8b4d5-mmr49.istio-system\", \"requestID\":\"6e544e82-2a0c-4b83-abcc-0f62b89cdf3f\", \"requestMethod\":\"POST\", \"requestPath\":\"/istio.mixer.v1.Mixer/Check\", \"requestTotalSize\":\"2748\", \"responseCode\":\"200\", \"responseDurationNanoSec\":\"695653\", \"responseTotalSize\":\"199\", \"sourceUID\":\"kubernetes://pet-be--controller-deployment-6f6f5768dc-n9jf7.default\", \"spanID\":\"ae295f3a4bbbe537\", \"traceID\":\"b55a0f7f20d36e49f8612bac4311791d\"}"
)

func main() {
	port := adapter.DefaultAdapterPort

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

	var configMap = &configMap{
		tls: struct {
			cert string
			key  string
			ca   string
		}{cert: "", key: "", ca: ""},
		publisher: struct {
			endpoint            string
			sendIntervalSeconds int
		}{endpoint: "http://localhost:8500", sendIntervalSeconds: 5},
		persistence: struct {
			bufferSize           int
			bufferTimeoutSeconds int
			source               struct {
				directory struct {
					path string
				}
				db struct {
					host     string
					port     string
					username string
					password string
				}
			}
		}{bufferSize: 2, bufferTimeoutSeconds: 10, source: struct {
			directory struct {
				path string
			}
			db struct {
				host     string
				port     string
				username string
				password string
			}
		}{
			directory: struct {
				path string
			}{path: ""},
			db: struct {
				host     string
				port     string
				username string
				password string
			}{
				host:     "",
				port:     "",
				username: "",
				password: "",
			},
		}},
	}

	// Mutual TLS feature to secure connection between workloads. This is optional.
	adapterCertificate := configMap.tls.cert // adapter.crt
	adapterPrivateKey := configMap.tls.key   // adapter.key
	caCertificate := configMap.tls.ca        // ca.pem
	spServerUrl := configMap.publisher.endpoint
	filePath := configMap.persistence.source.directory.path
	bufferTimeoutSeconds := configMap.persistence.bufferTimeoutSeconds
	minBufferSize := configMap.persistence.bufferSize
	tickerSec := configMap.publisher.sendIntervalSeconds

	logger.Infof("Sp server url : %s", spServerUrl)
	client := &http.Client{}
	var serverOption grpc.ServerOption = nil
	if adapterCertificate != "" {
		serverOption, err = getServerTLSOption(adapterCertificate, adapterPrivateKey, caCertificate)
		if err != nil {
			logger.Warn("Server option could not be fetched, Connection will not be encrypted")
		}
	}
	buffer := make(chan string, minBufferSize*1000)
	shutdown := make(chan error, 1)
	spAdapter, err := adapter.New(port, logger, client, serverOption, spServerUrl, buffer)
	if err != nil {
		logger.Fatal("unable to start server: ", err.Error())
	}
	go func() {
		spAdapter.Run(shutdown)
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
	var source = configMap.persistence.source
	if source.directory.path != "" {
		//File storage will be used for persistence. Priority will be given to the file system
		logger.Info("Enabling file persistence")
		ps = &file.Persister{
			Logger:    logger,
			Directory: filePath,
		}
		wrt.Persister = ps
	} else if (source.db.host != "") && (source.db.port != "") && (source.db.username != "") {
		//Db will be used for persistence
		logger.Info("Enabling database persistence")
		dsn := (&mysql.Config{
			User:   source.db.username,
			Passwd: source.db.password,
			Addr:   fmt.Sprintf("%s:%s", source.db.host, source.db.port),
			DBName: "PERSISTENCE",
		}).FormatDSN()
		db, err := sql.Open("mysql", dsn)
		if err != nil {
			logger.Fatal("Could not connect to the MySQL database : %s", err.Error())
		} else {
			ps = &database.Persister{
				Logger: logger,
				Db:     db,
			}
			wrt.Persister = ps
		}
	} else {
		//In memory persistence
		logger.Info("Enabling in memory persistence")
		inMemoryBuffer := make(chan string, minBufferSize*1000)
		ps = &memory.Persister{
			Logger: logger,
			Buffer: inMemoryBuffer,
		}
		wrt.Persister = ps
	}
	go func() {
		wrt.Run(shutdown)
	}()
	pub.Persister = ps
	go func() {
		pub.Run(shutdown)
	}()

	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr
	buffer <- testStr

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
