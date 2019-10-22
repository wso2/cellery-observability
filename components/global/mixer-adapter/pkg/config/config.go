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
)

type (
	Config struct {
		Mixer struct {
			TLS struct {
				Certificate   string `json:"certificate"`
				PrivateKey    string `json:"privateKey"`
				CaCertificate string `json:"caCertificate"`
			} `json:"tls"`
		} `json:"mixer"`
		SpEndpoint struct {
			URL                 string `json:"url"`
			SendIntervalSeconds int    `json:"sendIntervalSeconds"`
		} `json:"spEndpoint"`
		Store struct {
			FileStorage struct {
				Path string `json:"path"`
			} `json:"fileStorage"`
			Database struct {
				Host     string `json:"host"`
				Port     int    `json:"port"`
				Protocol string `json:"protocol"`
				Username string `json:"username"`
				Password string `json:"password"`
				Name     string `json:"name"`
			} `json:"database"`
			InMemory struct {
			} `json:"inMemory"`
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
		return nil, fmt.Errorf("could not read the config file : %s", err.Error())
	}
	config := &Config{}
	err = json.Unmarshal(data, &config)
	if err != nil {
		return nil, fmt.Errorf("could not unmarshal the config file : %s", err.Error())
	}
	return config, nil
}
