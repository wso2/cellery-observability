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

// nolint:lll
// Generates the adapter's resource yaml. It contains the adapter's configuration, name,
// supported template names (metric in this case), and whether it is session or no-session based.
//go:generate $GOPATH/src/istio.io/istio/bin/mixer_codegen.sh -a mixer/adapter/adapter/config/config.proto -x "-s=false -n adapter -t metric"

package adapter

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"

	"google.golang.org/grpc/credentials"

	"go.uber.org/zap"
	"google.golang.org/grpc"
	"istio.io/api/mixer/adapter/model/v1beta1"
	policy "istio.io/api/policy/v1beta1"
	"istio.io/istio/mixer/template/metric"
)

const (
	AdapterPort int = 38355
)

type (
	// Server is basic server interface
	Server interface {
		Addr() string
		Close() error
		Run(errCh chan error)
	}

	// The struct for the adapter. (Adapter supports metric template)
	Adapter struct {
		listener   net.Listener
		server     *grpc.Server
		logger     *zap.SugaredLogger
		httpClient *http.Client
		buffer     chan string
	}

	Mixer struct {
		TLS struct {
			Certificate   string `json:"certificate"`
			PrivateKey    string `json:"privateKey"`
			CaCertificate string `json:"caCertificate"`
		} `json:"tls"`
	}
)

// Decode received metrics from the mixer
func (adapter *Adapter) HandleMetric(ctx context.Context, r *metric.HandleMetricRequest) (*v1beta1.ReportResult, error) {

	var instances = r.Instances
	for _, inst := range instances {
		var attributesMap = decodeDimensions(inst.Dimensions)
		adapter.logger.Debugf("received request : %s", attributesMap)
		adapter.writeToBuffer(attributesMap)
	}

	return &v1beta1.ReportResult{}, nil
}

func (adapter *Adapter) writeToBuffer(attributeMap map[string]interface{}) {
	jsonValue, err := json.Marshal(attributeMap)
	if err != nil {
		return
	}
	adapter.buffer <- string(jsonValue)
}

func decodeDimensions(in map[string]*policy.Value) map[string]interface{} {
	out := make(map[string]interface{}, len(in))
	for k, v := range in {
		out[k] = decodeValue(v.GetValue())
	}
	return out
}

func decodeValue(in interface{}) interface{} {
	switch t := in.(type) {
	case *policy.Value_StringValue:
		return t.StringValue
	case *policy.Value_Int64Value:
		return t.Int64Value
	case *policy.Value_DoubleValue:
		return t.DoubleValue
	case *policy.Value_BoolValue:
		return t.BoolValue
	case *policy.Value_IpAddressValue:
		return t.IpAddressValue
	case *policy.Value_DurationValue:
		return t.DurationValue.Value.Nanos
	default:
		return fmt.Sprintf("%v", in)
	}
}

// Addr returns the listening address of the server
func (adapter *Adapter) Addr() string {
	return adapter.listener.Addr().String()
}

// Run starts the server run
func (adapter *Adapter) Run(errCh chan error) {
	errCh <- adapter.server.Serve(adapter.listener)
	_ = adapter.Close()
}

// Close gracefully shuts down the server; used for testing
func (adapter *Adapter) Close() error {
	if adapter.server != nil {
		adapter.server.GracefulStop()
	}
	if adapter.listener != nil {
		_ = adapter.listener.Close()
	}
	return nil
}

// New creates a new SP adapter that listens at provided port.
func New(addr int, logger *zap.SugaredLogger, httpClient *http.Client, buffer chan string, config *Mixer) (Server, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", addr))
	if err != nil {
		return nil, fmt.Errorf("unable to listen on socket: %v", err)
	}
	adapter := &Adapter{
		listener:   listener,
		logger:     logger,
		httpClient: httpClient,
		buffer:     buffer,
	}
	logger.Info("listening on ", adapter.Addr())
	mixerTls := config.TLS
	var serverOption grpc.ServerOption = nil
	if mixerTls.Certificate != "" {
		serverOption, err = getServerTLSOption(mixerTls.Certificate, mixerTls.PrivateKey, mixerTls.CaCertificate)
		if err != nil {
			logger.Warn("Server option could not be fetched, Connection will not be encrypted")
		}
	}
	if serverOption != nil {
		adapter.server = grpc.NewServer(serverOption)
	} else {
		adapter.server = grpc.NewServer()
	}
	metric.RegisterHandleMetricServiceServer(adapter.server, adapter)
	return adapter, nil
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
		return nil, fmt.Errorf("failed to read client ca cert: %v", err)
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
