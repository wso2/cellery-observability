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

import brave.Tracing;
import brave.opentracing.BraveTracer;
import brave.propagation.B3Propagation;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.map.keyvalue.sourcemapper.KeyValueSourceMapper;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.SiddhiTestHelper;
import zipkin2.Endpoint;
import zipkin2.codec.BytesEncoder;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Cases for Tracing Event Source.
 */
public class TracingEventSourceTestCase {
    private static final String HOST = "localhost";
    private static final int PORT = 20435;
    private static final String API_CONTEXT = "/api/v1/spans";
    private static final String TRACING_RECEIVER_ENDPOINT = "http://" + HOST + ":" + PORT + API_CONTEXT;
    private static final String SERVICE_NAME = "test-service";

    private static final int WAIT_TIME = 50;
    private static final int TIMEOUT = 5000;

    private AtomicInteger eventCount = new AtomicInteger(0);
    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> receivedEvents;

    @BeforeMethod
    public void init() {
        eventCount.set(0);
        String inStreamDefinition = "@App:name(\"test-siddhi-app\")\n" +
                "@source(type=\"tracing-receiver\", host=\"" + HOST + "\", " +
                "port=\"" + PORT + "\", api.context=\"" + API_CONTEXT + "\", " +
                "@map(type=\"keyvalue\", fail.on.missing.attribute=\"false\"))\n" +
                "define stream zipkinStream (traceId string, id string, parentId string, name string, " +
                "serviceName string, kind string, timestamp long, duration long, tags string);";
        String query = "@info(name = \"query\")\n" +
                "from zipkinStream\n" +
                "select *\n" +
                "insert into outputStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("keyvalue", KeyValueSourceMapper.class);
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + "\n" + query);
        receivedEvents = new ArrayList<>();
        siddhiAppRuntime.addCallback("query", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                for (Event event : inEvents) {
                    synchronized (this) {
                        receivedEvents.add(event);
                    }
                    eventCount.incrementAndGet();
                }
            }
        });
        siddhiAppRuntime.start();
    }

    @AfterMethod
    public void cleanUp() {
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void testZipkinOverJsonV1() throws Exception {
        Tracer tracer = getJsonBasedTracer(SpanBytesEncoder.JSON_V1);
        Span spanA = tracer.buildSpan("test-span-a")
                .start();
        spanA.setTag("k8s.service", "employee--sts");
        spanA.setTag("error", false);
        spanA.finish();

        Span spanB = tracer.buildSpan("test-span-b")
                .asChildOf(spanA)
                .start();
        spanB.setTag("span.kind", "client");
        spanB.setTag("cellery.component", "salary");
        spanB.setTag("k8s.service", "employee--salary");
        spanB.finish();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 9);
            if ("test-span-a".equals(data[3])) {
                Assert.assertNotNull(data[0]);
                Assert.assertNotNull(data[1]);
                Assert.assertNull(data[2]);
                Assert.assertEquals(data[4], SERVICE_NAME);
                Assert.assertEquals(data[5], "");
                Assert.assertNotEquals(data[6], 0);
                Assert.assertNotEquals(data[7], 0);
                Assert.assertEquals(data[8], "{\"k8s.service\":\"employee--sts\"}");
            } else if ("test-span-b".equals(data[3])) {
                Assert.assertNotNull(data[0]);
                Assert.assertNotNull(data[1]);
                Assert.assertNotNull(data[2]);
                Assert.assertEquals(data[4], SERVICE_NAME);
                Assert.assertEquals(data[5], "CLIENT");
                Assert.assertNotEquals(data[6], 0);
                Assert.assertNotEquals(data[7], 0);
                Assert.assertEquals(data[8], "{\"cellery.component\":\"salary\"," +
                        "\"k8s.service\":\"employee--salary\"}");
            } else {
                Assert.fail("Unexpected span with name " + data[3]);
            }
        }
    }

    @Test
    public void testZipkinOverJsonV2() throws Exception {
        Tracer tracer = getJsonBasedTracer(SpanBytesEncoder.JSON_V2);
        Span spanA = tracer.buildSpan("test-span-c")
                .start();
        spanA.setTag("span.kind", "server");
        spanA.setTag("k8s.service", "employee--cell-gateway");
        spanA.setTag("error", false);
        spanA.finish();

        Span spanB = tracer.buildSpan("test-span-d")
                .asChildOf(spanA)
                .start();
        spanB.setTag("cellery.component", "stock");
        spanB.setTag("k8s.service", "stock-options");
        spanB.finish();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 9);
            if ("test-span-c".equals(data[3])) {
                Assert.assertNotNull(data[0]);
                Assert.assertNotNull(data[1]);
                Assert.assertNull(data[2]);
                Assert.assertEquals(data[4], SERVICE_NAME);
                Assert.assertEquals(data[5], "SERVER");
                Assert.assertNotEquals(data[6], 0);
                Assert.assertNotEquals(data[7], 0);
                Assert.assertEquals(data[8], "{\"k8s.service\":\"employee--cell-gateway\"}");
            } else if ("test-span-d".equals(data[3])) {
                Assert.assertNotNull(data[0]);
                Assert.assertNotNull(data[1]);
                Assert.assertNotNull(data[2]);
                Assert.assertEquals(data[4], SERVICE_NAME);
                Assert.assertEquals(data[5], "");
                Assert.assertNotEquals(data[6], 0);
                Assert.assertNotEquals(data[7], 0);
                Assert.assertEquals(data[8], "{\"cellery.component\":\"stock\"," +
                        "\"k8s.service\":\"stock-options\"}");
            } else {
                Assert.fail("Unexpected span with name " + data[3]);
            }
        }
    }

    @Test
    public void testZipkinOverThriftOnHttp() throws Exception {
        List<zipkin2.Span> spansList = new ArrayList<>();
        spansList.add(zipkin2.Span.newBuilder()
                .traceId("1234567890")
                .id("1234567891")
                .parentId("1234567890")
                .name("test-a-span")
                .kind(zipkin2.Span.Kind.CLIENT)
                .duration(10001)
                .timestamp(100000001)
                .localEndpoint(Endpoint.newBuilder()
                        .ip("192.168.56.100")
                        .port(9000)
                        .serviceName(SERVICE_NAME)
                        .build())
                .remoteEndpoint(Endpoint.newBuilder()
                        .ip("192.168.56.101")
                        .port(9001)
                        .serviceName("hello")
                        .build())
                .putTag("keyA1", "valueA1")
                .putTag("keyA2", "valueA2")
                .build());

        byte[] bytes = SpanBytesEncoder.THRIFT.encodeList(spansList);
        HttpPost request = new HttpPost(TRACING_RECEIVER_ENDPOINT);
        request.setHeader(Constants.HTTP_CONTENT_TYPE_HEADER, Constants.HTTP_APPLICATION_THRIFT_CONTENT_TYPE);
        request.setEntity(new StringEntity(new String(bytes)));
        HttpResponse response = HttpClientBuilder.create()
                .build()
                .execute(request);

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
        Assert.assertEquals(eventCount.get(), 1);

        Object[] data = receivedEvents.get(0).getData();
        Assert.assertNotNull(data[0]);
        Assert.assertNotNull(data[1]);
        Assert.assertNotNull(data[2]);
        Assert.assertEquals(data[3], "test-a-span");
        Assert.assertEquals(data[4], SERVICE_NAME);
        Assert.assertEquals(data[5], "CLIENT");
        Assert.assertNotEquals(data[6], 10001);
        Assert.assertNotEquals(data[7], 100000001);
        Assert.assertEquals(data[8], "{\"keyA1\":\"valueA1\",\"keyA2\":\"valueA2\"}");
    }

    @Test
    public void testZipkinOverThriftOnHttpWithInvalidData() throws Exception {
        byte[] bytes = "random set of bytes".getBytes();
        HttpPost request = new HttpPost(TRACING_RECEIVER_ENDPOINT);
        request.setHeader(Constants.HTTP_CONTENT_TYPE_HEADER, Constants.HTTP_APPLICATION_THRIFT_CONTENT_TYPE);
        request.setEntity(new StringEntity(new String(bytes)));
        HttpResponse response = HttpClientBuilder.create()
                .build()
                .execute(request);

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
        Assert.assertEquals(eventCount.get(), 0);
    }

    /**
     * Get a Tracer which can report JSON encoded tracing data.
     *
     * @param encoder Encoder to use
     * @return The tracer
     */
    private static Tracer getJsonBasedTracer(BytesEncoder<zipkin2.Span> encoder) {
        Sender sender = URLConnectionSender.create(TRACING_RECEIVER_ENDPOINT).toBuilder()
                .compressionEnabled(false)
                .build();
        Reporter<zipkin2.Span> reporter = AsyncReporter.builder(sender)
                .build(encoder);
        return buildTracer(reporter);
    }

    /**
     * Build and return a tracer.
     *
     * @param reporter The reporter for tracing
     * @return The tracer
     */
    private static Tracer buildTracer(Reporter<zipkin2.Span> reporter) {
        Tracing braveTracing = Tracing.newBuilder()
                .localServiceName(SERVICE_NAME)
                .spanReporter(reporter)
                .build();
        return BraveTracer.newBuilder(braveTracing)
                .textMapPropagation(Format.Builtin.HTTP_HEADERS, B3Propagation.B3_STRING)
                .build();
    }
}
