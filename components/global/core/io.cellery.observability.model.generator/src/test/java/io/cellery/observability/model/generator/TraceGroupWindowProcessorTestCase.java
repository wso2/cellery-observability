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
package io.cellery.observability.model.generator;

import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;

/**
 * This test case validates test group window processor initialization.
 */
public class TraceGroupWindowProcessorTestCase {

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initializeWithNoAttribute() {
        initSiddhi("");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initializeWithFirstParamNonConstantAttribute() {
        initSiddhi("traceId, traceId");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initializeWithFirstParamStringConstAttribute() {
        initSiddhi("'test', traceId");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initializeWithSecondConstantAttribute() {
        initSiddhi("5 sec, 'test'");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initializeWithSecondNonStringAttribute() {
        initSiddhi("5 sec, timestamp");
    }

    private void initSiddhi(String paramString) {
        String query = "define stream ZipkinStreamIn(traceId string, id string, parentId string, name string, " +
                "serviceName string," +
                "kind string, timestamp long, duration long, tags string);\n";
        query += "from ZipkinStreamIn#observe:traceGroupWindow(" + paramString + ") " +
                "select *\n" +
                "insert into AfterTraceGroupStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("observe:traceGroupWindow", TraceGroupWindowProcessor.class);
        siddhiManager.createSiddhiAppRuntime(query);
    }
}
