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

import org.apache.log4j.Logger;
import zipkin2.SpanBytesDecoderDetector;
import zipkin2.codec.BytesDecoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Codec used for decoding tracing data.
 */
public class Codec {

    private static final Logger logger = Logger.getLogger(Codec.class.getName());

    /**
     * Decode a default byte array.
     *
     * @param byteArray The byte array to decode
     * @return spans list
     */
    public static List<ZipkinSpan> decodeData(byte[] byteArray) {
        BytesDecoder<zipkin2.Span> spanBytesDecoder = SpanBytesDecoderDetector.decoderForListMessage(byteArray);
        if (logger.isDebugEnabled()) {
            logger.debug("Using " + spanBytesDecoder.getClass().getName() + " decoder for received tracing data");
        }
        List<zipkin2.Span> zipkin2Spans = spanBytesDecoder.decodeList(byteArray);

        List<ZipkinSpan> spans = new ArrayList<>();
        for (zipkin2.Span zipkin2Span : zipkin2Spans) {
            ZipkinSpan span = new ZipkinSpan();
            span.setTraceId(zipkin2Span.traceId());
            span.setId(zipkin2Span.id());
            span.setParentId(zipkin2Span.parentId());
            span.setName(zipkin2Span.name());
            span.setServiceName(zipkin2Span.localServiceName() != null ? zipkin2Span.localServiceName() : "");
            span.setKind(zipkin2Span.kind() != null ? zipkin2Span.kind().toString() : "");
            span.setTimestamp(zipkin2Span.timestampAsLong());
            span.setDuration(zipkin2Span.durationAsLong());
            span.setTags(zipkin2Span.tags());
            spans.add(span);
        }
        return spans;
    }

    private Codec() {   // Prevent initialization
    }
}
