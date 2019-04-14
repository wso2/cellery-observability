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

package io.cellery.observability.k8s.api.server.client;

import io.fabric8.kubernetes.api.model.Node;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.CannotRestoreSiddhiAppStateException;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.SiddhiTestHelper;
import org.wso2.siddhi.core.util.persistence.InMemoryPersistenceStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Test Cases for Get Component Pods Stream Processor.
 */
public class GetComponentPodsStreamProcessorTestCase extends BaseTestCase {
    private static final Logger logger = Logger.getLogger(GetComponentPodsStreamProcessorTestCase.class.getName());

    private static final String INPUT_STREAM = "inputStream";

    private AtomicInteger eventCount = new AtomicInteger(0);
    private List<String> nodeValues;
    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> receivedEvents;

    @BeforeClass
    public void initTestCase() {
        nodeValues = k8sApiServerClient.nodes()
                .list()
                .getItems()
                .stream()
                .map((Node node) -> node.getMetadata().getName())
                .collect(Collectors.toList());
    }

    @BeforeMethod
    public void init() {
        eventCount.set(0);
        receivedEvents = new ArrayList<>();
    }

    @AfterMethod
    public void cleanUp() {
        siddhiAppRuntime.shutdown();
        if (logger.isDebugEnabled()) {
            logger.debug("Removing all created test pods");
        }
        k8sApiServerClient.pods()
                .withLabel(TEST_LABEL)
                .delete();
    }

    @Test
    public void testGetPods() throws InterruptedException {
        initializeSiddhiAppRuntime();
        createCelleryComponent("pet-be", "test-a");
        createCelleryComponent("pet-be", "test-b");
        createCelleryComponent("pet-fe", "test-a");
        createCelleryGatewayPod("pet-fe");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-01"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 4, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 4);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 6);
            Assert.assertEquals(data[0], "event-01");
            if ("pet-be--test-a".equals(data[3])) {
                validatePodData("pet-be", "test-a", data);
            } else if ("pet-be--test-b".equals(data[3])) {
                validatePodData("pet-be", "test-b", data);
            } else if ("pet-fe--test-a".equals(data[3])) {
                validatePodData("pet-fe", "test-a", data);
            } else if ("pet-fe--gateway".equals(data[3])) {
                validatePodData("pet-fe", "gateway", data);
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testGetPodsWithOnlyComponents() throws InterruptedException {
        initializeSiddhiAppRuntime();
        createCelleryComponent("pet-be", "test-a");
        createCelleryComponent("pet-be", "test-b");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-02"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 6);
            Assert.assertEquals(data[0], "event-02");
            if ("pet-be--test-a".equals(data[3])) {
                validatePodData("pet-be", "test-a", data);
            } else if ("pet-be--test-b".equals(data[3])) {
                validatePodData("pet-be", "test-b", data);
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testGetPodsWithOnlyGateways() throws InterruptedException {
        initializeSiddhiAppRuntime();
        createCelleryGatewayPod("pet-be");
        createCelleryGatewayPod("pet-fe");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-03"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 6);
            Assert.assertEquals(data[0], "event-03");
            if ("pet-be--gateway".equals(data[3])) {
                validatePodData("pet-be", "gateway", data);
            } else if ("pet-fe--gateway".equals(data[3])) {
                validatePodData("pet-fe", "gateway", data);
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testGetPodsWithRunningNonCelleryPods() throws InterruptedException {
        initializeSiddhiAppRuntime();
        createCelleryComponent("pet-be", "test-a");
        createCelleryComponent("pet-fe", "test-b");
        createNormalPod("normal-test-pod-a");
        createNormalPod("normal-test-pod-b");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-04"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 6);
            Assert.assertEquals(data[0], "event-04");
            if ("pet-be--test-a".equals(data[3])) {
                validatePodData("pet-be", "test-a", data);
            } else if ("pet-fe--test-b".equals(data[3])) {
                validatePodData("pet-fe", "test-b", data);
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testGetPodsWithFailingPods() throws InterruptedException {
        initializeSiddhiAppRuntime();
        createCelleryComponent("pet-be-inst", "test-e");
        createCelleryComponent("pet-fe-inst", "test-f");
        createNormalPod("normal-test-pod-a");
        createFailingContainer("failing-test-pod-b");
        createFailingContainer("failing-test-pod-e");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-05"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 6);
            Assert.assertEquals(data[0], "event-05");
            if ("pet-be-inst--test-e".equals(data[3])) {
                validatePodData("pet-be-inst", "test-e", data);
            } else if ("pet-fe-inst--test-f".equals(data[3])) {
                validatePodData("pet-fe-inst", "test-f", data);
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testOnlyNormalPodsWithFailingPods() throws InterruptedException {
        initializeSiddhiAppRuntime();
        createNormalPod("normal-test-pod-w");
        createNormalPod("normal-test-pod-x");
        createFailingContainer("failing-test-pod-p");
        createFailingContainer("failing-test-pod-q");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-06"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testWithNoPods() throws InterruptedException {
        initializeSiddhiAppRuntime();

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-07"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testWithApiServerDown() throws InterruptedException {
        System.setProperty("kubernetes.master", "localhost");
        initializeSiddhiAppRuntime();

        createCelleryComponent("pet-be-inst", "test-e");
        createCelleryComponent("pet-fe-inst", "test-f");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-08"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);

        System.getProperties().remove("kubernetes.master");
    }

    @Test
    public void testPersistence() throws InterruptedException, CannotRestoreSiddhiAppStateException {
        initializeSiddhiAppRuntime();
        createCelleryComponent("pet-be", "test-a");
        siddhiAppRuntime.persist();
        siddhiAppRuntime.restoreLastRevision();
        createCelleryGatewayPod("pet-fe");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-01"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 6);
            Assert.assertEquals(data[0], "event-01");
            if ("pet-be--test-a".equals(data[3])) {
                validatePodData("pet-be", "test-a", data);
            } else if ("pet-fe--gateway".equals(data[3])) {
                validatePodData("pet-fe", "gateway", data);
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    /**
     * Initialize the Siddhi App Runtime.
     */
    private void initializeSiddhiAppRuntime() {
        String inStreamDefinition = "@App:name(\"test-siddhi-app\")\n" +
                "define stream " + INPUT_STREAM + " (inputValue string);";
        String query = "@info(name = \"query\")\n" +
                "from inputStream#k8sApiServerClient:getComponentPods()\n" +
                "select *\n" +
                "insert into outputStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
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
            logger.debug("Initialized Siddhi App Runtime");
        }
    }

    /**
     * Validate the data from a pod event.
     *
     * @param cell      The cell the pod belonged to
     * @param component The component the pod belonged to
     * @param data      The pod event data
     */
    private void validatePodData(String cell, String component, Object[] data) {
        Assert.assertEquals(data[1], cell);
        Assert.assertEquals(data[2], component);
        Assert.assertNotNull(data[4]);
        Assert.assertTrue(data[5] instanceof String);
        Assert.assertNotEquals(nodeValues.indexOf(data[5]), -1);
    }
}
