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

import io.fabric8.kubernetes.api.model.Node;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.map.keyvalue.sourcemapper.KeyValueSourceMapper;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.SiddhiTestHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Test case for Component Pod Event Source.
 */
public class ComponentPodsEventSourceTestCase extends BaseTestCase {

    private static final Logger logger = Logger.getLogger(ComponentPodsEventSourceTestCase.class.getName());

    private AtomicInteger eventCount = new AtomicInteger(0);
    private List<String> nodeValues;
    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> receivedEvents;

    @BeforeClass
    public void initTestCase() {
        nodeValues = k8sClient.nodes()
                .list()
                .getItems()
                .stream()
                .map((Node node) -> node.getMetadata().getName())
                .collect(Collectors.toList());
        nodeValues.add("");
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
        cleanUpTestPods();
    }

    @Test
    public void testCreatePods() throws Exception {
        initializeSiddhiAppRuntime();
        createCelleryComponentPod("pet-be", "test-a");
        createCelleryGatewayPod("pet-fe");

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 16, eventCount, TIMEOUT);
        Map<String, String[]> podInfo = new HashMap<>();
        Set<String> podStatuses = new HashSet<>();
        Set<String> actions = new HashSet<>();
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            validatePodData(data);
            podInfo.put((String) data[2], new String[]{(String) data[0], (String) data[1]});
            podStatuses.add((String) data[5]);
            actions.add((String) data[6]);
        }
        assertEquals(podInfo, new HashMap<String, String[]>() {
            {
                put("pet-be--test-a", new String[]{"pet-be", "test-a"});
                put("pet-fe--gateway", new String[]{"pet-fe", "gateway"});
            }
        });
        assertEquals(podStatuses, new HashSet<>(Arrays.asList("Pending", "Running")));
        assertEquals(actions, new HashSet<>(Arrays.asList("ADDED", "MODIFIED")));
    }

    @Test
    public void testRemovePods() throws Exception {
        createCelleryComponentPod("pet-be", "test-a");  // Missed event (will be replayed)
        createCelleryComponentPod("pet-be", "test-b");  // Missed event (will be replayed)
        initializeSiddhiAppRuntime();
        createCelleryGatewayPod("pet-fe");
        deletePod("pet-be--test-a");
        deletePod("pet-fe--gateway");

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 8, eventCount, TIMEOUT);
        // K8s server replays the missed events as well
        Map<String, String[]> podInfo = new HashMap<>();
        Set<String> podStatuses = new HashSet<>();
        Set<String> actions = new HashSet<>();
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            validatePodData(data);
            podInfo.put((String) data[2], new String[]{(String) data[0], (String) data[1]});
            podStatuses.add((String) data[5]);
            actions.add((String) data[6]);
            if ("DELETED".equals(data[6])) {
                Assert.assertNotNull(data[6]);
                Assert.assertNotEquals("", data[6]);
            }
        }
        assertEquals(podInfo, new HashMap<String, String[]>() {
            {
                put("pet-be--test-a", new String[]{"pet-be", "test-a"});
                put("pet-be--test-b", new String[]{"pet-be", "test-b"});
                put("pet-fe--gateway", new String[]{"pet-fe", "gateway"});
            }
        });
        assertEquals(podStatuses, new HashSet<>(Arrays.asList("Pending", "Running")));
        assertEquals(actions, new HashSet<>(Arrays.asList("ADDED", "MODIFIED", "DELETED")));
    }

    @Test
    public void testModifyPods() throws Exception {
        initializeSiddhiAppRuntime();
        createCelleryComponentPod("pet-be", "test-a");
        k8sClient.pods()
                .inNamespace(Constants.NAMESPACE)
                .withName("pet-be--test-a")
                .edit()
                .editSpec()
                .editFirstContainer()
                .withNewImage("scratch")
                .endContainer()
                .endSpec()
                .done();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 8, eventCount, TIMEOUT);
        // K8s server replays the missed events as well
        Map<String, String[]> podInfo = new HashMap<>();
        Set<String> podStatuses = new HashSet<>();
        Set<String> actions = new HashSet<>();
        for (Event event : receivedEvents) {
            Object[] data = event.getData();
            Assert.assertEquals(data.length, 7);
            validatePodData(data);
            podInfo.put((String) data[2], new String[]{(String) data[0], (String) data[1]});
            podStatuses.add((String) data[5]);
            actions.add((String) data[6]);
        }
        assertEquals(podInfo, new HashMap<String, String[]>() {
            {
                put("pet-be--test-a", new String[]{"pet-be", "test-a"});
            }
        });
        assertEquals(podStatuses, new HashSet<>(Arrays.asList("Pending", "Running")));
        assertEquals(actions, new HashSet<>(Arrays.asList("ADDED", "MODIFIED")));
    }

    /**
     * Initialize the Siddhi App Runtime with the k8s
     */
    private void initializeSiddhiAppRuntime() {
        String inStreamDefinition = "@App:name(\"test-siddhi-app\")\n" +
                "@source(type=\"k8s-component-pods\", @map(type=\"keyvalue\", " +
                "fail.on.missing.attribute=\"false\"))\n" +
                "define stream k8sComponentPodsStream (cell string, component string, name string, " +
                "creationTimestamp long, nodeName string, status string, action string);";
        String query = "@info(name = \"query\")\n" +
                "from k8sComponentPodsStream\n" +
                "select *\n" +
                "insert into outputStream;";
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("keyvalue", KeyValueSourceMapper.class);
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
        Assert.assertNotNull(data[3]);
        Assert.assertTrue(data[4] instanceof String);
        Assert.assertNotEquals(nodeValues.indexOf(data[4]), -1);
    }

    /**
     * Assert whether the two pod info maps are equal.
     *
     * @param actual The actual 2D array
     * @param expected The expected 2D array
     */
    private void assertEquals(Map<String, String[]> actual, Map<String, String[]> expected) {
        Assert.assertEquals(actual.size(), expected.size());
        for (Map.Entry<String, String[]> expectedEntry : expected.entrySet()) {
            String[] actualPodInfo = actual.get(expectedEntry.getKey());
            String[] expectedPodInfo = expectedEntry.getValue();
            Assert.assertNotNull(actualPodInfo, "expected pod info missing");
            for (int i = 0; i < expectedPodInfo.length; i++) {
                Assert.assertEquals(actualPodInfo[i], expectedPodInfo[i], "pod info does not match in pod " +
                        expectedEntry.getKey());
            }
        }
    }

    /**
     * Assert that two string sets are equal.
     *
     * @param actual   The actual string set
     * @param expected The expected string set
     */
    private void assertEquals(Set<String> actual, Set<String> expected) {
        Assert.assertEquals(actual.size(), expected.size());
        for (String expectedValue : expected) {
            Assert.assertTrue(actual.contains(expectedValue));
        }
    }
}
