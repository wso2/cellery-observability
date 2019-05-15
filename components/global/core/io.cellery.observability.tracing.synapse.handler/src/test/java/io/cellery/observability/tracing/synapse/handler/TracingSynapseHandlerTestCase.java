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

package io.cellery.observability.tracing.synapse.handler;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import brave.propagation.B3Propagation;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Test Cases for Tracing Synapse Handler.
 */
@PrepareForTest(URLConnectionSender.class)
@PowerMockIgnore("org.apache.log4j.*")
public class TracingSynapseHandlerTestCase {

    private static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";
    private static final String B3_SPAN_ID_HEADER = "X-B3-SpanId";
    private static final String B3_PARENT_SPAN_ID_HEADER = "X-B3-ParentSpanId";
    private static final String B3_SAMPLED_HEADER = "X-B3-Sampled";

    private Tracer tracer;
    private List<zipkin2.Span> reportedSpans;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @BeforeMethod
    public void init() {
        reportedSpans = new ArrayList<>();
        Tracing braveTracing = Tracing.newBuilder()
                .localServiceName(Constants.GLOBAL_GATEWAY_SERVICE_NAME)
                .spanReporter(reportedSpans::add)
                .build();
        tracer = BraveTracer.newBuilder(braveTracing)
                .textMapPropagation(Format.Builtin.HTTP_HEADERS, B3Propagation.B3_STRING)
                .build();
    }

    @Test
    public void testInitializeWithEnvironment() {
        PowerMockito.spy(URLConnectionSender.class);

        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();

        PowerMockito.verifyStatic(URLConnectionSender.class);
        URLConnectionSender.create("http://wso2sp-worker.cellery-system:9411/api/v1/spans");
        Assert.assertNotNull(Whitebox.getInternalState(tracingSynapseHandler, "tracer"));
    }

