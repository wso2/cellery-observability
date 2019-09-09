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
public class ComponentPodsEventSourceTestCase extends BaseTestCase {

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
        Pod podA = generateCelleryComponentPod("pet-be", "test-a");
        podA.getMetadata().setCreationTimestamp(null);
        podA.getSpec().setNodeName(null);

        Pod podC = generateCelleryComponentPod("pet-be", "test-c");
        podC.getMetadata().setDeletionTimestamp(deletionTimestamp);

        k8sServer.expect()
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(podA, "ADDED"))
                .waitFor(193)
                .andEmit(new WatchEvent(generateCelleryComponentPod("pet-fe", "test-b"), "MODIFIED"))
                .waitFor(1)
                .andEmit(new WatchEvent(podC, "DELETED"))
                .waitFor(21)
                .andEmit(new WatchEvent(generateFailingCelleryComponentPod("pet-fe", "test-d"), "ERROR"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(generateCelleryGatewayPod("pet-fe"), "MODIFIED"))
                .waitFor(53)
                .andEmit(new WatchEvent(generateFailingCelleryGatewayPod("pet-be"), "ERROR"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 6, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 6);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 8);
            if ("pet-be--test-a".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "test-a");
                Assert.assertEquals(data[3], -1L);
                Assert.assertEquals(data[4], -1L);
                Assert.assertEquals(data[5], "");
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "ADDED");
            } else if ("pet-fe--test-b".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "test-b");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[4], -1L);
                Assert.assertEquals(data[5], NODE_NAME);
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "MODIFIED");
            } else if ("pet-be--test-c".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "test-c");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[4], new SimpleDateFormat(Constants.K8S_DATE_FORMAT, Locale.US)
                        .parse(deletionTimestamp).getTime());
                Assert.assertEquals(data[5], NODE_NAME);
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "DELETED");
            } else if ("pet-fe--test-d".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "test-d");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[4], -1L);
                Assert.assertEquals(data[5], NODE_NAME);
                Assert.assertEquals(data[6], "ErrImagePull");
                Assert.assertEquals(data[7], "ERROR");
            } else if ("pet-fe--gateway".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "gateway");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[4], -1L);
                Assert.assertEquals(data[5], NODE_NAME);
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "MODIFIED");
            } else if ("pet-be--gateway".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "gateway");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[4], -1L);
                Assert.assertEquals(data[5], NODE_NAME);
                Assert.assertEquals(data[6], "ErrImagePull");
                Assert.assertEquals(data[7], "ERROR");
            } else {
                Assert.fail("Received unexpect pod " + data[2]);
            }
        }
    }

    @Test
    public void testPodEventsWithOnlyComponents() throws Exception {
        k8sServer.expect()
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryComponentPod("pet-be", "test-a"), "ADDED"))
                .waitFor(193)
                .andEmit(new WatchEvent(generateCelleryComponentPod("pet-fe", "test-b"), "MODIFIED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 8);
            validatePodData(data);
            if ("pet-be--test-a".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "test-a");
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "ADDED");
            } else if ("pet-fe--test-b".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "test-b");
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "MODIFIED");
            } else {
                Assert.fail("Received unexpect pod " + data[2]);
            }
        }
    }

    @Test
    public void testPodEventsWithOnlyGateways() throws Exception {
        k8sServer.expect()
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1436)
                .andEmit(new WatchEvent(generateCelleryGatewayPod("pet-be"), "ADDED"))
                .waitFor(3)
                .andEmit(new WatchEvent(generateFailingCelleryGatewayPod("pet-fe"), "ERROR"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 8);
            validatePodData(data);
            if ("pet-be--gateway".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "gateway");
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "ADDED");
            } else if ("pet-fe--gateway".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "gateway");
                Assert.assertEquals(data[6], "ErrImagePull");
                Assert.assertEquals(data[7], "ERROR");
            } else {
                Assert.fail("Received unexpect pod " + data[2]);
            }
        }
    }

    @Test
    public void testPodEventsWithNoPods() throws Exception {
        k8sServer.expect()
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
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
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(generateFailingCelleryComponentPod("pet-be", "test-a"), "ERROR"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(123)
                .andEmit(new WatchEvent(generateCelleryGatewayPod("pet-fe"), "MODIFIED"))
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
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryComponentPod("pet-be", "test-a"), "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(generateCelleryGatewayPod("pet-fe"), "MODIFIED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();
        Thread.sleep(1200);
        siddhiAppRuntime.persist();
        Thread.sleep(100);
        siddhiAppRuntime.restoreLastRevision();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 8);
            validatePodData(data);
            if ("pet-be--test-a".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-be");
                Assert.assertEquals(data[1], "test-a");
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "ADDED");
            } else if ("pet-fe--gateway".equals(data[2])) {
                Assert.assertEquals(data[0], "pet-fe");
                Assert.assertEquals(data[1], "gateway");
                Assert.assertEquals(data[6], "Running");
                Assert.assertEquals(data[7], "MODIFIED");
            } else {
                Assert.fail("Received unexpect pod " + data[2]);
            }
        }
    }

    @Test
    public void testTimestampParseFailure() throws Exception {
        Pod componentPod = generateCelleryComponentPod("pet-be", "test-a");
        componentPod.getMetadata().setCreationTimestamp("invalid-date-1");
        Pod gatewayPod = generateCelleryGatewayPod("pet-fe");
        gatewayPod.getMetadata().setCreationTimestamp("invalid-date-2");

        k8sServer.expect()
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(componentPod, "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(gatewayPod, "MODIFIED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testShutdownWithFailure() throws Exception {
        k8sServer.expect()
                .withPath(getWatchComponentPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(generateCelleryComponentPod("pet-be", "test-a"), "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(getWatchGatewayPodsUrl())
                .andUpgradeToWebSocket()
                .open()
                .waitFor(2264)
                .andEmit(new WatchEvent(generateCelleryGatewayPod("pet-be"), "MODIFIED"))
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
                "define stream k8sComponentPodsStream (cell string, component string, podName string, " +
                "creationTimestamp long, deletionTimestamp long, nodeName string, status string, action string);";
        String query = "@info(name = \"query\")\n" +
                "from k8sComponentPodsStream\n" +
                "select *\n" +
                "insert into outputStream;";
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
        Assert.assertEquals(data[3], creationTimestamp);
        Assert.assertEquals(data[4], -1L);
        Assert.assertEquals(data[5], NODE_NAME);
    }

    /**
     * Get a watch component pods URL.
     *
     * @return The watch component URL
     */
    private String getWatchComponentPodsUrl() throws Exception {
        return "/api/v1/namespaces/" + URLEncoder.encode(Constants.NAMESPACE, StandardCharsets.UTF_8.toString())
                + "/pods?labelSelector=" + URLEncoder.encode(Constants.CELL_NAME_LABEL + ","
                + Constants.COMPONENT_NAME_LABEL, StandardCharsets.UTF_8.toString()) + "&watch=true";
    }

    /**
     * Get a watch gateway pods URL.
     *
     * @return The watch component URL
     */
    private String getWatchGatewayPodsUrl() throws Exception {
        return "/api/v1/namespaces/" + URLEncoder.encode(Constants.NAMESPACE, StandardCharsets.UTF_8.toString())
                + "/pods?labelSelector=" + URLEncoder.encode(Constants.GATEWAY_NAME_LABEL + ","
                + Constants.CELL_NAME_LABEL, StandardCharsets.UTF_8.toString()) + "&watch=true";
    }
}
