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

package config

import (
	"encoding/json"
	"fmt"
	"io/ioutil"

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store/database"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store/file"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/store/memory"

	"cellery.io/cellery-observability/components/global/observability-agent/pkg/adapter"
	"cellery.io/cellery-observability/components/global/observability-agent/pkg/publisher"
)

type (
	Config struct {
		adapter.Mixer        `json:"mixer"`
		publisher.SpEndpoint `json:"spEndpoint"`
		Store                struct {
			*file.File         `json:"fileStorage"`
			*database.Database `json:"database"`
			*memory.Memory     `json:"inMemory"`
		} `json:"store"`
		Advanced struct {
			MaxRecordsForSingleWrite int `json:"maxRecordsForSingleWrite"`
			BufferSizeFactor         int `json:"bufferSizeFactor"`
			BufferTimeoutSeconds     int `json:"bufferTimeoutSeconds"`
		} `json:"advanced"`
	}
)

func New(configFilePath string) (*Config, error) {
	data, err := ioutil.ReadFile(configFilePath)
	if err != nil {
		return nil, fmt.Errorf("could not read the config file : %v", err)
	}
	config := &Config{}
	err = json.Unmarshal(data, config)
	if err != nil {
		return nil, fmt.Errorf("could not unmarshal the config file : %v", err)
	}
	return config, nil
}
