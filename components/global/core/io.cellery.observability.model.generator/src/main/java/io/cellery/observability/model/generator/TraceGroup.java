/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.cellery.observability.model.generator;

import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;

import java.io.Serializable;

/**
 * This keeps the information of a trace key.
 */
public class TraceGroup implements Serializable, Comparable {
    private String key;
    private long startTimestamp;
    private long endTimestamp;
    private ComplexEventChunk<StreamEvent> currentTraceGroup;

    public TraceGroup(String key, long startTimestamp) {
        currentTraceGroup = new ComplexEventChunk<>(false);
        this.startTimestamp = startTimestamp;
        this.key = key;
    }

    public TraceGroup() {
        currentTraceGroup = new ComplexEventChunk<>(false);
    }

    public String getKey() {
        return key;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public void setStartTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void add(StreamEvent event) {
        this.currentTraceGroup.add(event);
    }

    public boolean isEmpty() {
        return this.currentTraceGroup.getFirst() == null;
    }

    public ComplexEventChunk<StreamEvent> getCurrentTraceGroup() {
        return currentTraceGroup;
    }

    public void clear() {
        this.currentTraceGroup.clear();
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof TraceGroup) {
            return new Long(endTimestamp - ((TraceGroup) o).endTimestamp).intValue();
        } else {
            return 1;
        }
    }
}
