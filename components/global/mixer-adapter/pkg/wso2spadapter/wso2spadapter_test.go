package wso2spadapter

import (
	"fmt"
	"net"
	"net/http"
	"testing"

	"go.uber.org/zap"
	"istio.io/api/policy/v1beta1"
	"istio.io/istio/mixer/template/metric"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

const defaultAdapterPort string = "38355"

type DummyServerResponseInfoError struct {
	response bool
	err      error
}

var (
	sampleInstance1 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_code": {
				Value: &v1beta1.Value_Int64Value{Int64Value: 200},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 350},
		},
	}
	sampleInstance2 = &metric.InstanceMsg{
		Name: "wso2spadapter-metric",
		Dimensions: map[string]*v1beta1.Value{
			"response_code": {
				Value: &v1beta1.Value_Int64Value{Int64Value: 0},
			},
		},
		Value: &v1beta1.Value{
			Value: &v1beta1.Value_Int64Value{Int64Value: 0},
		},
	}
)

func (dummyServerResponseInfoError DummyServerResponseInfoError) sendMetrics(attributeMap map[string]interface{}, logger *zap.SugaredLogger, httpClient *http.Client) bool {
	if dummyServerResponseInfoError.err != nil {
		return false
	}
	return dummyServerResponseInfoError.response
}

func TestNewWso2SpAdapter(t *testing.T) {
	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}
	defer logger.Sync()
	adapter, err := NewWso2SpAdapter(defaultAdapterPort, logger, &http.Client{}, DummyServerResponseInfoError{}, "", "", "")
	wantStr := "[::]:38355"
	if err != nil {
		t.Errorf("Error while creating the adapter : %s", err.Error())
	}
	if adapter == nil {
		t.Error("Adapter is nil")
	} else {
		if adapter.Addr() == wantStr {
			adapter.Close()
			t.Log("Success, expected address is received")
		} else {
			t.Error("Fail, Expected address is not received")
		}
	}
}

func TestWso2SpAdapter_HandleMetric(t *testing.T) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%s", defaultAdapterPort))
	if err != nil {
		t.Errorf("Unable to listen on socket: %s", err.Error())
	}

	logger, err := logging.NewLogger()
	if err != nil {
		t.Errorf("Error building logger: %s", err.Error())
	}

	adapter := &Wso2SpAdapter{
		listener:          listener,
		logger:            logger,
		httpClient:        &http.Client{},
		responseInfoError: DummyServerResponseInfoError{},
	}

	var sampleInstances []*metric.InstanceMsg
	sampleInstances = append(sampleInstances, sampleInstance1)
	sampleInstances = append(sampleInstances, sampleInstance2)

	sampleMetricRequest := metric.HandleMetricRequest{
		Instances: sampleInstances,
	}

	_, err = adapter.HandleMetric(nil, &sampleMetricRequest)

	if err != nil {
		t.Errorf("Metric could not be handled : %s", err.Error())
	} else {
		t.Log("Successfully handled the metrics")
	}

}
