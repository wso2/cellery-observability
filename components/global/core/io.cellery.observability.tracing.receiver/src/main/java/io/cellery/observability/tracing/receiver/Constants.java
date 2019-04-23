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

package io.cellery.observability.tracing.receiver;

/**
 * Tracing Receiver Constants.
 */
public class Constants {
    public static final String HTTP_CONTENT_TYPE_HEADER = "Content-Type";
    public static final String HTTP_APPLICATION_THRIFT_CONTENT_TYPE = "application/x-thrift";

    public static final String TRACING_RECEIVER_HOST_KEY = "host";
    public static final String TRACING_RECEIVER_PORT_KEY = "port";
    public static final String TRACING_RECEIVER_API_CONTEXT_KEY = "api.context";

    public static final String DEFAULT_TRACING_RECEIVER_IP = "0.0.0.0";
    public static final String DEFAULT_TRACING_RECEIVER_PORT = "9411";
    public static final String DEFAULT_TRACING_RECEIVER_API_CONTEXT = "/api/v1/spans";

    // Zipkin fields
    public static final String ATTRIBUTE_TRACE_ID = "traceId";
    public static final String ATTRIBUTE_SPAN_ID = "id";
    public static final String ATTRIBUTE_PARENT_ID = "parentId";
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_SERVICE_NAME = "serviceName";
    public static final String ATTRIBUTE_KIND = "kind";
    public static final String ATTRIBUTE_TIMESTAMP = "timestamp";
    public static final String ATTRIBUTE_DURATION = "duration";
    public static final String ATTRIBUTE_TAGS = "tags";

    // Span kinds
    public static final String CLIENT_SPAN_KIND = "CLIENT";
    public static final String SERVER_SPAN_KIND = "SERVER";
    public static final String PRODUCER_SPAN_KIND = "PRODUCER";
    public static final String CONSUMER_SPAN_KIND = "CONSUMER";

    // Thrift span annotation values
    public static final String THRIFT_SPAN_ANNOTATION_VALUE_CLIENT_SEND = "cs";
    public static final String THRIFT_SPAN_ANNOTATION_VALUE_CLIENT_RECEIVE = "cr";
    public static final String THRIFT_SPAN_ANNOTATION_VALUE_SERVER_SEND = "ss";
    public static final String THRIFT_SPAN_ANNOTATION_VALUE_SERVER_RECEIVE = "sr";
    public static final String THRIFT_SPAN_ANNOTATION_VALUE_PRODUCER_SEND = "ms";
    public static final String THRIFT_SPAN_ANNOTATION_VALUE_CONSUMER_RECEIVER = "wr";

    private Constants() {   // Prevent initialization
    }
}
