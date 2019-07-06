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

package io.cellery.observability.k8s.client.crds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;

import javax.validation.Valid;

/**
 * This class implements the Event Source which can be used to listen for k8s cell changes.
 */

@JsonDeserialize
@JsonIgnoreProperties(ignoreUnknown = true)
public class CellImpl extends CustomResource implements Cell {

    @JsonProperty("spec")
    @Valid
    private CellSpec spec;
    private static final long serialVersionUID = 1L;

    public CellSpec getSpec() {
        return spec;
    }

    public void setSpec(CellSpec spec) {
        this.spec = spec;
    }

    public String getKind() {
        return "Cell";
    }

    @Override
    public String toString() {
        return "Cell{" +
                "apiVersion='" + getApiVersion() + '\'' +
                ", metadata=" + getMetadata() +
                ", spec=" + spec +
                '}';
    }
}
