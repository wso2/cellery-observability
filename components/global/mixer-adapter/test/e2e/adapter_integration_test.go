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

package e2e

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"
	"testing"

	adapterIntegration "istio.io/istio/mixer/pkg/adapter/test"

	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/adapter"
	"github.com/cellery-io/mesh-observability/components/global/mixer-adapter/pkg/logging"
)

const defaultAdapterPort int = 38355

func TestReport(t *testing.T) {
	adapterBytes, err := ioutil.ReadFile("./testdata/sample-adapter.yaml")
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
	buffer := make(chan string, 100)

	adapterIntegration.RunTest(
		t,
		nil,
		adapterIntegration.Scenario{
			Setup: func() (ctx interface{}, err error) {
				pServer, err := adapter.New(defaultAdapterPort, logger, &http.Client{}, adapter.SPMetricsPublisher{}, nil, "", buffer, false)
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
				s := ctx.(adapter.Server)
				defer func() {
					err := s.Close()
					if err != nil {
						logger.Warn("Could not close the server")
					}
				}()
			},
			ParallelCalls: []adapterIntegration.Call{
				{
					CallKind: adapterIntegration.REPORT,
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
				s := ctx.(adapter.Server)
				return []string{
					// CRs for built-in templates (metric is what we need for this test)
					// are automatically added by the integration test framework.
					string(adapterBytes),
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
