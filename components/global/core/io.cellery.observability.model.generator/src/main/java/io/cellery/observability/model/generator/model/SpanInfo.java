/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package io.cellery.observability.model.generator.model;

/**
 * This is the reference of the span instance in the request tree
 */
public class SpanInfo implements Comparable {
    private String cellName;
    private String componentName;
    private String operationName;
    private Kind kind;
    private String spanId;
    private String parentId;
    private long startTime;

    public SpanInfo(String cellName, String componentName, String operationName, String spanId, String parentId,
                    Kind kind, long startTime) {
        this.cellName = cellName;
        this.componentName = componentName;
        this.operationName = operationName;
        this.spanId = spanId;
        this.parentId = parentId;
        this.kind = kind;
        this.startTime = startTime;
    }

    public String getCellName() {
        return cellName;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getSpanId() {
        return spanId;
    }

    public String getParentId() {
        return parentId;
    }

    public Kind getKind() {
        return kind;
    }

    public long getStartTime() {
        return startTime;
    }

    @Override
    public int compareTo(Object o) {
        return Long.compare(this.startTime, ((SpanInfo) (o)).startTime);
    }

    /**
     * This enum represents the Kind of Spans
     */
    public enum Kind {
        SERVER, CLIENT, NONE
    }
}
