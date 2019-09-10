package e2e

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"
	"testing"

	adapter_integration "istio.io/istio/mixer/pkg/adapter/test"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/wso2spadapter"
)

const defaultAdapterPort string = "38355"

func TestReport(t *testing.T) {
	adptCrBytes, err := ioutil.ReadFile("./testdata/sample-wso2spadapter.yaml")
	if err != nil {
		t.Fatalf("could not read file: %v", err)
	}

	operatorCfgBytes, err := ioutil.ReadFile("./testdata/sample-operator-cfg.yaml")
	if err != nil {
		t.Fatalf("could not read file: %v", err)
	}
	operatorCfg := string(operatorCfgBytes)
	shutdown := make(chan error, 1)

	var outFile *os.File
	outFile, err = os.OpenFile("out.txt", os.O_RDONLY|os.O_CREATE, 0666)
	if err != nil {
		t.Fatal(err)
	}
	defer func() {
		if removeErr := os.Remove(outFile.Name()); removeErr != nil {
			t.Logf("Could not remove temporary file %s: %v", outFile.Name(), removeErr)
		}
	}()

	logger, err := logging.NewLogger()

	adapter_integration.RunTest(
		t,
		nil,
		adapter_integration.Scenario{
			Setup: func() (ctx interface{}, err error) {
				pServer, err := wso2spadapter.NewWso2SpAdapter(defaultAdapterPort, logger, &http.Client{}, wso2spadapter.SpServerResponseInfoError{}, "", "", "")
				if err != nil {
					return nil, err
				}
				go func() {
					pServer.Run(shutdown)
					_ = <-shutdown
				}()
				return pServer, nil
			},
			Teardown: func(ctx interface{}) {
				s := ctx.(wso2spadapter.Server)
				s.Close()
			},
			ParallelCalls: []adapter_integration.Call{
				{
					CallKind: adapter_integration.REPORT,
					Attrs:    map[string]interface{}{"request.size": int64(555)},
				},
			},
			GetState: func(ctx interface{}) (interface{}, error) {
				// validate if the content of "out.txt" is as expected
				bytes, err := ioutil.ReadFile("out.txt")
				if err != nil {
					return nil, err
				}
				s := string(bytes)
				wantStr := `HandleMetric invoked with:
       Adapter config: &Params{FilePath:out.txt,}
       Instances: 'wso2spadapter-metric.instance.istio-system':
       {
           Value = 555
           Dimensions = map[response_code:0]
       }
`
				if normalize(s) != normalize(wantStr) {
					return nil, fmt.Errorf("got adapters state as : '%s'; want '%s'", s, wantStr)
				}
				return nil, nil
			},
			GetConfig: func(ctx interface{}) ([]string, error) {
				s := ctx.(wso2spadapter.Server)
				return []string{
					// CRs for built-in templates (metric is what we need for this test)
					// are automatically added by the integration test framework.
					string(adptCrBytes),
					strings.Replace(operatorCfg, "{ADDRESS}", s.Addr(), 1),
				}, nil
			},
			Want: `
     {
      "AdapterState": null,
      "Returns": [
       {
        "Check": {
         "Status": {},
         "ValidDuration": 0,
         "ValidUseCount": 0
        },
        "Quota": null,
        "Error": null
       }
      ]
     }`,
		},
	)
}

func normalize(s string) string {
	s = strings.TrimSpace(s)
	s = strings.Replace(s, "\t", "", -1)
	s = strings.Replace(s, "\n", "", -1)
	s = strings.Replace(s, " ", "", -1)
	return s
}
