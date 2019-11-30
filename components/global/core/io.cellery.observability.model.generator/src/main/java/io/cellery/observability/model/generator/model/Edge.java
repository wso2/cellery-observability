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
 * Represents an edge in the dependency model.
 */
public class Edge {
    private EdgeNode source;
    private EdgeNode target;

    public Edge(EdgeNode source, EdgeNode target) {
        this.source = source;
        this.target = target;
    }

    public EdgeNode getSource() {
        return source;
    }

    public EdgeNode getTarget() {
        return target;
    }

    public boolean equals(Object anotherObject) {
        boolean equals;
        if (anotherObject instanceof Edge) {
            Edge anotherEdge = (Edge) anotherObject;
            equals = Objects.equals(this.source, anotherEdge.getSource())
                    && Objects.equals(this.target, anotherEdge.getTarget());
        } else {
            equals = false;
        }
        return equals;
    }

    public int hashCode() {
        return Objects.hash(this.source, this.target);
    }

    public String toString() {
        return this.source + " --> " + this.target;
    }
}
