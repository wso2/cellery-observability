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

import io.cellery.observability.api.siddhi.SiddhiStoreQueryTemplates.Params;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test Cases for Siddhi Store Query Templates.
 * These validate whether the templates and setArg methods work as intended.
 */
public class SiddhiStoreQueryTemplatesTestCase {

    @Test
    public void testRequestAggregationCellsTemplate() {
        final long queryStartTime = 13213213;
        final long queryEndTime = 973458743;
        final String timeGranularity = "minutes";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELLS.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"" + timeGranularity + "\"\n" +
                "select sourceCell, destinationCell, httpResponseGroup, " +
                "sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, sum(requestCount) as requestCount\n" +
                "group by sourceCell, destinationCell, httpResponseGroup");
    }

    @Test
    public void testRequestAggregationCellsMetadataTemplate() {
        final long queryStartTime = 345321523;
        final long queryEndTime = 386573257;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELLS_METADATA.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"seconds\"\n" +
                "select sourceCell, destinationCell\n" +
                "group by sourceCell, destinationCell");
    }

    @Test
    public void testRequestAggregationCellsMetricsTemplate() {
        final long queryStartTime = 6784356;
        final long queryEndTime = 83465265;
        final String timeGranularity = "seconds";
        final String sourceCell = "pet-fe";
        final String destinationCell = "pet-be";
        final String condition = "sourceCell != destinationCell";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELLS_METRICS.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .setArg(Params.SOURCE_CELL, sourceCell)
                .setArg(Params.DESTINATION_CELL, destinationCell)
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on (\"" + sourceCell + "\" == \"\" or sourceCell == \"" + sourceCell + "\") and " +
                "(\"" + destinationCell + "\" == \"\" or destinationCell == \"" + destinationCell + "\") " +
                "and (sourceCell != destinationCell)\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"" + timeGranularity + "\"\n" +
                "select AGG_TIMESTAMP, httpResponseGroup, " +
                "sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
                "sum(totalRequestSizeBytes) as totalRequestSizeBytes, " +
                "sum(totalResponseSizeBytes) as totalResponseSizeBytes, sum(requestCount) as requestCount\n" +
                "group by AGG_TIMESTAMP, httpResponseGroup");
    }

    @Test
    public void testRequestAggregationCellComponentsTemplate() {
        final long queryStartTime = 6784356;
        final long queryEndTime = 83465265;
        final String timeGranularity = "hours";
        final String cell = "pet-be";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_CELL_COMPONENTS.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .setArg(Params.CELL, cell)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on sourceCell == \"" + cell + "\" or destinationCell == \"" + cell + "\"\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"" + timeGranularity + "\"\n" +
                "select sourceCell, sourceComponent, destinationCell, destinationComponent, httpResponseGroup, " +
                "sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, sum(requestCount) as requestCount\n" +
                "group by sourceCell, sourceComponent, destinationCell, destinationComponent, httpResponseGroup");
    }

    @Test
    public void testRequestAggregationComponentsMetadataTemplate() {
        final long queryStartTime = 1324234;
        final long queryEndTime = 6234235;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METADATA.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"seconds\"\n" +
                "select sourceCell, sourceComponent, destinationCell, destinationComponent\n" +
                "group by sourceCell, sourceComponent, destinationCell, destinationComponent");
    }

    @Test
    public void testRequestAggregationComponentsMetricsTemplate() {
        final long queryStartTime = 54362342;
        final long queryEndTime = 63452342;
        final String timeGranularity = "seconds";
        final String sourceCell = "pet-fe";
        final String sourceComponent = "portal";
        final String destinationCell = "pet-be";
        final String destinationComponent = "controller";
        final String condition = "sourceCell != destinationCell";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METRICS.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .setArg(Params.SOURCE_CELL, sourceCell)
                .setArg(Params.SOURCE_COMPONENT, sourceComponent)
                .setArg(Params.DESTINATION_CELL, destinationCell)
                .setArg(Params.DESTINATION_COMPONENT, destinationComponent)
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on (\"" + sourceCell + "\" == \"\" or sourceCell == \"" + sourceCell + "\") " +
                "and (\"" + sourceComponent + "\" == \"\" or sourceComponent == \"" + sourceComponent + "\") " +
                "and (\"" + destinationCell + "\" == \"\" or destinationCell == \"" + destinationCell + "\")\n" +
                "and (\"" + destinationComponent + "\" == \"\" or " +
                "destinationComponent == \"" + destinationComponent + "\") and (" + condition + ")\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"seconds\"\n" +
                "select AGG_TIMESTAMP, httpResponseGroup, " +
                "sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
                "sum(totalRequestSizeBytes) as totalRequestSizeBytes, " +
                "sum(totalResponseSizeBytes) as totalResponseSizeBytes, sum(requestCount) as requestCount\n" +
                "group by AGG_TIMESTAMP, httpResponseGroup");
    }

    @Test
    public void testDistributedTracingMetadataTemplate() {
        final long queryStartTime = 74645635;
        final long queryEndTime = 164523652;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.DISTRIBUTED_TRACING_METADATA.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on (" + queryStartTime + "L == -1 or startTime >= " + queryStartTime + "L) " +
                "and (" + queryEndTime + "L == -1 or startTime <= " + queryEndTime + "L)\n" +
                "select cell, serviceName, operationName\n" +
                "group by cell, serviceName, operationName");
    }

    @Test
    public void testDistributedTracingSearchGetTraceIdsTemplate() {
        final long queryStartTime = 234235234;
        final long queryEndTime = 234234124;
        final String cell = "pet-be";
        final String serviceName = "customers";
        final String operationName = "GET /customer/john";
        final long minDuration = 232;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.CELL, cell)
                .setArg(Params.SERVICE_NAME, serviceName)
                .setArg(Params.OPERATION_NAME, operationName)
                .setArg(Params.MIN_DURATION, minDuration)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on (\"" + cell + "\" == \"\" or cell == \"" + cell + "\") and (\"" + serviceName + "\" == \"\" " +
                "or serviceName == \"" + serviceName + "\") and (\"" + operationName + "\" == \"\" " +
                "or operationName == \"" + operationName + "\") and (" + minDuration + "L == -1 " +
                "or duration >= " + minDuration + "L)\n" +
                "select traceId\n" +
                "group by traceId");
    }

    @Test
    public void testDistributedTracingSearchGetTraceIdsWithTagsTemplate() {
        final String cell = "pet-be";
        final String serviceName = "orders";
        final String operationName = "GET /orders/1";
        final long minDuration = 4353;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates
                .DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS_WITH_TAGS.builder()
                .setArg(Params.CELL, cell)
                .setArg(Params.SERVICE_NAME, serviceName)
                .setArg(Params.OPERATION_NAME, operationName)
                .setArg(Params.MIN_DURATION, minDuration)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on (\"pet-be\" == \"\" or cell == \"pet-be\") and " +
                "(\"orders\" == \"\" or serviceName == \"orders\") and " +
                "(\"GET /orders/1\" == \"\" or operationName == \"GET /orders/1\") and " +
                "(4353L == -1 or duration >= 4353L)\n" +
                "select traceId, tags");
    }

    @Test
    public void testDistributedTracingSearchGetRootSpanMetadataTemplate() {
        final long queryStartTime = 21234322;
        final long queryEndTime = 243423;
        final long maxDuration = 4323453;
        final String condition = "traceId=\"342fsd23423\" or traceId=\"4ger435f324\"";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates
                .DISTRIBUTED_TRACING_SEARCH_GET_ROOT_SPAN_METADATA.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.MAX_DURATION, maxDuration)
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on traceId == spanId and (traceId=\"342fsd23423\" or traceId=\"4ger435f324\") and " +
                "(4323453L == -1 or duration <= 4323453L) and (21234322L == -1 or startTime >= 21234322L) " +
                "and (243423L == -1 or startTime <= 243423L)\n" +
                "select traceId, cell, serviceName, " +
                "operationName, startTime, duration\n" +
                "order by startTime desc");
    }

    @Test
    public void testDistributedTracingSearchGetMultipleCellServiceCountsTemplate() {
        final String condition = "traceId=\"wtg345feg\" or traceId=\"54gdf245g\"";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates
                .DISTRIBUTED_TRACING_SEARCH_GET_MULTIPLE_CELL_SERVICE_COUNTS.builder()
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on " + condition + "\n" +
                "select traceId, cell, serviceName, count() as count\n" +
                "group by traceId, cell, serviceName\n" +
                "order by startTime desc");
    }

    @Test
    public void testDistributedTracingGetTraceTemplate() {
        final String traceId = "sfg43kjs5dr";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.DISTRIBUTED_TRACING_GET_TRACE.builder()
                .setArg(Params.TRACE_ID, traceId)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on traceId == \"" + traceId + "\"\n" +
                "select traceId, spanId, parentId, namespace, cell, serviceName, pod, operationName, kind, " +
                "startTime, duration, tags");
    }

    @Test
    public void testK8sGetPodsForComponentTemplate() {
        final long queryStartTime = 21234322;
        final long queryEndTime = 243423;
        final String cell = "pet-be";
        final String component = "catalog";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.K8S_GET_PODS_FOR_COMPONENT.builder()
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.CELL, cell)
                .setArg(Params.COMPONENT, component)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from K8sPodInfoTable\n" +
                "on (\"" + cell + "\" == \"\" or cell == \"" + cell + "\") and (\"" + component + "\" == \"\" or " +
                "component == \"catalog\") and ((creationTimestamp >= 21234322L and " +
                "creationTimestamp <= 243423L) or (lastKnownAliveTimestamp >= 21234322L and " +
                "lastKnownAliveTimestamp <= 243423L) or (creationTimestamp <= 21234322L and " +
                "lastKnownAliveTimestamp >= 243423L))\n" +
                "select cell, component, name, creationTimestamp, lastKnownAliveTimestamp, nodeName");
    }
}
