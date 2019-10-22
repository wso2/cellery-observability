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

package adapter

import (
	"context"
	"fmt"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gogo/protobuf/types"
	"istio.io/api/policy/v1beta1"
	"istio.io/istio/mixer/template/metric"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

type (
	RoundTripFunc func(req *http.Request) *http.Response
)

func (f RoundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) {
	return f(req), nil
}

func NewTestClient(fn RoundTripFunc) *http.Client {
	return &http.Client{
		Transport: fn,
	}
}

var (
	sampleInstance1 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_ip": {
				Value: &v1beta1.Value_IpAddressValue{IpAddressValue: &v1beta1.IPAddress{Value: []byte{}}},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 350},
		},
	}
	sampleInstance2 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_bool": {
				Value: &v1beta1.Value_BoolValue{BoolValue: false},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 0},
		},
	}
	sampleInstance3 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_string": {
				Value: &v1beta1.Value_StringValue{StringValue: "Test"},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 0},
		},
	}
	sampleInstance4 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_double": {
				Value: &v1beta1.Value_DoubleValue{DoubleValue: 1.5},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 0},
		},
	}
	sampleInstance5 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_code": {
				Value: &v1beta1.Value_Int64Value{Int64Value: 200},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 0},
		},
	}
	sampleInstance6 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_duration": {
				Value: &v1beta1.Value_DurationValue{DurationValue: &v1beta1.Duration{Value: &types.Duration{Nanos: 200}}},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 0},
		},
	}
)

func TestNewAdapter(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}
	buffer := make(chan string, 100)
	client := NewTestClient(func(req *http.Request) *http.Response {
		return &http.Response{
			StatusCode: 200,
			Header:     make(http.Header),
		}
	})
	adapter, err := New(DefaultAdapterPort, logger, client, buffer, &Mixer{})
	wantStr := fmt.Sprintf("[::]:%d", DefaultAdapterPort)
	if err != nil {
		t.Errorf("Error while creating the adapter : %s", err.Error())
	}
	if adapter == nil {
		t.Error("Adapter is nil")
	} else {
		if adapter.Addr() == wantStr {
			defer func() {
				err := adapter.Close()
				if err != nil {
					log.Fatalf("Error closing adapter: %s", err.Error())
				}
			}()
			t.Log("Success, expected address is received")
		} else {
			t.Error("Fail, Expected address is not received")
		}
	}
}

func TestHandleMetric(t *testing.T) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", DefaultAdapterPort))
	if err != nil {
		t.Errorf("Unable to listen on socket: %s", err.Error())
	}

	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}

	testServer := httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		res.WriteHeader(200)
	}))
	defer func() { testServer.Close() }()

	buffer := make(chan string, 100)

	wso2SpAdapter := &Adapter{
		listener:   listener,
		logger:     logger,
		httpClient: &http.Client{},
		buffer:     buffer,
	}

	var sampleInstances []*metric.InstanceMsg
	sampleInstances = append(sampleInstances, sampleInstance1)
	sampleInstances = append(sampleInstances, sampleInstance2)
	sampleInstances = append(sampleInstances, sampleInstance3)
	sampleInstances = append(sampleInstances, sampleInstance4)
	sampleInstances = append(sampleInstances, sampleInstance5)
	sampleInstances = append(sampleInstances, sampleInstance6)

	sampleMetricRequest := metric.HandleMetricRequest{
		Instances: sampleInstances,
	}

	_, err = wso2SpAdapter.HandleMetric(context.TODO(), &sampleMetricRequest)

	if err != nil {
		t.Errorf("Metric could not be handled : %s", err.Error())
	} else {
		t.Log("Successfully handled the metrics")
	}

}
