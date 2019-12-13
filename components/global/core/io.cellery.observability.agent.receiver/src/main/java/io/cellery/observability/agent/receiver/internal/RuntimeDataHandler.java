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

package io.cellery.observability.agent.receiver.internal;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.cellery.observability.auth.Permission;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.stream.input.source.SourceEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * This class is responsible for handling metrics received from the http server.
 */
public class RuntimeDataHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(RuntimeDataHandler.class);
    private SourceEventListener sourceEventListener;
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TELEMETRY_ENTRY_RUNTIME_KEY = "runtime";
    private static final String TELEMETRY_ENTRY_DATA_KEY = "data";

    private static final String RUNTIME_ATTRIBUTE = "runtime";

    public RuntimeDataHandler(SourceEventListener sourceEventListener) {
        this.sourceEventListener = sourceEventListener;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (httpExchange.getRequestBody() != null) {
            String runtime = null;
            try (GZIPInputStream gis = new GZIPInputStream(httpExchange.getRequestBody());
                    BufferedReader bf = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
                String json = IOUtils.toString(bf);
                if (log.isDebugEnabled()) {
                    log.debug("Received metrics from the adapter : " + json);
                }

                JsonObject receivedTelemetryJsonObject = jsonParser.parse(json).getAsJsonObject();
                runtime = receivedTelemetryJsonObject.getAsJsonPrimitive(TELEMETRY_ENTRY_RUNTIME_KEY)
                        .getAsString();
                JsonArray data = receivedTelemetryJsonObject.getAsJsonArray(TELEMETRY_ENTRY_DATA_KEY);

                String authorizationHeader = httpExchange.getRequestHeaders().getFirst(HEADER_AUTHORIZATION);
                Permission requiredPermission = new Permission(runtime, StringUtils.EMPTY,
                        Collections.singletonList(Permission.Action.DATA_PUBLISH));
                if (ServiceHolder.getAuthProvider().isTokenValid(authorizationHeader, requiredPermission)) {
                    for (JsonElement datum : data) {
                        JsonObject telemetryEntry = datum.getAsJsonObject();
                        Map<String, Object> attributes = new HashMap<>();
                        for (String key : telemetryEntry.keySet()) {
                            attributes.put(key, this.getValue(telemetryEntry.get(key)));
                        }
                        // Runtime should be set last to avoid security issues
                        attributes.put(RUNTIME_ATTRIBUTE, runtime);
                        sourceEventListener.onEvent(attributes, new String[0]);
                    }
                    httpExchange.sendResponseHeaders(200, -1);
                } else {
                    log.warn("Blocked unauthorized data publish attempt from "
                            + (runtime == null ? " unknown runtime " : "runtime " + runtime));
                    httpExchange.sendResponseHeaders(401, -1);
                }
            } catch (Throwable t) {
                log.error("Failed to process received data from "
                        + (runtime == null ? " unknown runtime " : "runtime " + runtime), t);
                httpExchange.sendResponseHeaders(500, -1);
            }
        } else {
            log.warn("Ignoring received request with empty data");
            httpExchange.sendResponseHeaders(500, -1);
        }
        httpExchange.close();
    }

    /**
     * Get the actual java object for the Json Element.
     *
     * @param jsonElement The json element of which the value should be extracted
     * @return The extracted value object of the json primitive
     */
    private Object getValue(JsonElement jsonElement) {
        Object value;
        if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
            if (jsonPrimitive.isString()) {
                value = jsonPrimitive.getAsString();
            } else if (jsonPrimitive.isBoolean()) {
                value = jsonPrimitive.getAsBoolean();
            } else if (jsonPrimitive.isNumber()) {
                double doubleValue = jsonPrimitive.getAsDouble();
                if (doubleValue % 1 == 0) {
                    value = (long) doubleValue;
                } else {
                    value = doubleValue;
                }
            } else {
                value = jsonPrimitive.getAsString();
            }
        } else if (jsonElement.isJsonNull()) {
            value = null;
        } else {
            value = gson.toJson(jsonElement);
        }
        return value;
    }
}
