package io.cellery.observability.telemetry.receiver.internal;

import com.sun.net.httpserver.HttpHandler;
import io.cellery.observability.telemetry.receiver.TelemetryEventSource;
import io.cellery.observability.telemetry.receiver.Utils;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.stream.input.source.SourceEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

/**
 * This class is responsible for handling metrics received from the http server.
 */
public class MetricsHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(TelemetryEventSource.class);
    private SourceEventListener sourceEventListener;

    public MetricsHandler(SourceEventListener sourceEventListener) {
        this.sourceEventListener = sourceEventListener;
    }

    @Override
    public void handle(com.sun.net.httpserver.HttpExchange httpExchange) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        InputStreamReader in = null;
        BufferedReader bufferedReader = null;
        try {
            if (httpExchange != null && httpExchange.getRequestBody() != null) {
                in = new InputStreamReader(httpExchange.getRequestBody(),
                        Charset.defaultCharset());
                bufferedReader = new BufferedReader(in);
                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    stringBuilder.append((char) cp);
                }
                bufferedReader.close();
            }
            Objects.requireNonNull(in).close();
        } catch (Exception e) {
            throw new RuntimeException("Exception while reading: ", e);
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }

        HashMap<String, Object> testMap = Utils.toMap(stringBuilder.toString());

        sourceEventListener.onEvent(testMap, new String[0]);

        if (httpExchange != null) {
            String resStr = "Here is the response";
            byte[] response = resStr.getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(200, response.length);
            OutputStream os = httpExchange.getResponseBody();
            os.write(response);
            os.close();
        }

    }

}
