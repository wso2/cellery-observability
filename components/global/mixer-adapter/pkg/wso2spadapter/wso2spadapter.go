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
// Generates the mygrpcadapter adapter's resource yaml. It contains the adapter's configuration, name,
// supported template names (metric in this case), and whether it is session or no-session based.
//go:generate $GOPATH/src/istio.io/istio/bin/mixer_codegen.sh -a mixer/adapter/wso2spadapter/config/config.proto -x "-s=false -n wso2spadapter -t metric"

package wso2spadapter

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"os"

	"go.uber.org/zap"
	"google.golang.org/grpc"
	"istio.io/api/mixer/adapter/model/v1beta1"
	policy "istio.io/api/policy/v1beta1"
	"istio.io/istio/mixer/template/metric"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/config"
)

const spServerUrl string = "http://wso2sp-worker:9091"

type (
	// Server is basic server interface
	Server interface {
		Addr() string
		Close() error
		Run(shutdown chan error)
	}

	// Wso2SpAdapter supports metric template.
	Wso2SpAdapter struct {
		listener          net.Listener
		server            *grpc.Server
		logger            *zap.SugaredLogger
		httpClient        *http.Client
		responseInfoError ResponseInfoError
	}

	/* This interface and the struct has been implemented to test the sendMetrics() function */
	ResponseInfoError interface {
		sendMetrics(attributeMap map[string]interface{}, logger *zap.SugaredLogger, httpClient *http.Client) bool
	}

	SpServerResponseInfoError struct{}
)

// HandleMetric records metric entries
func (wso2SpAdapter *Wso2SpAdapter) HandleMetric(ctx context.Context, r *metric.HandleMetricRequest) (*v1beta1.ReportResult, error) {

	wso2SpAdapter.logger.Debug("received request : ", *r)

	var buffer bytes.Buffer
	cfg := &config.Params{}

	if r.AdapterConfig != nil {
		if err := cfg.Unmarshal(r.AdapterConfig.Value); err != nil {
			wso2SpAdapter.logger.Error("error unmarshalling adapter config: ", err.Error())
			return nil, err
		}
	}

	buffer.WriteString(fmt.Sprintf("HandleMetric invoked with:\n  Adapter config: %s\n  Instances: %s\n",
		cfg.String(), instances(r.Instances)))

	if cfg.FilePath != " " {
		_, err := os.OpenFile("out.txt", os.O_RDONLY|os.O_CREATE, 0666)
		if err != nil {
			wso2SpAdapter.logger.Error("error creating file: ", err.Error())
			return nil, err
		}
		file, err := os.OpenFile(cfg.FilePath, os.O_APPEND|os.O_WRONLY, 0600)
		if err != nil {
			wso2SpAdapter.logger.Error("error opening file for append: ", err.Error())
			return nil, err
		}

		defer file.Close()

		if _, err = file.Write(buffer.Bytes()); err != nil {
			wso2SpAdapter.logger.Error("error writing to file: ", err.Error())
			return nil, err
		}
	}

	var insts = r.Instances
	for _, inst := range insts {
		var attributesMap = decodeDimensions(inst.Dimensions)
		wso2SpAdapter.responseInfoError.sendMetrics(attributesMap, wso2SpAdapter.logger, wso2SpAdapter.httpClient) // ToDO : get the boolean value from the function to check whether the metric is delivered or not
	}

	return &v1beta1.ReportResult{}, nil
}

/* Send metrics to sp server */
func (spServerResponseInfoErr SpServerResponseInfoError) sendMetrics(attributeMap map[string]interface{}, logger *zap.SugaredLogger, httpClient *http.Client) bool {
	jsonValue, _ := json.Marshal(attributeMap)
	var jsonStr = []byte(jsonValue)
	req, err := http.NewRequest("POST", spServerUrl, bytes.NewBuffer(jsonStr))
	req.Header.Set("Content-Type", "application/json")

	resp, err := httpClient.Do(req)
	if err != nil {
		return false
	}
	defer resp.Body.Close()
	body, _ := ioutil.ReadAll(resp.Body)
	logger.Info("response : ", string(body))
	return true // ToDo : return the boolean value considering the response
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
	default:
		return fmt.Sprintf("%v", in)
	}
}

func instances(in []*metric.InstanceMsg) string {
	var b bytes.Buffer
	for _, inst := range in {
		b.WriteString(fmt.Sprintf("'%s':\n"+
			"  {\n"+
			"		Value = %v\n"+
			"		Dimensions = %v\n"+
			"  }", inst.Name, decodeValue(inst.Value.GetValue()), decodeDimensions(inst.Dimensions)))
	}
	return b.String()
}

// Addr returns the listening address of the server
func (wso2SpAdapter *Wso2SpAdapter) Addr() string {
	return wso2SpAdapter.listener.Addr().String()
}

// Run starts the server run
func (wso2SpAdapter *Wso2SpAdapter) Run(shutdown chan error) {
	shutdown <- wso2SpAdapter.server.Serve(wso2SpAdapter.listener)
}

// Close gracefully shuts down the server; used for testing
func (wso2SpAdapter *Wso2SpAdapter) Close() error {
	if wso2SpAdapter.server != nil {
		wso2SpAdapter.server.GracefulStop()
	}

	if wso2SpAdapter.listener != nil {
		_ = wso2SpAdapter.listener.Close()
	}

	return nil
}

// NewWso2SpAdapter creates a new IBP adapter that listens at provided port.
func NewWso2SpAdapter(addr string, logger *zap.SugaredLogger, httpClient *http.Client, responseInfoError ResponseInfoError, serverOption grpc.ServerOption) (Server, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%s", addr))
	if err != nil {
		return nil, fmt.Errorf("unable to listen on socket: %v", err)
	}

	adapter := &Wso2SpAdapter{
		listener:          listener,
		logger:            logger,
		httpClient:        httpClient,
		responseInfoError: responseInfoError,
	}

	logger.Info("listening on ", adapter.Addr())

	if serverOption != nil {
		adapter.server = grpc.NewServer(serverOption)
	} else {
		adapter.server = grpc.NewServer()
	}
	metric.RegisterHandleMetricServiceServer(adapter.server, adapter)
	return adapter, nil
}
