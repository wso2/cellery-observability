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
import org.wso2.siddhi.query.compiler.SiddhiCompiler;

/**
 * Test Cases for Siddhi Store Query Templates.
 * These validate whether the templates and setArg methods work as intended.
 */
public class SiddhiStoreQueryTemplatesTestCase {

    @Test
    public void testRequestAggregationInstancesTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 13213213;
        final long queryEndTime = 973458743;
        final String timeGranularity = "minutes";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCES.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on runtime == \"" + runtime + "\" and sourceNamespace == \"" + namespace + "\" " +
                "and destinationNamespace == \"" + namespace +  "\"\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"" + timeGranularity + "\"\n" +
                "select sourceInstance, sourceInstanceKind, destinationInstance, destinationInstanceKind, " +
                "httpResponseGroup, sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
                "sum(requestCount) as requestCount\n" +
                "group by sourceInstance, destinationInstance, httpResponseGroup");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testRequestAggregationInstancesMetadataTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 345321523;
        final long queryEndTime = 386573257;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCES_METADATA.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on runtime == \"" + runtime + "\" and sourceNamespace == \"" + namespace + "\" " +
                "and destinationNamespace == \"" + namespace +  "\"\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"seconds\"\n" +
                "select sourceInstance, destinationInstance\n" +
                "group by sourceInstance, destinationInstance");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testRequestAggregationInstancesMetricsTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 6784356;
        final long queryEndTime = 83465265;
        final String timeGranularity = "seconds";
        final String sourceInstance = "pet-fe";
        final String destinationInstance = "pet-be";
        final String condition = "sourceInstance != destinationInstance";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCES_METRICS.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .setArg(Params.SOURCE_INSTANCE, sourceInstance)
                .setArg(Params.DESTINATION_INSTANCE, destinationInstance)
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on runtime == \"" + runtime + "\" and sourceNamespace == \"" + namespace + "\" " +
                "and destinationNamespace == \"" + namespace +  "\" " +
                "and (\"" + sourceInstance + "\" == \"\" or sourceInstance == \"" + sourceInstance + "\") and " +
                "(\"" + destinationInstance + "\" == \"\" or destinationInstance == \"" + destinationInstance + "\") " +
                "and (sourceInstance != destinationInstance)\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"" + timeGranularity + "\"\n" +
                "select AGG_TIMESTAMP, httpResponseGroup, " +
                "sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
                "sum(totalRequestSizeBytes) as totalRequestSizeBytes, " +
                "sum(totalResponseSizeBytes) as totalResponseSizeBytes, sum(requestCount) as requestCount\n" +
                "group by AGG_TIMESTAMP, httpResponseGroup");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testRequestAggregationInstanceComponentsTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 6784356;
        final long queryEndTime = 83465265;
        final String timeGranularity = "hours";
        final String instance = "pet-be";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_INSTANCE_COMPONENTS.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .setArg(Params.INSTANCE, instance)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on runtime == \"" + runtime + "\" and sourceNamespace == \"" + namespace + "\" " +
                "and destinationNamespace == \"" + namespace +  "\" " +
                "and (sourceInstance == \"" + instance + "\" or destinationInstance == \"" + instance + "\")\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"" + timeGranularity + "\"\n" +
                "select sourceInstance, sourceComponent, destinationInstance, destinationComponent, " +
                "httpResponseGroup, sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
                "sum(requestCount) as requestCount\n" +
                "group by sourceInstance, sourceComponent, destinationInstance, destinationComponent, " +
                "httpResponseGroup");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testRequestAggregationComponentsMetadataTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 1324234;
        final long queryEndTime = 6234235;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METADATA.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on runtime == \"" + runtime + "\" and sourceNamespace == \"" + namespace + "\" " +
                "and destinationNamespace == \"" + namespace +  "\"\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"seconds\"\n" +
                "select sourceInstance, sourceComponent, destinationInstance, destinationComponent\n" +
                "group by sourceInstance, sourceComponent, destinationInstance, destinationComponent");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testRequestAggregationComponentsMetricsTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 54362342;
        final long queryEndTime = 63452342;
        final String timeGranularity = "seconds";
        final String sourceInstance = "pet-fe";
        final String sourceComponent = "portal";
        final String destinationInstance = "pet-be";
        final String destinationComponent = "controller";
        final String condition = "sourceInstance != destinationInstance";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.REQUEST_AGGREGATION_COMPONENTS_METRICS.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.TIME_GRANULARITY, timeGranularity)
                .setArg(Params.SOURCE_INSTANCE, sourceInstance)
                .setArg(Params.SOURCE_COMPONENT, sourceComponent)
                .setArg(Params.DESTINATION_INSTANCE, destinationInstance)
                .setArg(Params.DESTINATION_COMPONENT, destinationComponent)
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from RequestAggregation\n" +
                "on runtime == \"" + runtime + "\" and sourceNamespace == \"" + namespace + "\" " +
                "and destinationNamespace == \"" + namespace +  "\" " +
                "and (\"" + sourceInstance + "\" == \"\" or sourceInstance == \"" + sourceInstance + "\") " +
                "and (\"" + sourceComponent + "\" == \"\" or sourceComponent == \"" + sourceComponent + "\") " +
                "and (\"" + destinationInstance + "\" == \"\" " +
                "or destinationInstance == \"" + destinationInstance + "\")\n" +
                "and (\"" + destinationComponent + "\" == \"\" or " +
                "destinationComponent == \"" + destinationComponent + "\") and (" + condition + ")\n" +
                "within " + queryStartTime + "L, " + queryEndTime + "L\n" +
                "per \"seconds\"\n" +
                "select AGG_TIMESTAMP, httpResponseGroup, " +
                "sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
                "sum(totalRequestSizeBytes) as totalRequestSizeBytes, " +
                "sum(totalResponseSizeBytes) as totalResponseSizeBytes, sum(requestCount) as requestCount\n" +
                "group by AGG_TIMESTAMP, httpResponseGroup");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testDistributedTracingMetadataTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 74645635;
        final long queryEndTime = 164523652;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.DISTRIBUTED_TRACING_METADATA.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on runtime == \"" + runtime + "\" and namespace == \"" + namespace + "\" " +
                "and (" + queryStartTime + "L == -1L or startTime >= " + queryStartTime + "L) " +
                "and (" + queryEndTime + "L == -1L or startTime <= " + queryEndTime + "L)\n" +
                "select instance, serviceName, operationName\n" +
                "group by instance, serviceName, operationName");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testDistributedTracingSearchGetTraceIdsTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 243423;
        final long queryEndTime = 21234322;
        final String instance = "pet-be";
        final String serviceName = "customers";
        final String operationName = "GET /customer/john";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(Params.INSTANCE, instance)
                .setArg(Params.SERVICE_NAME, serviceName)
                .setArg(Params.OPERATION_NAME, operationName)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on runtime == \"" + runtime + "\" and namespace == \"" + namespace + "\" " +
                "and (\"" + instance + "\" == \"\" or instance == \"" + instance + "\") " +
                "and (\"" + serviceName + "\" == \"\" or serviceName == \"" + serviceName + "\") " +
                "and (\"" + operationName + "\" == \"\" or operationName == \"" + operationName + "\")\n" +
                "select traceId\n" +
                "group by traceId\n" +
                "order by startTime desc");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testDistributedTracingSearchGetTraceIdsWithTagsTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final String instance = "pet-be";
        final String serviceName = "orders";
        final String operationName = "GET /orders/1";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates
                .DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS_WITH_TAGS.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.INSTANCE, instance)
                .setArg(Params.SERVICE_NAME, serviceName)
                .setArg(Params.OPERATION_NAME, operationName)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on runtime == \"" + runtime + "\" and namespace == \"" + namespace + "\" " +
                "and (\"" + instance + "\" == \"\" or instance == \"" + instance + "\") and " +
                "(\"" + serviceName + "\" == \"\" or serviceName == \"" + serviceName + "\") and " +
                "(\"" + operationName + "\" == \"\" or operationName == \"" + operationName + "\")\n" +
                "select traceId, tags\n" +
                "order by startTime desc");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testDistributedTracingSearchGetTraceIdsWithValidRootSpans() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final long queryStartTime = 243423;
        final long queryEndTime = 21234322;
        final long minDuration = 12321312;
        final long maxDuration = 12321;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates
                .DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS_WITH_VALID_ROOT_SPANS.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .setArg(SiddhiStoreQueryTemplates.Params.MIN_DURATION, minDuration)
                .setArg(SiddhiStoreQueryTemplates.Params.MAX_DURATION, maxDuration)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on runtime == \"" + runtime + "\" and namespace == \"" + namespace + "\" " +
                "and parentId is null " +
                "and (" + queryStartTime + "L == -1L or startTime >= " + queryStartTime + "L) " +
                "and (" + queryEndTime + "L == -1L or startTime <= " + queryEndTime + "L) " +
                "and (" + minDuration + "L == -1L or duration >= " + minDuration + "L) " +
                "and (" + maxDuration + "L == -1L or duration <= " + maxDuration + "L)\n" +
                "select traceId\n" +
                "group by traceId\n" +
                "order by startTime desc");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testDistributedTracingSearchGetRootSpanMetadataTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final String condition = "traceId == \"342fsd23423\" or traceId == \"4ger435f324\"";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates
                .DISTRIBUTED_TRACING_SEARCH_GET_ROOT_SPAN_METADATA.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on runtime == \"" + runtime + "\" and namespace == \"" + namespace + "\" " +
                "and parentId is null and (" + condition + ")\n" +
                "select traceId, instance, serviceName, operationName, startTime, duration\n" +
                "group by traceId\n" +
                "order by startTime desc");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testDistributedTracingSearchGetMultipleInstanceServiceCountsTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final String condition = "traceId == \"wtg345feg\" or traceId == \"54gdf245g\"";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates
                .DISTRIBUTED_TRACING_SEARCH_GET_MULTIPLE_INSTANCE_SERVICE_COUNTS.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.CONDITION, condition)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on runtime == \"" + runtime + "\" " +
                "and (" + condition + ")\n" +
                "select traceId, instance, serviceName, count() as count\n" +
                "group by traceId, instance, serviceName\n" +
                "order by startTime desc");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testDistributedTracingGetTraceTemplate() {
        final String runtime = "test-runtime";
        final String namespace = "test-namespace";
        final String traceId = "sfg43kjs5dr";

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.DISTRIBUTED_TRACING_GET_TRACE.builder()
                .setArg(Params.RUNTIME, runtime)
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.TRACE_ID, traceId)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from DistributedTracingTable\n" +
                "on runtime == \"" + runtime + "\" " +
                "and traceId == \"" + traceId + "\"\n" +
                "select traceId, spanId, parentId, namespace, instance, instanceKind, serviceName, pod, " +
                "operationName, spanKind, startTime, duration, tags");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testK8sGetPodsForComponentTemplate() {
        final String namespace = "test-namespace";
        final String instance = "pet-be";
        final String component = "catalog";
        final long queryStartTime = 243423;
        final long queryEndTime = 21234322;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.K8S_GET_PODS_FOR_COMPONENT.builder()
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.INSTANCE, instance)
                .setArg(Params.COMPONENT, component)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from K8sPodInfoTable\n" +
                "on (\"" + namespace + "\" == \"\" or namespace == \"" + namespace + "\") " +
                "and (\"" + instance + "\" == \"\" or instance == \"" + instance + "\") " +
                "and (\"" + component + "\" == \"\" or component == \"catalog\") " +
                "and ((" + queryStartTime + "L == -1L and " + queryEndTime + "L == -1L) " +
                "or ((creationTimestamp >= " + queryStartTime + "L " +
                "and creationTimestamp <= " + queryEndTime + "L) " +
                "or (lastKnownAliveTimestamp >= " + queryStartTime + "L " +
                "and lastKnownAliveTimestamp <= " + queryEndTime + "L) " +
                "or (creationTimestamp <= " + queryStartTime + "L " +
                "and lastKnownAliveTimestamp >= " + queryEndTime + "L)))\n" +
                "select instance, component, podName, creationTimestamp, lastKnownAliveTimestamp, nodeName");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testK8sGetInstancesTemplate() {
        final String namespace = "test-namespace";
        final String instance = "pet-be";
        final long queryStartTime = 243423;
        final long queryEndTime = 21234322;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.K8S_GET_INSTANCES.builder()
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.INSTANCE, instance)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from K8sComponentInfoTable\n" +
                "on (\"" + namespace + "\" == \"\" or namespace == \"" + namespace + "\") " +
                "and (\"" + instance + "\" == \"\" or instance == \"" + instance + "\") " +
                "and ((" + queryStartTime + "L == -1L and " + queryEndTime + "L == -1L) " +
                "or ((creationTimestamp >= " + queryStartTime + "L " +
                "and creationTimestamp <= " + queryEndTime + "L) " +
                "or (lastKnownAliveTimestamp >= " + queryStartTime + "L " +
                "and lastKnownAliveTimestamp <= " + queryEndTime + "L) " +
                "or (creationTimestamp <= " + queryStartTime + "L " +
                "and lastKnownAliveTimestamp >= " + queryEndTime + "L)))\n" +
                "select instance, instanceKind\n" +
                "group by instance");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }

    @Test
    public void testK8sGetComponentsTemplate() {
        final String namespace = "test-namespace";
        final String instance = "pet-be";
        final String component = "catalog";
        final long queryStartTime = 243423;
        final long queryEndTime = 21234322;

        SiddhiStoreQuery siddhiStoreQuery = SiddhiStoreQueryTemplates.K8S_GET_COMPONENTS.builder()
                .setArg(Params.NAMESPACE, namespace)
                .setArg(Params.INSTANCE, instance)
                .setArg(Params.COMPONENT, component)
                .setArg(Params.QUERY_START_TIME, queryStartTime)
                .setArg(Params.QUERY_END_TIME, queryEndTime)
                .build();
        String resultantQuery = Whitebox.getInternalState(siddhiStoreQuery, "query");

        Assert.assertEquals(resultantQuery, "from K8sComponentInfoTable\n" +
                "on (\"" + namespace + "\" == \"\" or namespace == \"" + namespace + "\") " +
                "and (\"" + instance + "\" == \"\" or instance == \"" + instance + "\") " +
                "and (\"" + component + "\" == \"\" or component == \"" + component + "\") " +
                "and ((" + queryStartTime + "L == -1L and " + queryEndTime + "L == -1L) " +
                "or ((creationTimestamp >= " + queryStartTime + "L " +
                "and creationTimestamp <= " + queryEndTime + "L) " +
                "or (lastKnownAliveTimestamp >= " + queryStartTime + "L " +
                "and lastKnownAliveTimestamp <= " + queryEndTime + "L) " +
                "or (creationTimestamp <= " + queryStartTime + "L " +
                "and lastKnownAliveTimestamp >= " + queryEndTime + "L)))\n" +
                "select instance, component, instanceKind, ingressTypes\n" +
                "group by instance, component");
        SiddhiCompiler.parseStoreQuery(resultantQuery);
    }
}
