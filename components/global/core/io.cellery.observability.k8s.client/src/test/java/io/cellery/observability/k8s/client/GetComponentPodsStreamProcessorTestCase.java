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
public class GetComponentPodsStreamProcessorTestCase extends BaseSiddhiExtensionTestCase {

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
        expectGetCellComponentPods(
                generateCelleryCellComponentPod("pet-be", "test-a"),
                generateCelleryCellComponentPod("pet-be", "test-b"),
                generateCelleryCellComponentPod("pet-fe", "test-a")
        );
        expectGetCellGatewayPods(generateCelleryCellGatewayPod("pet-fe"));
        expectGetCompositeComponentPods(
                generateCelleryCompositeComponentPod("employee-comp", "employee"),
                generateCelleryCompositeComponentPod("employee-comp", "salary")
        );

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-01"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 7, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 3);
        Assert.assertEquals(eventCount.get(), 6);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            Assert.assertEquals(data[0], "event-01");
            if ("pet-be--test-a".equals(data[4])) {
                validatePodData("pet-be", "Cell", "test-a", data);
            } else if ("pet-be--test-b".equals(data[4])) {
                validatePodData("pet-be", "Cell", "test-b", data);
            } else if ("pet-fe--test-a".equals(data[4])) {
                validatePodData("pet-fe", "Cell", "test-a", data);
            } else if ("pet-fe--gateway".equals(data[4])) {
                validatePodData("pet-fe", "Cell", "gateway", data);
            } else if ("employee-comp--employee".equals(data[4])) {
                validatePodData("employee-comp", "Composite", "employee", data);
            } else if ("employee-comp--salary".equals(data[4])) {
                validatePodData("employee-comp", "Composite", "salary", data);
            } else {
                Assert.fail("Received unexpect pod " + data[4]);
            }
        }
    }

    @Test
    public void testGetPodsWithOnlyCellComponents() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetCellComponentPods(
                generateCelleryCellComponentPod("pet-be", "test-a"),
                generateCelleryCellComponentPod("pet-be", "test-b")
        );
        expectGetCellGatewayPods();
        expectGetCompositeComponentPods();

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-02"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 3);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            Assert.assertEquals(data[0], "event-02");
            if ("pet-be--test-a".equals(data[4])) {
                validatePodData("pet-be", "Cell", "test-a", data);
            } else if ("pet-be--test-b".equals(data[4])) {
                validatePodData("pet-be", "Cell", "test-b", data);
            } else {
                Assert.fail("Received unexpect pod " + data[4]);
            }
        }
    }

    @Test
    public void testGetPodsWithOnlyCellGateways() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetCellComponentPods();
        expectGetCellGatewayPods(
                generateCelleryCellGatewayPod("pet-be"),
                generateCelleryCellGatewayPod("pet-fe")
        );
        expectGetCompositeComponentPods();

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-03"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 3);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            Assert.assertEquals(data[0], "event-03");
            if ("pet-be--gateway".equals(data[4])) {
                validatePodData("pet-be", "Cell", "gateway", data);
            } else if ("pet-fe--gateway".equals(data[4])) {
                validatePodData("pet-fe", "Cell", "gateway", data);
            } else {
                Assert.fail("Received unexpect pod " + data[4]);
            }
        }
    }

    @Test
    public void testGetPodsWithOnlyComponentComponents() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetCellComponentPods();
        expectGetCellGatewayPods();
        expectGetCompositeComponentPods(
                generateCelleryCompositeComponentPod("stock-comp", "stock"),
                generateCelleryCompositeComponentPod("hr-comp", "hr")
        );

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-02"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 3);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            Assert.assertEquals(data[0], "event-02");
            if ("hr-comp--hr".equals(data[4])) {
                validatePodData("hr-comp", "Composite", "hr", data);
            } else if ("stock-comp--stock".equals(data[4])) {
                validatePodData("stock-comp", "Composite", "stock", data);
            } else {
                Assert.fail("Received unexpect pod " + data[4]);
            }
        }
    }

    @Test
    public void testWithNoPods() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetCellComponentPods();
        expectGetCellGatewayPods();
        expectGetCompositeComponentPods();

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-04"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 3);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testWithApiServerDown() throws Exception {
        initializeSiddhiAppRuntime();

        expectGetCellComponentPods(generateCelleryCellComponentPod("pet-be-inst", "test-e"));
        expectGetCellGatewayPods(generateCelleryCellGatewayPod("pet-fe"));
        expectGetCompositeComponentPods(generateCelleryCompositeComponentPod("hr-comp", "hr"));

        String originalMaster = k8sClient.getConfiguration().getMasterUrl();
        k8sClient.getConfiguration().setMasterUrl("https://localhost");

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-05"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 0);
        Assert.assertEquals(eventCount.get(), 0);

        k8sClient.getConfiguration().setMasterUrl(originalMaster);

        inputHandler.send(new Object[]{"event-05"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 4, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 3);
        Assert.assertEquals(eventCount.get(), 3);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            Assert.assertEquals(data[0], "event-05");
            if ("pet-be-inst--test-e".equals(data[4])) {
                validatePodData("pet-be-inst", "Cell", "test-e", data);
            } else if ("hr-comp--hr".equals(data[4])) {
                validatePodData("hr-comp", "Composite", "hr", data);
            } else if ("pet-fe--gateway".equals(data[4])) {
                validatePodData("pet-fe", "Cell", "gateway", data);
            } else {
                Assert.fail("Received unexpect pod " + data[4]);
            }
        }
    }

    @Test
    public void testPersistence() throws Exception {
        initializeSiddhiAppRuntime();
        expectGetCellComponentPods(generateCelleryCellComponentPod("pet-be", "test-a"));
        expectGetCellGatewayPods(generateCelleryCellGatewayPod("pet-fe"));
        siddhiAppRuntime.persist();
        siddhiAppRuntime.restoreLastRevision();
        expectGetCompositeComponentPods(generateCelleryCompositeComponentPod("stock-comp", "stock"));

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler(INPUT_STREAM);
        inputHandler.send(new Object[]{"event-06"});
        SiddhiTestHelper.waitForEvents(WAIT_TIME, 4, eventCount, TIMEOUT);
        Assert.assertEquals(k8sServer.getMockServer().getRequestCount(), 3);
        Assert.assertEquals(eventCount.get(), 3);
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            Assert.assertEquals(data[0], "event-06");
            if ("pet-be--test-a".equals(data[4])) {
                validatePodData("pet-be", "Cell", "test-a", data);
            } else if ("pet-fe--gateway".equals(data[4])) {
                validatePodData("pet-fe", "Cell", "gateway", data);
            } else if ("stock-comp--stock".equals(data[4])) {
                validatePodData("stock-comp", "Composite", "stock", data);
            } else {
                Assert.fail("Received unexpect pod " + data[4]);
            }
        }
    }

    @Test
    public void testTimestampParseFailure() throws Exception {
        initializeSiddhiAppRuntime();
        Pod cellComponentPod = generateCelleryCellComponentPod("pet-be", "test-a");
        cellComponentPod.getMetadata().setCreationTimestamp("invalid-date-1");
        expectGetCellComponentPods(cellComponentPod);
        Pod cellGatewayPod = generateCelleryCellGatewayPod("pet-fe");
        cellGatewayPod.getMetadata().setCreationTimestamp("invalid-date-3");
        expectGetCellGatewayPods(cellGatewayPod);
        Pod compositeComponentPod = generateCelleryCellComponentPod("employee-comp", "employee");
        compositeComponentPod.getMetadata().setCreationTimestamp("invalid-date-2");
        expectGetCompositeComponentPods(compositeComponentPod);

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
                "select inputValue, instance, instanceKind, component, podName, creationTimestamp, nodeName\n" +
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
     * @param instance  The cell the pod belonged to
     * @param kind      The kind of the instance
     * @param component The component the pod belonged to
     * @param data      The pod event data
     */
    private void validatePodData(String instance, String kind, String component, Object[] data) {
        Assert.assertEquals(data[1], instance);
        Assert.assertEquals(data[2], kind);
        Assert.assertEquals(data[3], component);
        Assert.assertEquals(data[5], creationTimestamp);
        Assert.assertEquals(data[6], NODE_NAME);
    }

    /**
     * Expect a get cell component pods call to Mock K8s Server.
     *
     * @param returnPods The pods to be returned
     */
    private void expectGetCellComponentPods(Pod... returnPods) throws Exception {
        expectGetPods(Constants.CELL_NAME_LABEL + "," + Constants.COMPONENT_NAME_LABEL, returnPods);
    }

    /**
     * Expect a get cell gateway pods call to Mock K8s Server.
     *
     * @param returnPods The pods to be returned
     */
    private void expectGetCellGatewayPods(Pod... returnPods) throws Exception {
        expectGetPods(Constants.GATEWAY_NAME_LABEL + "," + Constants.CELL_NAME_LABEL, returnPods);
    }

    /**
     * Expect a get composite component pods call to Mock K8s Server.
     *
     * @param returnPods The pods to be returned
     */
    private void expectGetCompositeComponentPods(Pod... returnPods) throws Exception {
        expectGetPods(Constants.COMPOSITE_NAME_LABEL + "," + Constants.COMPONENT_NAME_LABEL, returnPods);
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
