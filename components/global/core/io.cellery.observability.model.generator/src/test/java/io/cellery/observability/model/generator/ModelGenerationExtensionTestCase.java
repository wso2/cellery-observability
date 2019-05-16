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
 * This test case focuses on the model generation extension initialization
 */
public class ModelGenerationExtensionTestCase {

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithNoParam() {
        initSiddhi("");
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithFirstParamNonString() {
        initSiddhi(getParam(0));
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithSecondParamNonString() {
        initSiddhi(getParam(1));
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithThridParamNonString() {
        initSiddhi(getParam(2));
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithFourthParamNonString() {
        initSiddhi(getParam(3));
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithFifthParamNonString() {
        initSiddhi(getParam(4));
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithSixthParamNonString() {
        initSiddhi(getParam(5));
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithSeventhParamNonString() {
        initSiddhi(getParam(6));
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void initWithSeventhParamNonNumber() {
        initSiddhi(getParam(7));
    }

    private String getParam(int index) {
        String traceId = "traceId";
        String timestamp = "timestamp";
        String param = "";
        for (int i = 0; i < 8; i++) {
            if (i != 7) {
                if (i == index) {
                    param = setParam(param, timestamp);
                } else {
                    param = setParam(param, traceId);
                }
            } else {
                if (i == index) {
                    param = setParam(param, traceId);
                } else {
                    param = setParam(param, timestamp);
                }
            }
        }
        return param;
    }

    private String setParam(String param, String value) {
        if (param.isEmpty()) {
            param += value;
        } else {
            param += "," + value;
        }
        return param;
    }

    private void initSiddhi(String paramString) {
        String query = "define stream ZipkinStreamIn(traceId string, id string, parentId string, name string, " +
                "serviceName string," +
                "kind string, timestamp long, duration long, tags string);\n";
        query += "from ZipkinStreamIn#observe:modelGenerator(" + paramString + ") " +
                "select *\n" +
                "insert into AfterModelGenStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("observe:modelGenerator", ModelGenerationExtension.class);
        siddhiManager.createSiddhiAppRuntime(query);
    }
}