    @Test
    public void testFullSinglePass() {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Whitebox.setInternalState(tracingSynapseHandler, "tracer", tracer);
        Map<String, Stack<Span>> spansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");

        String correlationId;
        // Request In
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(new HashMap<>());
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-1-in");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("GET");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_REMOTE_HOST))
                    .thenReturn("192.168.12.100");

            String[] correlationIdHolder = new String[1];
            Mockito.doAnswer(invocation -> correlationIdHolder[0] = invocation.getArgumentAt(1, String.class))
                    .when(synapseAxis2MessageContext)
                    .setProperty(Mockito.eq(Constants.TRACING_CORRELATION_ID), Mockito.anyString());

            boolean continueFlow = tracingSynapseHandler.handleRequestInFlow(synapseAxis2MessageContext);
            correlationId = correlationIdHolder[0];

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertNotNull(correlationId);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 1);
        }
        // Request Out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(new HashMap<>());
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-1-out");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("GET");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_ENDPOINT))
                    .thenReturn("destination-service-1");
            Mockito.when(synapseAxis2MessageContext.getProperty(
                    Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_PEER_ADDRESS))
                    .thenReturn("192.168.12.104");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_TRANSPORT))
                    .thenReturn("http");

            boolean continueFlow = tracingSynapseHandler.handleRequestOutFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 2);
        }
        // Response in
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(200);

            boolean continueFlow = tracingSynapseHandler.handleResponseInFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 1);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 1);
        }
        // Response out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(200);

            boolean continueFlow = tracingSynapseHandler.handleResponseOutFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 2);
            Assert.assertEquals(spansMap.size(), 0);
        }

        zipkin2.Span span1 = reportedSpans.get(1);
        Assert.assertNotNull(span1.traceId());
        Assert.assertNotNull(span1.id());
        Assert.assertEquals(span1.id(), span1.traceId());
        Assert.assertNull(span1.parentId());
        Assert.assertEquals(span1.kind(), zipkin2.Span.Kind.SERVER);
        Assert.assertEquals(span1.name(), "get /test-call-1-in");
        Assert.assertTrue(span1.timestamp() > 0);
        Assert.assertTrue(span1.duration() > 0);
        Assert.assertNotNull(span1.localEndpoint());
        Assert.assertEquals(span1.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span1.remoteEndpoint());
        Assert.assertEquals(span1.annotations().size(), 0);
        Assert.assertNotNull(span1.tags());
        Assert.assertEquals(span1.tags().size(), 3);
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_HTTP_METHOD), "GET");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "200");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "192.168.12.100");

        zipkin2.Span span2 = reportedSpans.get(0);
        Assert.assertNotNull(span2.traceId());
        Assert.assertNotNull(span2.id());
        Assert.assertNotNull(span2.parentId());
        Assert.assertEquals(span2.parentId(), span1.id());
        Assert.assertEquals(span2.kind(), zipkin2.Span.Kind.CLIENT);
        Assert.assertEquals(span2.name(), "get /test-call-1-out");
        Assert.assertTrue(span2.timestamp() > span1.timestamp());
        Assert.assertTrue(span2.duration() > 0);
        Assert.assertNotNull(span2.localEndpoint());
        Assert.assertEquals(span2.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span2.remoteEndpoint());
        Assert.assertEquals(span2.annotations().size(), 0);
        Assert.assertNotNull(span2.tags());
        Assert.assertEquals(span2.tags().size(), 5);
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_HTTP_METHOD), "GET");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "200");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_HTTP_URL), "destination-service-1");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "192.168.12.104");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_PROTOCOL), "http");
    }

    @Test
    public void testFullDoublePass() {
        /*
         * A double pass is done by the APIM.
         */

        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Whitebox.setInternalState(tracingSynapseHandler, "tracer", tracer);
        Map<String, Stack<Span>> spansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");

        String correlationId;
        // First Request In
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(new HashMap<>());
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-3-in");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("POST");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_REMOTE_HOST))
                    .thenReturn("192.168.11.153");

            String[] correlationIdHolder = new String[1];
            Mockito.doAnswer(invocation -> correlationIdHolder[0] = invocation.getArgumentAt(1, String.class))
                    .when(synapseAxis2MessageContext)
                    .setProperty(Mockito.eq(Constants.TRACING_CORRELATION_ID), Mockito.anyString());

            boolean continueFlow = tracingSynapseHandler.handleRequestInFlow(synapseAxis2MessageContext);
            correlationId = correlationIdHolder[0];

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertNotNull(correlationId);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 1);
        }
        Map<String, String> requestHeadersMap2 = new HashMap<>();
        // First Request Out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(requestHeadersMap2);
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-4-out");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("POST");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_ENDPOINT))
                    .thenReturn("localhost");
            Mockito.when(synapseAxis2MessageContext.getProperty(
                    Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_PEER_ADDRESS))
                    .thenReturn("127.0.0.1");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_TRANSPORT))
                    .thenReturn("https");

            boolean continueFlow = tracingSynapseHandler.handleRequestOutFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 2);
        }
        Assert.assertNotNull(requestHeadersMap2.get(B3_TRACE_ID_HEADER));
        Assert.assertNotNull(requestHeadersMap2.get(B3_PARENT_SPAN_ID_HEADER));
        Assert.assertNotNull(requestHeadersMap2.get(B3_SPAN_ID_HEADER));
        Assert.assertNotNull(requestHeadersMap2.get(B3_SAMPLED_HEADER));
        Assert.assertNotNull(requestHeadersMap2.get(Constants.B3_GLOBAL_GATEWAY_CORRELATION_ID_HEADER));
        // Second Request In
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(requestHeadersMap2);
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-5-in");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("POST");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_REMOTE_HOST))
                    .thenReturn("127.0.0.1");

            boolean continueFlow = tracingSynapseHandler.handleRequestInFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertNotNull(correlationId);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 3);
        }
        // Second Request Out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(new HashMap<>());
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-6-out");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("POST");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_ENDPOINT))
                    .thenReturn("destination-service-2");
            Mockito.when(synapseAxis2MessageContext.getProperty(
                    Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_PEER_ADDRESS))
                    .thenReturn("192.168.13.74");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_TRANSPORT))
                    .thenReturn("https");

            boolean continueFlow = tracingSynapseHandler.handleRequestOutFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 4);
        }
        // Second Response in
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(200);

            boolean continueFlow = tracingSynapseHandler.handleResponseInFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 1);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 3);
        }
        // Second Response out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(200);

            boolean continueFlow = tracingSynapseHandler.handleResponseOutFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 2);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 2);
        }
        // First Response in
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(200);

            boolean continueFlow = tracingSynapseHandler.handleResponseInFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 3);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 1);
        }
        // First Response out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(200);

            boolean continueFlow = tracingSynapseHandler.handleResponseOutFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 4);
            Assert.assertEquals(spansMap.size(), 0);
        }

        zipkin2.Span span1 = reportedSpans.get(3);
        Assert.assertNotNull(span1.traceId());
        Assert.assertNotNull(span1.id());
        Assert.assertEquals(span1.id(), span1.traceId());
        Assert.assertNull(span1.parentId());
        Assert.assertEquals(span1.kind(), zipkin2.Span.Kind.SERVER);
        Assert.assertEquals(span1.name(), "get /test-call-3-in");
        Assert.assertTrue(span1.timestamp() > 0);
        Assert.assertTrue(span1.duration() > 0);
        Assert.assertNotNull(span1.localEndpoint());
        Assert.assertEquals(span1.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span1.remoteEndpoint());
        Assert.assertEquals(span1.annotations().size(), 0);
        Assert.assertNotNull(span1.tags());
        Assert.assertEquals(span1.tags().size(), 3);
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_HTTP_METHOD), "POST");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "200");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "192.168.11.153");

        zipkin2.Span span2 = reportedSpans.get(2);
        Assert.assertNotNull(span2.traceId());
        Assert.assertNotNull(span2.id());
        Assert.assertNotNull(span2.parentId());
        Assert.assertEquals(span2.parentId(), span1.id());
        Assert.assertEquals(span2.kind(), zipkin2.Span.Kind.CLIENT);
        Assert.assertEquals(span2.name(), "get /test-call-4-out");
        Assert.assertTrue(span2.timestamp() > span1.timestamp());
        Assert.assertTrue(span2.duration() > 0);
        Assert.assertNotNull(span2.localEndpoint());
        Assert.assertEquals(span2.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span2.remoteEndpoint());
        Assert.assertEquals(span2.annotations().size(), 0);
        Assert.assertNotNull(span2.tags());
        Assert.assertEquals(span2.tags().size(), 5);
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_HTTP_METHOD), "POST");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "200");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_HTTP_URL), "localhost");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "127.0.0.1");
        Assert.assertEquals(span2.tags().get(Constants.TAG_KEY_PROTOCOL), "https");

        zipkin2.Span span3 = reportedSpans.get(1);
        Assert.assertNotNull(span3.traceId());
        Assert.assertNotNull(span3.id());
        Assert.assertNotNull(span3.parentId());
        Assert.assertEquals(span3.parentId(), span2.id());
        Assert.assertNull(span3.kind());
        Assert.assertEquals(span3.name(), "get /test-call-5-in");
        Assert.assertTrue(span3.timestamp() > span1.timestamp());
        Assert.assertTrue(span3.duration() > 0);
        Assert.assertNotNull(span3.localEndpoint());
        Assert.assertEquals(span3.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span3.remoteEndpoint());
        Assert.assertEquals(span3.annotations().size(), 0);
        Assert.assertNotNull(span3.tags());
        Assert.assertEquals(span3.tags().size(), 3);
        Assert.assertEquals(span3.tags().get(Constants.TAG_KEY_HTTP_METHOD), "POST");
        Assert.assertEquals(span3.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "200");
        Assert.assertEquals(span3.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "127.0.0.1");

        zipkin2.Span span4 = reportedSpans.get(0);
        Assert.assertNotNull(span4.traceId());
        Assert.assertNotNull(span4.id());
        Assert.assertNotNull(span4.parentId());
        Assert.assertEquals(span4.parentId(), span3.id());
        Assert.assertEquals(span4.kind(), zipkin2.Span.Kind.CLIENT);
        Assert.assertEquals(span4.name(), "get /test-call-6-out");
        Assert.assertTrue(span4.timestamp() > span1.timestamp());
        Assert.assertTrue(span4.duration() > 0);
        Assert.assertNotNull(span4.localEndpoint());
        Assert.assertEquals(span4.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span4.remoteEndpoint());
        Assert.assertEquals(span4.annotations().size(), 0);
        Assert.assertNotNull(span4.tags());
        Assert.assertEquals(span4.tags().size(), 5);
        Assert.assertEquals(span4.tags().get(Constants.TAG_KEY_HTTP_METHOD), "POST");
        Assert.assertEquals(span4.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "200");
        Assert.assertEquals(span4.tags().get(Constants.TAG_KEY_HTTP_URL), "destination-service-2");
        Assert.assertEquals(span4.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "192.168.13.74");
        Assert.assertEquals(span4.tags().get(Constants.TAG_KEY_PROTOCOL), "https");
    }

    @Test
    public void testRequestInWithoutRequestOut() {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Whitebox.setInternalState(tracingSynapseHandler, "tracer", tracer);
        Map<String, Stack<Span>> spansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");

        String correlationId;
        // Request In
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(new HashMap<>());
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-7-in");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("PUT");
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_REMOTE_HOST))
                    .thenReturn("192.168.17.56");

            String[] correlationIdHolder = new String[1];
            Mockito.doAnswer(invocation -> correlationIdHolder[0] = invocation.getArgumentAt(1, String.class))
                    .when(synapseAxis2MessageContext)
                    .setProperty(Mockito.eq(Constants.TRACING_CORRELATION_ID), Mockito.anyString());

            boolean continueFlow = tracingSynapseHandler.handleRequestInFlow(synapseAxis2MessageContext);
            correlationId = correlationIdHolder[0];

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertNotNull(correlationId);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 1);
        }
        // Response out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(500);

            boolean continueFlow = tracingSynapseHandler.handleResponseOutFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 1);
            Assert.assertEquals(spansMap.size(), 0);
        }

        zipkin2.Span span = reportedSpans.get(0);
        Assert.assertNotNull(span.traceId());
        Assert.assertNotNull(span.id());
        Assert.assertEquals(span.id(), span.traceId());
        Assert.assertNull(span.parentId());
        Assert.assertEquals(span.kind(), zipkin2.Span.Kind.SERVER);
        Assert.assertEquals(span.name(), "get /test-call-7-in");
        Assert.assertTrue(span.timestamp() > 0);
        Assert.assertTrue(span.duration() > 0);
        Assert.assertNotNull(span.localEndpoint());
        Assert.assertEquals(span.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span.remoteEndpoint());
        Assert.assertEquals(span.annotations().size(), 0);
        Assert.assertNotNull(span.tags());
        Assert.assertEquals(span.tags().size(), 4);
        Assert.assertEquals(span.tags().get(Constants.TAG_KEY_HTTP_METHOD), "PUT");
        Assert.assertEquals(span.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "500");
        Assert.assertEquals(span.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "192.168.17.56");
        Assert.assertEquals(span.tags().get(Constants.TAG_KEY_ERROR), "true");
    }

    @Test
    public void testRequestOutWithoutRequestIn() {
        /*
         * This is an edge case which is not required. However, for safety this is validated.
         * This validates whether the handler fails when the span stack is empty in the request out flow.
         */

        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Whitebox.setInternalState(tracingSynapseHandler, "tracer", tracer);
        Map<String, Stack<Span>> spansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");

        String correlationId;
        // Request Out
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
            EndpointReference toEndpointReference = Mockito.mock(EndpointReference.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                    .thenReturn(new HashMap<>());
            Mockito.when(synapseAxis2MessageContext.getTo()).thenReturn(toEndpointReference);
            Mockito.when(toEndpointReference.getAddress()).thenReturn("GET /test-call-8-out");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_HTTP_METHOD))
                    .thenReturn("GET");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_ENDPOINT))
                    .thenReturn("destination-service-1");
            Mockito.when(synapseAxis2MessageContext.getProperty(
                    Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_PEER_ADDRESS))
                    .thenReturn("192.168.15.84");
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.SYNAPSE_MESSAGE_CONTEXT_PROPERTY_TRANSPORT))
                    .thenReturn("http");

            String[] correlationIdHolder = new String[1];
            Mockito.doAnswer(invocation -> correlationIdHolder[0] = invocation.getArgumentAt(1, String.class))
                    .when(synapseAxis2MessageContext)
                    .setProperty(Mockito.eq(Constants.TRACING_CORRELATION_ID), Mockito.anyString());

            boolean continueFlow = tracingSynapseHandler.handleRequestOutFlow(synapseAxis2MessageContext);
            correlationId = correlationIdHolder[0];

            Assert.assertTrue(continueFlow);
            Assert.assertNotNull(correlationId);
            Assert.assertEquals(reportedSpans.size(), 0);
            Assert.assertEquals(spansMap.size(), 1);
            Assert.assertNotNull(spansMap.get(correlationId));
            Assert.assertEquals(spansMap.get(correlationId).size(), 1);
        }
        // Response in
        {
            Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
            MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);

            Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);
            Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                    .thenReturn(correlationId);
            Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                    .thenReturn(200);

            boolean continueFlow = tracingSynapseHandler.handleResponseInFlow(synapseAxis2MessageContext);

            Assert.assertTrue(continueFlow);
            Assert.assertEquals(reportedSpans.size(), 1);
            Assert.assertEquals(spansMap.size(), 0);
        }

        zipkin2.Span span1 = reportedSpans.get(0);
        Assert.assertNotNull(span1.traceId());
        Assert.assertNotNull(span1.id());
        Assert.assertEquals(span1.id(), span1.traceId());
        Assert.assertNull(span1.parentId());
        Assert.assertEquals(span1.kind(), zipkin2.Span.Kind.CLIENT);
        Assert.assertEquals(span1.name(), "get /test-call-8-out");
        Assert.assertTrue(span1.timestamp() > 0);
        Assert.assertTrue(span1.duration() > 0);
        Assert.assertNotNull(span1.localEndpoint());
        Assert.assertEquals(span1.localEndpoint().serviceName(), Constants.GLOBAL_GATEWAY_SERVICE_NAME);
        Assert.assertNull(span1.remoteEndpoint());
        Assert.assertEquals(span1.annotations().size(), 0);
        Assert.assertNotNull(span1.tags());
        Assert.assertEquals(span1.tags().size(), 5);
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_HTTP_METHOD), "GET");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_HTTP_STATUS_CODE), "200");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_HTTP_URL), "destination-service-1");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_PEER_ADDRESS), "192.168.15.84");
        Assert.assertEquals(span1.tags().get(Constants.TAG_KEY_PROTOCOL), "http");
    }

    @Test
    public void testFinishLastSpan() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        TracingSynapseHandler tracingSynapseHandler = Mockito.spy(new TracingSynapseHandler());
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
        Span spanA = Mockito.mock(Span.class);
        Span spanB = Mockito.mock(Span.class);

        Map<String, Stack<Span>> spansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");
        Stack<Span> spanStack = new Stack<>();
        spanStack.push(spanA);
        spanStack.push(spanB);
        spansMap.put(correlationId, spanStack);

        Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                .thenReturn(correlationId);
        PowerMockito.when(tracingSynapseHandler, "getAxis2MessageContext", synapseAxis2MessageContext)
                .thenReturn(axis2MessageContext);
        Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                .thenReturn(201);

        boolean success = Whitebox.invokeMethod(tracingSynapseHandler, "finishLastSpan", synapseAxis2MessageContext);

        Assert.assertTrue(success);
        Map<String, Stack<Span>> finalSpansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");
        Assert.assertEquals(finalSpansMap.size(), 1);
        Assert.assertNotNull(finalSpansMap.get(correlationId));
        Assert.assertEquals(finalSpansMap.get(correlationId).size(), 1);
        Assert.assertEquals(finalSpansMap.get(correlationId).peek(), spanA);
        Mockito.verify(spanB, Mockito.times(1)).setTag(Constants.TAG_KEY_HTTP_STATUS_CODE, 201);
        Mockito.verify(spanB, Mockito.times(1)).setTag(Constants.TAG_KEY_ERROR, false);
        Mockito.verify(spanB, Mockito.times(1)).finish();
    }

    @Test
    public void testFinishLastSpanCleanup() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        TracingSynapseHandler tracingSynapseHandler = Mockito.spy(new TracingSynapseHandler());
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
        Span span = Mockito.mock(Span.class);

        Map<String, Stack<Span>> spansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");
        Stack<Span> spanStack = new Stack<>();
        spanStack.push(span);
        spansMap.put(correlationId, spanStack);

        Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                .thenReturn(correlationId);
        PowerMockito.when(tracingSynapseHandler, "getAxis2MessageContext", synapseAxis2MessageContext)
                .thenReturn(axis2MessageContext);
        Mockito.when(axis2MessageContext.getProperty(Constants.AXIS2_MESSAGE_CONTEXT_PROPERTY_HTTP_STATUS_CODE))
                .thenReturn(201);

        boolean success = Whitebox.invokeMethod(tracingSynapseHandler, "finishLastSpan", synapseAxis2MessageContext);

        Assert.assertTrue(success);
        Map<String, Stack<Span>> finalSpansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");
        Assert.assertEquals(finalSpansMap.size(), 0);
        Mockito.verify(span, Mockito.times(1)).setTag(Constants.TAG_KEY_HTTP_STATUS_CODE, 201);
        Mockito.verify(span, Mockito.times(1)).setTag(Constants.TAG_KEY_ERROR, false);
        Mockito.verify(span, Mockito.times(1)).finish();
    }

    @Test
    public void testFinishLastSpanWithNoSpanStack() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        TracingSynapseHandler tracingSynapseHandler = Mockito.spy(new TracingSynapseHandler());
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);

        Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                .thenReturn(correlationId);

        boolean success = Whitebox.invokeMethod(tracingSynapseHandler, "finishLastSpan", synapseAxis2MessageContext);

        Assert.assertTrue(success);
        Map<String, Stack<Span>> finalSpansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");
        Assert.assertEquals(finalSpansMap.size(), 0);
    }

    @Test
    public void testFinishLastSpanWithNoAxis2MessageContext() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        TracingSynapseHandler tracingSynapseHandler = Mockito.spy(new TracingSynapseHandler());
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        Span span = Mockito.mock(Span.class);

        Map<String, Stack<Span>> spansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");
        Stack<Span> spanStack = new Stack<>();
        spanStack.push(span);
        spansMap.put(correlationId, spanStack);

        Mockito.when(synapseAxis2MessageContext.getProperty(Constants.TRACING_CORRELATION_ID))
                .thenReturn(correlationId);

        boolean success = Whitebox.invokeMethod(tracingSynapseHandler, "finishLastSpan", synapseAxis2MessageContext);

        Assert.assertTrue(success);
        Map<String, Stack<Span>> finalSpansMap = Whitebox.getInternalState(tracingSynapseHandler, "spansMap");
        Assert.assertEquals(finalSpansMap.size(), 0);
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.anyString(), Mockito.anyInt());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(span, Mockito.times(1)).finish();
    }

    @Test
    public void testExtractHeadersFromSynapseContext() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
        Map headers = Mockito.mock(Map.class);

        PowerMockito.when(tracingSynapseHandler, "getAxis2MessageContext", synapseAxis2MessageContext)
                .thenReturn(axis2MessageContext);
        Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                .thenReturn(headers);

        Map<String, String> returnedHeaders = Whitebox.invokeMethod(tracingSynapseHandler,
                "extractHeadersFromSynapseContext", synapseAxis2MessageContext);

        Assert.assertNotNull(headers);
        Assert.assertSame(returnedHeaders, headers);
    }

    @Test
    public void testExtractHeadersFromSynapseContextWithNulContext() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        PowerMockito.when(tracingSynapseHandler, "getAxis2MessageContext", synapseAxis2MessageContext)
                .thenReturn(null);

        Map<String, String> returnedHeaders = Whitebox.invokeMethod(tracingSynapseHandler,
                "extractHeadersFromSynapseContext", synapseAxis2MessageContext);
        Assert.assertNotNull(returnedHeaders);
        Assert.assertEquals(returnedHeaders.size(), 0);
    }

    @Test
    public void testExtractHeadersFromSynapseContextWithHeadersMapOfInvalidTyp() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
        PowerMockito.when(tracingSynapseHandler, "getAxis2MessageContext", synapseAxis2MessageContext)
                .thenReturn(axis2MessageContext);

        Object headers = Mockito.mock(Object.class);
        Mockito.when(axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS))
                .thenReturn(headers);

        Map<String, String> returnedHaders = Whitebox.invokeMethod(tracingSynapseHandler,
                "extractHeadersFromSynapseContext", synapseAxis2MessageContext);

        Assert.assertNotNull(headers);
        Assert.assertEquals(returnedHaders.size(), 0);
    }

    @Test
    public void testGetAxis2MessageContext() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Axis2MessageContext synapseAxis2MessageContext = Mockito.mock(Axis2MessageContext.class);
        MessageContext axis2MessageContext = Mockito.mock(MessageContext.class);
        Mockito.when(synapseAxis2MessageContext.getAxis2MessageContext()).thenReturn(axis2MessageContext);

        MessageContext returnedMessageContext = Whitebox.invokeMethod(tracingSynapseHandler,
                "getAxis2MessageContext", synapseAxis2MessageContext);

        Assert.assertEquals(returnedMessageContext, axis2MessageContext);
    }

    @Test
    public void testGetAxis2MessageContextOfWrongMessageContextType() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        org.apache.synapse.MessageContext invalidAxis2MessageContext =
                Mockito.mock(org.apache.synapse.MessageContext.class);

        MessageContext returnedMessageContext = Whitebox.invokeMethod(tracingSynapseHandler,
                "getAxis2MessageContext", invalidAxis2MessageContext);

        Assert.assertNull(returnedMessageContext);
    }

    @Test
    public void testAddTagOfTypeString() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Span span = Mockito.mock(Span.class);

        Whitebox.invokeMethod(tracingSynapseHandler, "addTag", span, "testKey1", "testValue1");

        Mockito.verify(span, Mockito.times(1)).setTag(Mockito.eq("testKey1"), Mockito.eq("testValue1"));
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyInt());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyBoolean());
    }

    @Test
    public void testAddTagOfTypeNumber() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Span span = Mockito.mock(Span.class);

        Whitebox.invokeMethod(tracingSynapseHandler, "addTag", span, "testKey2", 1534);

        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyString());
        Mockito.verify(span, Mockito.times(1)).setTag(Mockito.eq("testKey2"), Mockito.eq(1534));
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyBoolean());
    }

    @Test
    public void testAddTagOfTypeBoolean() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Span span = Mockito.mock(Span.class);

        Whitebox.invokeMethod(tracingSynapseHandler, "addTag", span, "testKey3", true);

        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyString());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyInt());
        Mockito.verify(span, Mockito.times(1)).setTag(Mockito.eq("testKey3"), Mockito.eq(true));
    }

    @Test
    public void testAddTagOfUnknownType() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Span span = Mockito.mock(Span.class);

        Whitebox.invokeMethod(tracingSynapseHandler, "addTag", span, "testKey4", new HashMap<>());

        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyString());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyInt());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyBoolean());
    }

    @Test
    public void testAddTagOfTypeNull() throws Exception {
        TracingSynapseHandler tracingSynapseHandler = new TracingSynapseHandler();
        Span span = Mockito.mock(Span.class);

        Whitebox.invokeMethod(tracingSynapseHandler, "addTag", span, "testKey5", null);

        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyString());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyInt());
        Mockito.verify(span, Mockito.times(0)).setTag(Mockito.eq("testKey5"), Mockito.anyBoolean());
    }
}
