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

import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;

/**
 * Collection used to manage session windows.
 *
 * @param <E> sub types of ComplexEvent such as StreamEvent and StateEvent.
 */
public class SessionComplexEventChunk<E extends ComplexEvent> extends ComplexEventChunk implements Comparable<SessionComplexEventChunk> {

    private String key;
    private long startTimestamp;
    private long endTimestamp;

    public SessionComplexEventChunk(String key) {
        super(false);
        this.key = key;
    }

    public SessionComplexEventChunk() {
        super(false);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setTimestamps(long startTimestamp, long endTimestamp) {
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }


    @Override
    public int compareTo(SessionComplexEventChunk o) {
        if (o != null) {
            return new Long(startTimestamp - o.startTimestamp).intValue();
        } else {
            return 1;
        }
    }
}
