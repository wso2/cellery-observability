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

package io.cellery.observability.api.siddhi;

import io.cellery.observability.api.internal.ServiceHolder;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.reflect.Whitebox;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;

/**
 * Test Cases for Siddhi Store Query Manager.
 */
@PrepareForTest(SiddhiStoreQueryManager.class)
@PowerMockIgnore("org.apache.log4j.*")
public class SiddhiStoreQueryManagerTestCase {

    private SiddhiStoreQueryManager siddhiStoreQueryManager;
    private SiddhiManager siddhiManager;
    private SiddhiAppRuntime internalSiddhiAppRuntime;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @BeforeMethod
    public void init() {
        String siddhiAppString = Whitebox.getInternalState(SiddhiStoreQueryManager.class, "SIDDHI_APP");
        internalSiddhiAppRuntime = Mockito.mock(SiddhiAppRuntime.class);

        siddhiManager = Mockito.mock(SiddhiManager.class);
        Mockito.when(siddhiManager.createSiddhiAppRuntime(siddhiAppString))
                .thenReturn(internalSiddhiAppRuntime);
        ServiceHolder.setSiddhiManager(siddhiManager);

        siddhiStoreQueryManager = new SiddhiStoreQueryManager();
    }

    @Test
    public void testInitialization() throws Exception {
        String siddhiAppString = Whitebox.getInternalState(SiddhiStoreQueryManager.class, "SIDDHI_APP");
        Mockito.verify(siddhiManager, Mockito.times(1))
                .createSiddhiAppRuntime(siddhiAppString);
        Mockito.verify(internalSiddhiAppRuntime, Mockito.times(1)).start();
    }

    @Test
    public void testQuery() {
        siddhiStoreQueryManager.query("test");
        Mockito.verify(internalSiddhiAppRuntime, Mockito.times(1)).query("test");
    }

    @Test
    public void testShutdown() {
        siddhiStoreQueryManager.stop();
        Mockito.verify(internalSiddhiAppRuntime, Mockito.times(1)).shutdown();
    }
}
