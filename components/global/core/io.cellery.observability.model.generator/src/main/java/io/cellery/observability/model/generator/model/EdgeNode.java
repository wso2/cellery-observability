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

package io.cellery.observability.model.generator.model;

import java.util.Objects;

/**
 * Represents the node (source or target) in a edge.
 */
public class EdgeNode {
    private String runtime;
    private String namespace;
    private String instance;
    private String component;

    public EdgeNode(String runtime, String namespace, String instance, String component) {
        this.runtime = runtime;
        this.namespace = namespace;
        this.instance = instance;
        this.component = component;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getInstance() {
        return instance;
    }

    public String getComponent() {
        return component;
    }

    public String getFQN() {
        return Model.getNodeFQN(this);
    }

    public boolean equals(Object anotherObject) {
        boolean equals;
        if (anotherObject instanceof EdgeNode) {
            EdgeNode anotherEdgeNode = (EdgeNode) anotherObject;
            equals = Objects.equals(this.runtime, anotherEdgeNode.getRuntime())
                    && Objects.equals(this.namespace, anotherEdgeNode.getNamespace())
                    && Objects.equals(this.instance, anotherEdgeNode.getInstance())
                    && Objects.equals(this.component, anotherEdgeNode.getComponent());
        } else {
            equals = false;
        }
        return equals;
    }

    public int hashCode() {
        return Objects.hash(this.runtime, this.namespace, this.instance, this.component);
    }

    public String toString() {
        return this.getFQN();
    }
}
