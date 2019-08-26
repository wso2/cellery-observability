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
	"os"

	"github.com/wso2-cellery/mesh-observability/components/global/mixer-adapter/pkg/logging"

	wso2spadapter "github.com/wso2-cellery/mesh-observability/components/global/mixer-adapter/pkg/wso2spadapter"

)

const defaultAdapterPort string = "38355"

func main() {

	addr := defaultAdapterPort //Pre defined port for the adaptor. ToDo: Should get this as an environment variable

	if len(os.Args) > 1 {
		addr = os.Args[1]
	}

	logger, err := logging.NewLogger()
	if err != nil {
		logger.Fatalf("Error building logger: ", err.Error())
	}
	defer logger.Sync()

	adapter, err := wso2spadapter.NewWso2SpAdapter(addr, logger)
	if err != nil {
		logger.Info("unable to start server: ", err)
		os.Exit(-1)
	}

	shutdown := make(chan error, 1)
	go func() {
		adapter.Run(shutdown)
	}()
	_ = <-shutdown
}
