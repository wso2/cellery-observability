/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import io.cellery.observability.telemetry.receiver.generated.AttributesOuterClass;
import io.cellery.observability.telemetry.receiver.generated.MixerGrpc;
import io.cellery.observability.telemetry.receiver.generated.Report;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import org.apache.commons.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.map.keyvalue.sourcemapper.KeyValueSourceMapper;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.output.StreamCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * This test case focus on testing the functionality of the GRPC Telemetry service.
 */
public class TelemetryEventSourceTestCase {
    private SiddhiAppRuntime siddhiAppRuntime;
    private int receive = 0;
    private MixerGrpc.MixerBlockingStub mixerBlockingStub;

    @BeforeClass
    public void init() throws IOException {
        initSiddhiApp();
        initClient();
    }

    private void initSiddhiApp() throws IOException {
        String tracingAppContent = IOUtils.toString(this.getClass().
                getResourceAsStream(File.separator + "telemetry-stream.siddhi"), StandardCharsets.UTF_8.name());
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("keyvalue", KeyValueSourceMapper.class);
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(tracingAppContent);
        siddhiAppRuntime.addCallback("TelemetryStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                receive++;
            }
        });
        siddhiAppRuntime.start();
    }

    private void initClient() {
        ManagedChannel managedChannel = NettyChannelBuilder.forAddress("localhost", 9091).usePlaintext().build();
        this.mixerBlockingStub = MixerGrpc.newBlockingStub(managedChannel);
    }

    @Test
    public void report() throws IOException {
        Report.ReportResponse reportResponse = mixerBlockingStub.report(
                loadRequest());
        Assert.assertNotNull(reportResponse);
        Assert.assertEquals(receive, 1);
    }

    private Report.ReportRequest loadRequest() throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(File.separator + "telemetry-sample.txt");
        return Report.ReportRequest.parseFrom(inputStream);
    }

    @Test(dependsOnMethods = "report")
    public void reportMissingMethod() throws IOException {
        Report.ReportRequest reportRequest = loadRequest();
        AttributesOuterClass.CompressedAttributes attributes = reportRequest.getAttributes(0);
        AttributesOuterClass.CompressedAttributes newAttributes = AttributesOuterClass.CompressedAttributes.newBuilder()
                .putAllStringMaps(attributes.getStringMapsMap())
                .build();
        Report.ReportRequest newRequest = Report.ReportRequest.newBuilder()
                .addAllDefaultWords(reportRequest.getDefaultWordsList())
                .addAttributes(newAttributes).build();
        Report.ReportResponse reportResponse = mixerBlockingStub.report(newRequest);
        Assert.assertNotNull(reportResponse);
        Assert.assertEquals(receive, 2);
    }

    @AfterClass
    public void cleanup() {
        siddhiAppRuntime.shutdown();
    }
}
