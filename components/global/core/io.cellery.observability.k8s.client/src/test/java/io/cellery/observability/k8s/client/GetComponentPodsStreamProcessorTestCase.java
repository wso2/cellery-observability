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
import io.fabric8.kubernetes.api.model.PodListBuilder;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.SiddhiTestHelper;
import org.wso2.siddhi.core.util.persistence.InMemoryPersistenceStore;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Cases for Get Component Pods Stream Processor.
 */
public class GetComponentPodsStreamProcessorTestCase extends BaseTestCase {

    private static final Logger logger = Logger.getLogger(GetComponentPodsStreamProcessorTestCase.class.getName());
    private static final String INPUT_STREAM = "inputStream";

    private AtomicInteger eventCount = new AtomicInteger(0);
    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> receivedEvents;

    public GetComponentPodsStreamProcessorTestCase() throws Exception {
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
    public void testGetPods() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetComponentPods(
                generateCelleryComponentPod("pet-be", "test-a"),
                generateCelleryComponentPod("pet-be", "test-b"),
                generateCelleryComponentPod("pet-fe", "test-a")
        );
        expectGetGatewayPods(generateCelleryGatewayPod("pet-fe"));

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-01"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 4, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 2);
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
    public void testGetPodsWithOnlyComponents() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetComponentPods(
                generateCelleryComponentPod("pet-be", "test-a"),
                generateCelleryComponentPod("pet-be", "test-b")
        );
        expectGetGatewayPods();

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-02"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 2);
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
    public void testGetPodsWithOnlyGateways() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetComponentPods();
        expectGetGatewayPods(
                generateCelleryGatewayPod("pet-be"),
                generateCelleryGatewayPod("pet-fe")
        );

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-03"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 2);
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
    public void testWithNoPods() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetComponentPods();
        expectGetGatewayPods();

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-04"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 2);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testWithApiServerDown() throws Exception {
        initializeSiddhiAppRuntime();

        expectGetComponentPods(
                generateCelleryComponentPod("pet-be-inst", "test-e"),
                generateCelleryComponentPod("pet-fe-inst", "test-f")
        );

        String originalMaster = k8sClient.getConfiguration().getMasterUrl();
        k8sClient.getConfiguration().setMasterUrl("https://localhost");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-05"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 0);
        Assert.assertEquals(eventCount.get(), 0);

        k8sClient.getConfiguration().setMasterUrl(originalMaster);

        inputHandler.send(new Object[]{"event-05"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 2);
        Assert.assertEquals(eventCount.get(), 2);
    }

    @Test
    public void testPersistence() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetComponentPods(generateCelleryComponentPod("pet-be", "test-a"));
        siddhiAppRuntime.persist();
        siddhiAppRuntime.restoreLastRevision();
        expectGetGatewayPods(generateCelleryGatewayPod("pet-fe"));

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-06"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 2);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 6);
            Assert.assertEquals(data[0], "event-06");
            if ("pet-be--test-a".equals(data[3])) {
                validatePodData("pet-be", "test-a", data);
            } else if ("pet-fe--gateway".equals(data[3])) {
                validatePodData("pet-fe", "gateway", data);
            } else {
                Assert.fail("Received unexpect pod " + data[3]);
            }
        }
    }

    @Test
    public void testTimestampParseFailure() throws Exception {
        initializeSiddhiAppRuntime();
        Pod componentPod = generateCelleryComponentPod("pet-be", "test-a");
        componentPod.getMetadata().setCreationTimestamp("invalid-date-1");
        expectGetComponentPods(componentPod);
        Pod gatewayPod = generateCelleryGatewayPod("pet-fe");
        gatewayPod.getMetadata().setCreationTimestamp("invalid-date-2");
        expectGetGatewayPods(gatewayPod);

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-07"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 1);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testInvalidParams() {
        String inStreamDefinition = "@App:name(\"test-siddhi-app\")\n" +
                "define stream " + INPUT_STREAM + " (inputValue string);";
        String query = "@info(name = \"query\")\n" +
                "from inputStream#k8sClient:getComponentPods(\"unexpected-value\")\n" +
                "select *\n" +
                "insert into outputStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + "\n" + query);
        siddhiAppRuntime.start();
    }

    /**
     * Initialize the Siddhi App Runtime.
     */
    private void initializeSiddhiAppRuntime() {
        String inStreamDefinition = "@App:name(\"test-siddhi-app\")\n" +
                "define stream " + INPUT_STREAM + " (inputValue string);";
        String query = "@info(name = \"query\")\n" +
                "from inputStream#k8sClient:getComponentPods()\n" +
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
        Assert.assertEquals(data[4], creationTimestamp);
        Assert.assertEquals(data[5], NODE_NAME);
    }

    /**
     * Expect a get component pods call to Mock K8s Server.
     *
     * @param returnPods The pods to be returned
     */
    private void expectGetComponentPods(Pod... returnPods) throws Exception {
        expectGetPods(Constants.CELL_NAME_LABEL + "," + Constants.COMPONENT_NAME_LABEL, returnPods);
    }

    /**
     * Expect a get gateway pods call to Mock K8s Server.
     *
     * @param returnPods The pods to be returned
     */
    private void expectGetGatewayPods(Pod... returnPods) throws Exception {
        expectGetPods(Constants.GATEWAY_NAME_LABEL + "," + Constants.CELL_NAME_LABEL, returnPods);
    }

    /**
     * Expect a get pods call to Mock K8s Server.
     *
     * @param labelSelector The label selector of the API call
     * @param returnPods The pods to be returned
     */
    private void expectGetPods(String labelSelector, Pod... returnPods) throws Exception {
        String path = "/api/v1/namespaces/" + URLEncoder.encode(Constants.NAMESPACE, StandardCharsets.UTF_8.toString())
                + "/pods?labelSelector=" + URLEncoder.encode(labelSelector, StandardCharsets.UTF_8.toString())
                + "&fieldSelector=" + URLEncoder.encode(Constants.STATUS_FIELD + "="
                + Constants.STATUS_FIELD_RUNNING_VALUE, StandardCharsets.UTF_8.toString());
        k8sServer.expect()
                .withPath(path)
                .andReturn(200, new PodListBuilder().addToItems(returnPods).build())
                .once();
    }
}
