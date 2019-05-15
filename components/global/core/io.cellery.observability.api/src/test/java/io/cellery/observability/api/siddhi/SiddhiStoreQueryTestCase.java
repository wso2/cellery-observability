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
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.event.Event;

/**
 * Test Cases for Siddhi Store Query.
 */
public class SiddhiStoreQueryTestCase {

    @Test
    public void testBuilderInitialization() {
        String query = "test query";
        SiddhiStoreQuery.Builder builder = new SiddhiStoreQuery.Builder(query);
        Assert.assertEquals(Whitebox.getInternalState(builder, "query"), query);
    }

    @Test
    public void testBuilderSetArg() {
        String key = "replaceValue";
        String query = "test query ${" + key + "} query end;";
        SiddhiStoreQuery.Builder builder = new SiddhiStoreQuery.Builder(query)
                .setArg(key, "Test Value 1");
        Assert.assertEquals(Whitebox.getInternalState(builder, "query"),
                "test query Test Value 1 query end;");
    }

    @Test
    public void testBuilderBuild() {
        String key = "testKey";
        SiddhiStoreQuery siddhiStoreQuery = new SiddhiStoreQuery.Builder("test ${" + key + "} query")
                .setArg(key, "Test Value 2")
                .build();
        Assert.assertEquals(Whitebox.getInternalState(siddhiStoreQuery, "query"),
                "test Test Value 2 query");
    }

    @Test
    public void testExecute() {
        Event[] resultantEvents = new Event[]{
                new Event(1, new Object[]{"pet-be", "controller", 15}),
                new Event(2, new Object[]{"pet-be", "orders", 12}),
                new Event(3, new Object[]{"pet-fe", "portal", 142}),
                new Event(4, new Object[]{"pet-fe", "gateway", 123})
        };
        String query = "test query";
        SiddhiStoreQueryManager siddhiStoreQueryManager = Mockito.mock(SiddhiStoreQueryManager.class);
        Mockito.when(siddhiStoreQueryManager.query(query)).thenReturn(resultantEvents);
        ServiceHolder.setSiddhiStoreQueryManager(siddhiStoreQueryManager);

        Object[][] result = new SiddhiStoreQuery.Builder(query)
                .build()
                .execute();

        Assert.assertEquals(result.length, resultantEvents.length);
        for (int i = 0; i < resultantEvents.length; i++) {
            Object[] actualData = result[i];
            Object[] expectedData = resultantEvents[i].getData();
            Assert.assertEquals(actualData.length, expectedData.length);
            for (int j = 0; j < expectedData.length; j++) {
                Assert.assertEquals(actualData[j], expectedData[j]);
            }
        }
    }

    @Test
    public void testExecuteWithSiddhiStoreQueryManagerReturnNull() {
        String query = "test query";
        SiddhiStoreQueryManager siddhiStoreQueryManager = Mockito.mock(SiddhiStoreQueryManager.class);
        Mockito.when(siddhiStoreQueryManager.query(query)).thenReturn(null);
        ServiceHolder.setSiddhiStoreQueryManager(siddhiStoreQueryManager);

        Object[][] result = new SiddhiStoreQuery.Builder(query)
                .build()
                .execute();

        Assert.assertEquals(result.length, 0);
    }

    @Test
    public void testExecuteWithSiddhiStoreQueryManagerReturnEmptyArray() {
        Event[] resultantEvents = new Event[0];
        String query = "test query";
        SiddhiStoreQueryManager siddhiStoreQueryManager = Mockito.mock(SiddhiStoreQueryManager.class);
        Mockito.when(siddhiStoreQueryManager.query(query)).thenReturn(resultantEvents);
        ServiceHolder.setSiddhiStoreQueryManager(siddhiStoreQueryManager);

        Object[][] result = new SiddhiStoreQuery.Builder(query)
                .build()
                .execute();

        Assert.assertEquals(result.length, 0);
    }
}
