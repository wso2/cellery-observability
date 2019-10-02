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

/**
 * Siddhi Store Query Templates Enum class containing all the Siddhi Store Queries.
 * The Siddhi Store Query Builder can be accessed from the Siddhi Store Query Templates.
 */
public enum SiddhiStoreQueryTemplates {

    /*
     * Siddhi Store Queries Start Here
     */

    REQUEST_AGGREGATION_INSTANCES("from RequestAggregation\n" +
            "within ${" + Params.QUERY_START_TIME + "}L, ${" + Params.QUERY_END_TIME + "}L\n" +
            "per \"${" + Params.TIME_GRANULARITY + "}\"\n" +
            "select sourceInstance, sourceInstanceKind, destinationInstance, destinationInstanceKind, " +
            "httpResponseGroup, sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
            "sum(requestCount) as requestCount\n" +
            "group by sourceInstance, destinationInstance, httpResponseGroup"
    ),
    REQUEST_AGGREGATION_INSTANCES_METADATA("from RequestAggregation\n" +
            "within ${" + Params.QUERY_START_TIME + "}L, ${" + Params.QUERY_END_TIME + "}L\n" +
            "per \"seconds\"\n" +
            "select sourceInstance, destinationInstance\n" +
            "group by sourceInstance, destinationInstance"
    ),
    REQUEST_AGGREGATION_INSTANCES_METRICS("from RequestAggregation\n" +
            "on (\"${" + Params.SOURCE_INSTANCE + "}\" == \"\" " +
            "or sourceInstance == \"${" + Params.SOURCE_INSTANCE + "}\") " +
            "and (\"${" + Params.DESTINATION_INSTANCE + "}\" == \"\" " +
            "or destinationInstance == \"${" + Params.DESTINATION_INSTANCE + "}\") " +
            "and (${" + Params.CONDITION + "})\n" +
            "within ${" + Params.QUERY_START_TIME + "}L, ${" + Params.QUERY_END_TIME + "}L\n" +
            "per \"${" + Params.TIME_GRANULARITY + "}\"\n" +
            "select AGG_TIMESTAMP, httpResponseGroup, sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
            "sum(totalRequestSizeBytes) as totalRequestSizeBytes, " +
            "sum(totalResponseSizeBytes) as totalResponseSizeBytes, sum(requestCount) as requestCount\n" +
            "group by AGG_TIMESTAMP, httpResponseGroup"
    ),
    REQUEST_AGGREGATION_INSTANCE_COMPONENTS("from RequestAggregation\n" +
            "on sourceInstance == \"${" + Params.INSTANCE + "}\" " +
            "or destinationInstance == \"${" + Params.INSTANCE + "}\"\n" +
            "within ${" + Params.QUERY_START_TIME + "}L, ${" + Params.QUERY_END_TIME + "}L\n" +
            "per \"${" + Params.TIME_GRANULARITY + "}\"\n" +
            "select sourceInstance, sourceComponent, destinationInstance, destinationComponent, httpResponseGroup, " +
            "sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
            "sum(requestCount) as requestCount\n" +
            "group by sourceInstance, sourceComponent, destinationInstance, destinationComponent, httpResponseGroup"
    ),
    REQUEST_AGGREGATION_COMPONENTS_METADATA("from RequestAggregation\n" +
            "within ${" + Params.QUERY_START_TIME + "}L, ${" + Params.QUERY_END_TIME + "}L\n" +
            "per \"seconds\"\n" +
            "select sourceInstance, sourceComponent, destinationInstance, destinationComponent\n" +
            "group by sourceInstance, sourceComponent, destinationInstance, destinationComponent"
    ),
    REQUEST_AGGREGATION_COMPONENTS_METRICS("from RequestAggregation\n" +
            "on (\"${" + Params.SOURCE_INSTANCE + "}\" == \"\" " +
            "or sourceInstance == \"${" + Params.SOURCE_INSTANCE + "}\") " +
            "and (\"${" + Params.SOURCE_COMPONENT + "}\" == \"\" " +
            "or sourceComponent == \"${" + Params.SOURCE_COMPONENT + "}\") " +
            "and (\"${" + Params.DESTINATION_INSTANCE + "}\" == \"\" " +
            "or destinationInstance == \"${" + Params.DESTINATION_INSTANCE + "}\")\n" +
            "and (\"${" + Params.DESTINATION_COMPONENT + "}\" == \"\" " +
            "or destinationComponent == \"${" + Params.DESTINATION_COMPONENT + "}\") " +
            "and (${" + Params.CONDITION + "})\n" +
            "within ${" + Params.QUERY_START_TIME + "}L, ${" + Params.QUERY_END_TIME + "}L\n" +
            "per \"${" + Params.TIME_GRANULARITY + "}\"\n" +
            "select AGG_TIMESTAMP, httpResponseGroup, sum(totalResponseTimeMilliSec) as totalResponseTimeMilliSec, " +
            "sum(totalRequestSizeBytes) as totalRequestSizeBytes, " +
            "sum(totalResponseSizeBytes) as totalResponseSizeBytes, sum(requestCount) as requestCount\n" +
            "group by AGG_TIMESTAMP, httpResponseGroup"
    ),
    DISTRIBUTED_TRACING_METADATA("from DistributedTracingTable\n" +
            "on (${" + Params.QUERY_START_TIME + "}L == -1L or startTime >= ${" + Params.QUERY_START_TIME + "}L) " +
            "and (${" + Params.QUERY_END_TIME + "}L == -1L or startTime <= ${" + Params.QUERY_END_TIME + "}L)\n" +
            "select instance, serviceName, operationName\n" +
            "group by instance, serviceName, operationName"
    ),
    DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS("from DistributedTracingTable\n" +
            "on (\"${" + Params.INSTANCE + "}\" == \"\" or instance == \"${" + Params.INSTANCE + "}\") " +
            "and (\"${" + Params.SERVICE_NAME + "}\" == \"\" or " +
            "serviceName == \"${" + Params.SERVICE_NAME + "}\") " +
            "and (\"${" + Params.OPERATION_NAME + "}\" == \"\" or " +
            "operationName == \"${" + Params.OPERATION_NAME + "}\") " +
            "and (${" + Params.MIN_DURATION + "}L == -1L or duration >= ${" + Params.MIN_DURATION + "}L)\n" +
            "select traceId\n" +
            "group by traceId\n" +
            "order by startTime desc"
    ),
    DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS_WITH_TAGS("from DistributedTracingTable\n" +
            "on (\"${" + Params.INSTANCE + "}\" == \"\" or instance == \"${" + Params.INSTANCE + "}\") " +
            "and (\"${" + Params.SERVICE_NAME + "}\" == \"\" or " +
            "serviceName == \"${" + Params.SERVICE_NAME + "}\") " +
            "and (\"${" + Params.OPERATION_NAME + "}\" == \"\" or " +
            "operationName == \"${" + Params.OPERATION_NAME + "}\") " +
            "and (${" + Params.MIN_DURATION + "}L == -1L or duration >= ${" + Params.MIN_DURATION + "}L)\n" +
            "select traceId, tags\n" +
            "order by startTime desc"
    ),
    DISTRIBUTED_TRACING_SEARCH_GET_TRACE_IDS_WITH_VALID_ROOT_SPANS("from DistributedTracingTable\n" +
            "on parentId is null " +
            "and (${" + Params.QUERY_START_TIME + "}L == -1L or startTime >= ${" + Params.QUERY_START_TIME + "}L) " +
            "and (${" + Params.QUERY_END_TIME + "}L == -1L or startTime <= ${" + Params.QUERY_END_TIME + "}L)\n" +
            "select traceId\n" +
            "group by traceId\n" +
            "order by startTime desc"
    ),
    DISTRIBUTED_TRACING_SEARCH_GET_ROOT_SPAN_METADATA("from DistributedTracingTable\n" +
            "on parentId is null and (${" + Params.CONDITION + "})\n" +
            "select traceId, instance, serviceName, operationName, startTime, duration\n" +
            "group by traceId\n" +
            "order by startTime desc"
    ),
    DISTRIBUTED_TRACING_SEARCH_GET_MULTIPLE_INSTANCE_SERVICE_COUNTS("from DistributedTracingTable\n" +
            "on ${" + Params.CONDITION + "}\n" +
            "select traceId, instance, serviceName, count() as count\n" +
            "group by traceId, instance, serviceName\n" +
            "order by startTime desc"
    ),
    DISTRIBUTED_TRACING_GET_TRACE("from DistributedTracingTable\n" +
            "on traceId == \"${" + Params.TRACE_ID + "}\"\n" +
            "select traceId, spanId, parentId, namespace, instance, instanceKind, serviceName, pod, operationName, " +
            "spanKind, startTime, duration, tags"
    ),
    K8S_GET_PODS_FOR_COMPONENT("from K8sPodInfoTable\n" +
            "on (\"${" + Params.INSTANCE + "}\" == \"\" or instance == \"${" + Params.INSTANCE + "}\") " +
            "and (\"${" + Params.COMPONENT + "}\" == \"\" or component == \"${" + Params.COMPONENT + "}\") " +
            "and ((${" + Params.QUERY_START_TIME + "}L == -1L and ${" + Params.QUERY_END_TIME + "}L == -1L) " +
            "or ((creationTimestamp >= ${" + Params.QUERY_START_TIME + "}L " +
            "and creationTimestamp <= ${" + Params.QUERY_END_TIME + "}L) " +
            "or (lastKnownAliveTimestamp >= ${" + Params.QUERY_START_TIME + "}L " +
            "and lastKnownAliveTimestamp <= ${" + Params.QUERY_END_TIME + "}L) " +
            "or (creationTimestamp <= ${" + Params.QUERY_START_TIME + "}L " +
            "and lastKnownAliveTimestamp >= ${" + Params.QUERY_END_TIME + "}L)))\n" +
            "select instance, component, podName, creationTimestamp, lastKnownAliveTimestamp, nodeName"
    ),
    K8S_GET_INSTANCES("from K8sComponentInfoTable\n" +
            "on (\"${" + Params.INSTANCE + "}\" == \"\" or instance == \"${" + Params.INSTANCE + "}\") " +
            "and ((${" + Params.QUERY_START_TIME + "}L == -1L and ${" + Params.QUERY_END_TIME + "}L == -1L) " +
            "or ((creationTimestamp >= ${" + Params.QUERY_START_TIME + "}L " +
            "and creationTimestamp <= ${" + Params.QUERY_END_TIME + "}L) " +
            "or (lastKnownAliveTimestamp >= ${" + Params.QUERY_START_TIME + "}L " +
            "and lastKnownAliveTimestamp <= ${" + Params.QUERY_END_TIME + "}L) " +
            "or (creationTimestamp <= ${" + Params.QUERY_START_TIME + "}L " +
            "and lastKnownAliveTimestamp >= ${" + Params.QUERY_END_TIME + "}L)))\n" +
            "select instance, instanceKind\n" +
            "group by instance"
    ),
    K8S_GET_COMPONENTS("from K8sComponentInfoTable\n" +
            "on (\"${" + Params.INSTANCE + "}\" == \"\" or instance == \"${" + Params.INSTANCE + "}\") " +
            "and (\"${" + Params.COMPONENT + "}\" == \"\" or component == \"${" + Params.COMPONENT + "}\") " +
            "and ((${" + Params.QUERY_START_TIME + "}L == -1L and ${" + Params.QUERY_END_TIME + "}L == -1L) " +
            "or ((creationTimestamp >= ${" + Params.QUERY_START_TIME + "}L " +
            "and creationTimestamp <= ${" + Params.QUERY_END_TIME + "}L) " +
            "or (lastKnownAliveTimestamp >= ${" + Params.QUERY_START_TIME + "}L " +
            "and lastKnownAliveTimestamp <= ${" + Params.QUERY_END_TIME + "}L) " +
            "or (creationTimestamp <= ${" + Params.QUERY_START_TIME + "}L " +
            "and lastKnownAliveTimestamp >= ${" + Params.QUERY_END_TIME + "}L)))\n" +
            "select instance, component, instanceKind, ingressTypes\n" +
            "group by instance, component"
    );

