/*
 * Copyright (c) ${year} WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 *  under the License.
 *
 */

package io.cellery.observability.telemetry.receiver.internal;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import io.cellery.observability.telemetry.receiver.TelemetryEventSource;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.stream.input.source.SourceEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * This class is responsible for handling metrics received from the http server.
 */
public class MetricsHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(TelemetryEventSource.class);
    private static final Gson gson = new Gson();
    private SourceEventListener sourceEventListener;

    public MetricsHandler(SourceEventListener sourceEventListener) {
        this.sourceEventListener = sourceEventListener;
    }

    @Override public void handle(com.sun.net.httpserver.HttpExchange httpExchange) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader in = null;
        BufferedReader bufferedReader = null;
        try {
            if (httpExchange != null && httpExchange.getRequestBody() != null) {
                in = new InputStreamReader(httpExchange.getRequestBody(), Charset.defaultCharset());
                bufferedReader = new BufferedReader(in);
                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    stringBuilder.append((char) cp);
                }
                bufferedReader.close();
                log.info(stringBuilder.toString());
                Metric metric = gson.fromJson(stringBuilder.toString(), Metric.class);

                sourceEventListener.onEvent(metric.toMap(), new String[0]);

                String resStr = "Success";
                byte[] response = resStr.getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(200, response.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(response);
                os.close();
            }
            Objects.requireNonNull(in).close();

        } catch (RuntimeException e) {
            throw new RuntimeException("Exception while reading: ", e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

    }

}
