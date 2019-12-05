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
 *
 */

package io.cellery.observability.telemetry.receiver;

import com.sun.net.httpserver.HttpServer;
import io.cellery.observability.telemetry.receiver.internal.MetricsHandler;
import org.apache.log4j.Logger;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.ConnectionUnavailableException;
import org.wso2.siddhi.core.stream.input.source.Source;
import org.wso2.siddhi.core.stream.input.source.SourceEventListener;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.core.util.transport.OptionHolder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * This class implements the event source, where the received telemetry attributes can be injected to streams.
 */
@Extension(name = "runtime-agent", namespace = "source",
        description = "Observability runtime agent data receiver for Cellery",
        parameters = {
                @Parameter(name = "port",
                        description = "The port which the service should be started on. Default is 9091",
                        type = {DataType.INT},
                        optional = true,
                        defaultValue = "9091"),
        },
        examples = {
                @Example(syntax = "this is synatax",
                        description = "some desc")
        }
)
public class TelemetryEventSource extends Source {
    private static final Logger log = Logger.getLogger(TelemetryEventSource.class);

    private SourceEventListener sourceEventListener;
    private int port;
    private HttpServer httpServer;

    @Override
    public void init(SourceEventListener sourceEventListener, OptionHolder optionHolder, String[] strings,
                     ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        this.sourceEventListener = sourceEventListener;
        this.port = Integer.parseInt(optionHolder.validateAndGetStaticValue(Constants.PORT_EVENT_SOURCE_OPTION_KEY,
                Constants.DEFAULT_RECEIVER_PORT));
    }

    @Override
    public Class[] getOutputEventClasses() {
        return new Class[]{Map.class};
    }

    @Override
    public void connect(ConnectionCallback connectionCallback) throws ConnectionUnavailableException {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", new MetricsHandler(sourceEventListener));
            httpServer.setExecutor(null); // creates a default executor
            httpServer.start();
            log.info("Http server started on port : " + port);
        } catch (IOException e) {
            throw new ConnectionUnavailableException("Unable to start the http server on port: " + port, e);
        }
    }

    @Override
    public void disconnect() {
        if (this.httpServer != null) {
            log.info("Shutting down the http server");
            this.httpServer.stop(0);
        }
    }

    @Override
    public void destroy() {
        // Do Nothing
    }

    @Override
    public void pause() {
        // Do Nothing
    }

    @Override
    public void resume() {
        // Do Nothing
    }

    @Override
    public Map<String, Object> currentState() {
        return null;    // Do Nothing
    }

    @Override
    public void restoreState(Map<String, Object> map) {
        // Do Nothing
    }
}
