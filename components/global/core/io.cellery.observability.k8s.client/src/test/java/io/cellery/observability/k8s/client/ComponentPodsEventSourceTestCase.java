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

package io.cellery.observability.k8s.client;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.log4j.Logger;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.map.keyvalue.sourcemapper.KeyValueSourceMapper;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.source.Source;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.SiddhiTestHelper;
import org.wso2.siddhi.core.util.persistence.InMemoryPersistenceStore;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test case for Component Pod Event Source.
 */
public class ComponentPodsEventSourceTestCase extends BaseSiddhiExtensionTestCase {

    private static final Logger logger = Logger.getLogger(ComponentPodsEventSourceTestCase.class.getName());

    private AtomicInteger eventCount = new AtomicInteger(0);
    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> receivedEvents;

    public ComponentPodsEventSourceTestCase() throws Exception {
        super();
    }

    @BeforeMethod
    public void init() {
        eventCount.set(0);
        receivedEvents = new ArrayList<>();
    }

    @AfterMethod
    public void cleanUp() {
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void testPodEvents() throws Exception {
        String deletionTimestamp = "2019-04-30T13:52:22Z";
        Pod podA = generateCelleryCellComponentPod("pet-be", "test-a");
        podA.getMetadata().setCreationTimestamp(null);
        podA.getSpec().setNodeName(null);

        Pod podB = generateCelleryCellComponentPod("pet-be", "test-c");
        podB.getMetadata().setDeletionTimestamp(deletionTimestamp);

        Pod podC = generateCelleryCompositeComponentPod("hr-comp", "test-d");
        podC.getMetadata().setDeletionTimestamp(deletionTimestamp);

        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(podA, "ADDED"))
                .waitFor(193)
                .andEmit(new WatchEvent(generateCelleryCellComponentPod("pet-fe", "test-b"), "MODIFIED"))
                .waitFor(1)
                .andEmit(new WatchEvent(podB, "DELETED"))
                .waitFor(21)
                .andEmit(new WatchEvent(generateFailingCelleryCellComponentPod("pet-fe", "test-d"), "ERROR"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(generateCelleryCellGatewayPod("pet-fe"), "MODIFIED"))
                .waitFor(53)
                .andEmit(new WatchEvent(generateFailingCelleryCellGatewayPod("pet-be"), "ERROR"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(generateCelleryCompositeComponentPod("employee-comp", "test-a"), "MODIFIED"))
                .waitFor(53)
                .andEmit(new WatchEvent(generateFailingCelleryCompositeComponentPod("stock-comp", "test-b"), "ERROR"))
                .waitFor(1)
                .andEmit(new WatchEvent(podC, "DELETED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 10, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 9);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 9);
            if ("pet-be--test-a".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "test-a");
                Assert.assertEquals(data[4], -1L);
                Assert.assertEquals(data[5], -1L);
                Assert.assertEquals(data[6], "");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "ADDED");
            } else if ("pet-fe--test-b".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "test-b");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], -1L);
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "MODIFIED");
            } else if ("pet-be--test-c".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "test-c");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US)
                        .parse(deletionTimestamp).getTime());
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "DELETED");
            } else if ("pet-fe--test-d".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "test-d");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], -1L);
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "ErrImagePull");
                Assert.assertEquals(data[8], "ERROR");
            } else if ("pet-fe--gateway".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "gateway");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], -1L);
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "MODIFIED");
            } else if ("pet-be--gateway".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "gateway");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], -1L);
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "ErrImagePull");
                Assert.assertEquals(data[8], "ERROR");
            } else if ("employee-comp--test-a".equals(data[3])) {
                Assert.assertEquals(data[0], "employee-comp");
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[2], "test-a");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], -1L);
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "MODIFIED");
            } else if ("stock-comp--test-b".equals(data[3])) {
                Assert.assertEquals(data[0], "stock-comp");
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[2], "test-b");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], -1L);
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "ErrImagePull");
                Assert.assertEquals(data[8], "ERROR");
            } else if ("hr-comp--test-d".equals(data[3])) {
                Assert.assertEquals(data[0], "hr-comp");
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[2], "test-d");
                Assert.assertEquals(data[4], creationTimestamp);
                Assert.assertEquals(data[5], new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US)
                        .parse(deletionTimestamp).getTime());
                Assert.assertEquals(data[6], NODE_NAME);
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "DELETED");
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testPodEventsWithOnlyCellComponents() throws Exception {
        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryCellComponentPod("pet-be", "test-a"), "ADDED"))
                .waitFor(193)
                .andEmit(new WatchEvent(generateCelleryCellComponentPod("pet-fe", "test-b"), "MODIFIED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 9);
            validatePodData(data);
            if ("pet-be--test-a".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "test-a");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "ADDED");
            } else if ("pet-fe--test-b".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "test-b");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "MODIFIED");
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testPodEventsWithOnlyCellGateways() throws Exception {
        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1436)
                .andEmit(new WatchEvent(generateCelleryCellGatewayPod("pet-be"), "ADDED"))
                .waitFor(3)
                .andEmit(new WatchEvent(generateFailingCelleryCellGatewayPod("pet-fe"), "ERROR"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 9);
            validatePodData(data);
            if ("pet-be--gateway".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "gateway");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "ADDED");
            } else if ("pet-fe--gateway".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "gateway");
                Assert.assertEquals(data[7], "ErrImagePull");
                Assert.assertEquals(data[8], "ERROR");
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testPodEventsWithOnlyCompositeComponents() throws Exception {
        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryCompositeComponentPod("hr-comp", "test-a"), "ADDED"))
                .waitFor(193)
                .andEmit(new WatchEvent(generateCelleryCompositeComponentPod("stock-comp", "test-b"), "MODIFIED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 9);
            validatePodData(data);
            if ("hr-comp--test-a".equals(data[3])) {
                Assert.assertEquals(data[0], "hr-comp");
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[2], "test-a");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "ADDED");
            } else if ("stock-comp--test-b".equals(data[3])) {
                Assert.assertEquals(data[0], "stock-comp");
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[2], "test-b");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "MODIFIED");
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testPodEventsWithNoPods() throws Exception {
        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testPodEventsWithApiServerDown() throws Exception {
        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(generateFailingCelleryCellComponentPod("pet-be", "test-a"), "ERROR"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(123)
                .andEmit(new WatchEvent(generateCelleryCellGatewayPod("pet-fe"), "MODIFIED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(123)
                .andEmit(new WatchEvent(generateFailingCelleryCompositeComponentPod("hr-comp", "test-s"), "MODIFIED"))
                .done()
                .once();

        k8sClient.getConfiguration().setMasterUrl("https://localhost");
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testPersistence() throws Exception {
        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryCellComponentPod("pet-be", "test-a"), "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(generateCelleryCellGatewayPod("pet-fe"), "MODIFIED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryCompositeComponentPod("hr-comp", "test-g"), "MODIFIED"))
                .done()
                .once();

        initializeSiddhiAppRuntime();
        Thread.sleep(1200);
        siddhiAppRuntime.persist();
        Thread.sleep(100);
        siddhiAppRuntime.restoreLastRevision();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 4, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 3);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 9);
            validatePodData(data);
            if ("pet-be--test-a".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "test-a");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "ADDED");
            } else if ("pet-fe--gateway".equals(data[3])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[2], "gateway");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "MODIFIED");
            } else if ("hr-comp--test-g".equals(data[3])) {
                Assert.assertEquals(data[0], "hr-comp");
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[2], "test-g");
                Assert.assertEquals(data[7], "Running");
                Assert.assertEquals(data[8], "MODIFIED");
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testTimestampParseFailure() throws Exception {
        Pod cellComponentPod = generateCelleryCellComponentPod("pet-be", "test-a");
        cellComponentPod.getMetadata().setCreationTimestamp("invalid-date-1");
        Pod cellGatewayPod = generateCelleryCellGatewayPod("pet-fe");
        cellGatewayPod.getMetadata().setCreationTimestamp("invalid-date-2");
        Pod compositeComponentPod = generateCelleryCompositeComponentPod("hr-comp", "test-a");
        compositeComponentPod.getMetadata().setCreationTimestamp("invalid-date-1");

        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(cellComponentPod, "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(cellGatewayPod, "MODIFIED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(compositeComponentPod, "DELETED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testShutdownWithFailure() throws Exception {
        k8sServer.expect()
                .withPath(getWatchCellComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryCellComponentPod("pet-be", "test-a"), "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCellGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(generateCelleryCellGatewayPod("pet-be"), "MODIFIED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchCompositeComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryCompositeComponentPod("stock-comp", "test-f"), "ADDED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        for (List<Source> siddhiAppSources : siddhiAppRuntime.getSources()) {
            for (Source source : siddhiAppSources) {
                if (source instanceof ComponentPodsEventSource) {
                    ComponentPodsEventSource componentPodsEventSource = (ComponentPodsEventSource) source;
                    List<Watch> k8sWatches = Whitebox.getInternalState(componentPodsEventSource, "k8sWatches");

                    for (Watch k8sWatch : k8sWatches) {
                        Watcher<Pod> podWatcher = Whitebox.getInternalState(k8sWatch, "watcher");
                        podWatcher.onClose(new KubernetesClientException("Mock Exception"));
                    }
                }
            }
        }
        Assert.assertEquals(eventCount.get(), 2);
    }

    /**
     * Initialize the Siddhi App Runtime with the k8s
     */
    private void initializeSiddhiAppRuntime() {
        String inStreamDefinition = "@App:name(\"test-siddhi-app\")\n" +
                "@source(type=\"k8s-component-pods\", @map(type=\"keyvalue\", " +
                "fail.on.missing.attribute=\"true\"))\n" +
                "define stream K8sComponentPodsStream (instance string, instanceKind string, component string, " +
                "podName string, creationTimestamp long, deletionTimestamp long, nodeName string, status string, " +
                "action string);";
        String query = "@info(name = \"query\")\n" +
                "from K8sComponentPodsStream\n" +
                "select *\n" +
                "insert into OutputStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("keyvalue", KeyValueSourceMapper.class);
        siddhiManager.setPersistenceStore(new InMemoryPersistenceStore());
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + "\n" + query);
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
        if (logger.isDebugEnabled()) {
            logger.debug("EventPrinter Initialized Siddhi App Runtime");
        }
    }

    /**
     * Validate the data from a pod event.
     *
     * @param data      The pod event data
     */
    private void validatePodData(Object[] data) {
        Assert.assertEquals(data[4], creationTimestamp);
        Assert.assertEquals(data[5], -1L);
        Assert.assertEquals(data[6], NODE_NAME);
    }

    /**
     * Get a watch component pods URL.
     *
     * @return The watch component URL
     */
    private String getWatchCellComponentPodsUrl() throws Exception {
        return "/api/v1/namespaces/" + URLEncoder.encode(Constants.NAMESPACE, StandardCharsets.UTF_8.toString())
                + "/pods?labelSelector=" + URLEncoder.encode(Constants.CELL_NAME_LABEL + ","
                + Constants.COMPONENT_NAME_LABEL, StandardCharsets.UTF_8.toString()) + "&watch=true";
    }

    /**
     * Get a watch gateway pods URL.
     *
     * @return The watch component URL
     */
    private String getWatchCellGatewayPodsUrl() throws Exception {
        return "/api/v1/namespaces/" + URLEncoder.encode(Constants.NAMESPACE, StandardCharsets.UTF_8.toString())
                + "/pods?labelSelector=" + URLEncoder.encode(Constants.GATEWAY_NAME_LABEL + ","
                + Constants.CELL_NAME_LABEL, StandardCharsets.UTF_8.toString()) + "&watch=true";
    }

    /**
     * Get a watch component pods URL.
     *
     * @return The watch component URL
     */
    private String getWatchCompositeComponentPodsUrl() throws Exception {
        return "/api/v1/namespaces/" + URLEncoder.encode(Constants.NAMESPACE, StandardCharsets.UTF_8.toString())
                + "/pods?labelSelector=" + URLEncoder.encode(Constants.COMPOSITE_NAME_LABEL + ","
                + Constants.COMPONENT_NAME_LABEL, StandardCharsets.UTF_8.toString()) + "&watch=true";
    }

}
