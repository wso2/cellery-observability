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

package io.cellery.observability.telemetry.deduplicator.internal;

/**
 * Telemetry Deduplicator Constants.
 */
public class Constants {
    public static final String MAX_REQUEST_SIZE = "maxRequestSize";
    public static final String MAX_RESPONSE_DURATION = "maxResponseDuration";
    public static final String MAX_RESPONSE_SIZE = "maxResponseSize";

    public static final int TIME_INTERVAL_EXECUTOR_INDEX = 0;
    public static final int RUNTIME_EXECUTOR_INDEX = 1;
    public static final int TRACE_ID_EXECUTOR_INDEX = 1;
    public static final int SPAN_ID_EXECUTOR_INDEX = 2;
    public static final int PARENT_SPAN_ID_EXECUTOR_INDEX = 3;
    public static final int SOURCE_NAMESPACE_EXECUTOR_INDEX = 4;
    public static final int SOURCE_INSTANCE_EXECUTOR_INDEX = 5;
    public static final int SOURCE_COMPONENT_EXECUTOR_INDEX = 6;
    public static final int DESTINATION_NAMESPACE_EXECUTOR_INDEX = 7;
    public static final int DESTINATION_INSTANCE_EXECUTOR_INDEX = 8;
    public static final int DESTINATION_COMPONENT_EXECUTOR_INDEX = 9;
    public static final int REQUEST_SIZE_EXECUTOR_INDEX = 10;
    public static final int RESPONSE_DURATION_EXECUTOR_INDEX = 11;
    public static final int RESPONSE_SIZE_EXECUTOR_INDEX = 12;

    public static final int NUM_OF_PARAMETERS = 13; // Number of parameters in the syntax

    private Constants() {   // Prevent initialization
    }
}
