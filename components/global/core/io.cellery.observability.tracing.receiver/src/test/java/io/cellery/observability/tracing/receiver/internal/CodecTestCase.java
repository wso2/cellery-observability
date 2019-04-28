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

package io.cellery.observability.tracing.receiver.internal;

import org.junit.Test;
import org.testng.Assert;
import zipkin2.Endpoint;
import zipkin2.Span;
import zipkin2.codec.SpanBytesEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test Cases for Zipkin Codec.
 */
public class CodecTestCase {

    @Test
    public void testDecodingEncodedSpans() {
        List<Span> spansList = generateSpans();
        SpanBytesEncoder[] encoders = new SpanBytesEncoder[]{
                SpanBytesEncoder.JSON_V1,
                SpanBytesEncoder.JSON_V2,
                SpanBytesEncoder.PROTO3,
                SpanBytesEncoder.THRIFT
        };
        for (SpanBytesEncoder encoder : encoders) {
            byte[] bytes = encoder.encodeList(spansList);
            List<ZipkinSpan> decodedSpans = Codec.decodeData(bytes);
            assertEqual(decodedSpans, spansList);
        }
    }

    @Test
    public void testDecodingThriftSpans() throws Exception {
        List<Span> spansList = generateSpans();
        byte[] bytes = SpanBytesEncoder.THRIFT.encodeList(spansList);
        List<ZipkinSpan> decodedSpans = Codec.decodeThriftData(bytes);
        assertEqual(decodedSpans, spansList);
    }

    /**
     * Generate spans list for testing.
     *
     * @return The generated spans list
     */
    private List<Span> generateSpans() {
        List<Span> spansList = new ArrayList<>();
        spansList.add(Span.newBuilder()
                .traceId("1234567890")
                .id("1234567891")
                .parentId("1234567890")
                .name("test-a-span")
                .kind(Span.Kind.CLIENT)
                .duration(10001)
                .timestamp(100000001)
                .remoteEndpoint(Endpoint.newBuilder()
                        .ip("192.168.56.101")
                        .port(9001)
                        .serviceName("hello")
                        .build())
                .localEndpoint(Endpoint.newBuilder()
                        .ip("192.168.56.100")
                        .port(9000)
                        .serviceName("hello-caller")
                        .build())
                .putTag("keyA1", "valueA1")
                .putTag("keyA2", "valueA2")
                .build());
        spansList.add(Span.newBuilder()
                .traceId("2234567890")
                .id("2234567891")
                .parentId("2234567890")
                .name("test-b-span")
                .kind(Span.Kind.SERVER)
                .duration(30001)
                .timestamp(300000001)
                .remoteEndpoint(Endpoint.newBuilder()
                        .ip("192.168.57.201")
                        .port(8004)
                        .serviceName("pet-fe")
                        .build())
                .localEndpoint(Endpoint.newBuilder()
                        .ip("192.168.57.205")
                        .port(7000)
                        .serviceName("pet-be")
                        .build())
                .putTag("keyB1", "valueB1")
                .putTag("keyB2", "valueB2")
                .putTag("keyB3", "valueB3")
                .build());
        spansList.add(Span.newBuilder()
                .traceId("3234567890")
                .id("3234567891")
                .parentId("3234567890")
                .name("test-c-span")
                .kind(Span.Kind.PRODUCER)
                .duration(40001)
                .timestamp(400000001)
                .localEndpoint(Endpoint.newBuilder()
                        .ip("192.168.60.232")
                        .port(10000)
                        .serviceName("hello-api-worker")
                        .build())
                .putTag("keyC1", "valueC1")
                .build());
        spansList.add(Span.newBuilder()
                .traceId("4234567890")
                .id("4234567891")
                .parentId("4234567890")
                .name("test-d-span")
                .kind(Span.Kind.CONSUMER)
                .duration(70001)
                .timestamp(400000001)
                .localEndpoint(Endpoint.newBuilder()
                        .ip("192.168.100.104")
                        .port(12000)
                        .serviceName("pet-recommendations")
                        .build())
                .putTag("keyD1", "valueD1")
                .putTag("keyD2", "valueD2")
                .putTag("keyD3", "valueD3")
                .putTag("keyD4", "valueD4")
                .build());
        spansList.add(Span.newBuilder()
                .traceId("4234567890")
                .id("4234567891")
                .parentId("4234567890")
                .name("test-d-span")
                .duration(70001)
                .timestamp(400000001)
                .putTag("keyD1", "valueD1")
                .putTag("keyD2", "valueD2")
                .putTag("keyD3", "valueD3")
                .putTag("keyD4", "valueD4")
                .build());
        return spansList;
    }

    /**
     * Compare and assert whether the two spans lists are equal.
     */
    private void assertEqual(List<ZipkinSpan> actualSpans, List<Span> expectedSpans) {
        Assert.assertEquals(actualSpans.size(), expectedSpans.size());
        for (int i = 0; i < expectedSpans.size(); i++) {
            Span expectedSpan = expectedSpans.get(i);
            ZipkinSpan actualSpan = actualSpans.get(i);

            Assert.assertEquals(actualSpan.getTraceId(), expectedSpan.traceId());
            Assert.assertEquals(actualSpan.getId(), expectedSpan.id());
            Assert.assertEquals(actualSpan.getParentId(), expectedSpan.parentId());
            if (expectedSpan.kind() == null) {
                Assert.assertEquals(actualSpan.getKind(), "");
            } else {
                Assert.assertEquals(actualSpan.getKind(), expectedSpan.kind().toString());
            }
            Assert.assertEquals(actualSpan.getName(), expectedSpan.name());
            if (expectedSpan.localServiceName() == null) {
                Assert.assertEquals(actualSpan.getServiceName(), "");
            } else {
                Assert.assertEquals(actualSpan.getServiceName(), expectedSpan.localServiceName());
            }
            Assert.assertEquals(new Long(actualSpan.getTimestamp()), expectedSpan.timestamp());
            Assert.assertEquals(new Long(actualSpan.getDuration()), expectedSpan.duration());

            Assert.assertEquals(expectedSpan.tags().size(), expectedSpan.tags().size());
            for (Map.Entry<String, String> tagEntry : expectedSpan.tags().entrySet()) {
                String expectedSpanValue = expectedSpan.tags().get(tagEntry.getKey());
                Assert.assertEquals(tagEntry.getValue(), expectedSpanValue);
            }
        }
    }
}