    /*
     * Siddhi Store Queries End Here
     */

    private String query;

    SiddhiStoreQueryTemplates(String query) {
        this.query = query;
    }

    /**
     * Parameters to be replaced in the Siddhi Queries.
     */
    public static class Params {
        public static final String QUERY_START_TIME = "queryStartTime";
        public static final String QUERY_END_TIME = "queryEndTime";
        public static final String TIME_GRANULARITY = "timeGranularity";
        public static final String INSTANCE = "instance";
        public static final String COMPONENT = "component";
        public static final String SOURCE_INSTANCE = "sourceInstance";
        public static final String SOURCE_COMPONENT = "sourceComponent";
        public static final String DESTINATION_INSTANCE = "destinationInstance";
        public static final String DESTINATION_COMPONENT = "destinationComponent";
        public static final String CONDITION = "condition";     // Should be used with caution considering SQL injection

        // Tracing specific Params
        public static final String SERVICE_NAME = "serviceName";
        public static final String OPERATION_NAME = "operationName";
        public static final String MIN_DURATION = "minDuration";
        public static final String MAX_DURATION = "maxDuration";
        public static final String TRACE_ID = "traceId";

        private Params() {      // Prevent initialization
        }
    }

    /**
     * Get the build for the query.
     *
     * @return The Siddhi Store Query Builder for the particular query
     */
    public SiddhiStoreQuery.Builder builder() {
        return new SiddhiStoreQuery.Builder(query);
    }
}
