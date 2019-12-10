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

package io.cellery.observability.api;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import javax.net.ssl.HttpsURLConnection;

/**
 * Test Cases for API Utils.
 */
@PrepareForTest(HttpsURLConnection.class)
@PowerMockIgnore({"org.apache.log4j.*", "javax.net.ssl.*"})
public class UtilsTestCase {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void testGenerateSiddhiMatchConditionForAnyValues() {
        String condition = Utils.generateSiddhiMatchConditionForMultipleValues(
                "testAttr",
                new String[]{"value1", "value2", "value2b"}
        );
        Assert.assertEquals(condition, "testAttr == \"value1\" or testAttr == \"value2\" " +
                "or testAttr == \"value2b\"");
    }

    @Test
    public void testGenerateSiddhiMatchConditionForAnyValuesWithEmptyArray() {
        String condition = Utils.generateSiddhiMatchConditionForMultipleValues("testAttr", new String[]{});
        Assert.assertEquals(condition, "");
    }
}
