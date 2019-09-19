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

import io.cellery.observability.k8s.client.crds.cell.Cell;
import io.cellery.observability.k8s.client.crds.composite.Composite;
import io.cellery.observability.k8s.client.crds.gateway.Destination;
import io.cellery.observability.k8s.client.crds.gateway.GRPC;
import io.cellery.observability.k8s.client.crds.gateway.HTTP;
import io.cellery.observability.k8s.client.crds.gateway.TCP;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.commons.lang3.StringUtils;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test case for Component Event Source.
 */
public class ComponentsEventSourceTestCase extends BaseSiddhiExtensionTestCase {

    private static final Logger logger = Logger.getLogger(ComponentsEventSourceTestCase.class.getName());

    private AtomicInteger eventCount = new AtomicInteger(0);
    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> receivedEvents;

    private static final String WATCH_CELL_URL = "/apis/" + Constants.CELLERY_CRD_GROUP + "/"
            + Constants.CELL_CRD_VERSION + "/cells?watch=true";
    private static final String WATCH_COMPOSITE_URL = "/apis/" + Constants.CELLERY_CRD_GROUP + "/"
            + Constants.COMPOSITE_CRD_VERSION + "/composites?watch=true";

    public ComponentsEventSourceTestCase() throws Exception {
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
    public void testComponentEvents() throws Exception {
        String deletionTimestamp = "2019-06-30T01:45:11Z";

        Destination cellAHttpIngressADestination = new Destination();
        cellAHttpIngressADestination.setHost("portal");
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setDestination(cellAHttpIngressADestination);

        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate("pet-store.com", Collections.singletonList(cellAHttpIngressA),
                        null, null),
                Collections.singletonList(generateServicesTemplate("portal", "HTTP")));
        cellA.getMetadata().setCreationTimestamp(null);

        Destination cellBTcpIngressADestination = new Destination();
        cellBTcpIngressADestination.setHost("test-a");
        TCP cellBTcpIngressA = new TCP();
        cellBTcpIngressA.setDestination(cellBTcpIngressADestination);

        Cell cellB = generateCell("cell-b",
                generateGatewayTemplate(null, null, Collections.singletonList(cellBTcpIngressA), null),
                Arrays.asList(generateServicesTemplate("test-a", "TCP"),
                        generateServicesTemplate("test-b", "TCP")));

        Destination cellCHttpIngressADestination = new Destination();
        cellCHttpIngressADestination.setHost("controller");
        HTTP cellCHttpIngressA = new HTTP();
        cellCHttpIngressA.setDestination(cellCHttpIngressADestination);

        Destination cellCHttpIngressBDestination = new Destination();
        cellCHttpIngressBDestination.setHost("customers");
        HTTP cellCHttpIngressB = new HTTP();
        cellCHttpIngressB.setDestination(cellCHttpIngressBDestination);

        Cell cellC = generateCell("cell-c",
                generateGatewayTemplate(null, Arrays.asList(cellCHttpIngressA, cellCHttpIngressB),
                        null, null),
                Arrays.asList(generateServicesTemplate("controller", "HTTP"),
                        generateServicesTemplate("customers", "HTTP"),
                        generateServicesTemplate("orders", "HTTP"),
                        generateServicesTemplate("catalog", "HTTP")));
        cellC.getMetadata().setDeletionTimestamp(deletionTimestamp);

        Destination cellDGrpcIngressADestination = new Destination();
        cellDGrpcIngressADestination.setHost("test-component");
        GRPC cellDGrpcIngressA = new GRPC();
        cellDGrpcIngressA.setDestination(cellDGrpcIngressADestination);

        Cell cellD = generateCell("cell-d",
                generateGatewayTemplate(null, null, null, Collections.singletonList(cellDGrpcIngressA)),
                Arrays.asList(generateServicesTemplate("test-component", "GRPC"),
                        // Ingress can be lower case as well
                        generateServicesTemplate("test-component-alt", "grpc")));

        Composite compositeA = generateComposite("composite-a",
                Arrays.asList(generateServicesTemplate("test-component-a", "GRPC"),
                        generateServicesTemplate("test-component-alt", "HTTP")));

        Composite compositeB = generateComposite("composite-b",
                Arrays.asList(generateServicesTemplate("test-component-b", "HTTP"),
                        generateServicesTemplate("test-component-alt", null)));

        Composite compositeC = generateComposite("composite-c",
                Arrays.asList(generateServicesTemplate("test-r", "TCP"),
                        generateServicesTemplate("test-s", "GRPC")));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .waitFor(193)
                .andEmit(new WatchEvent(cellB, "MODIFIED"))
                .waitFor(1)
                .andEmit(new WatchEvent(cellC, "DELETED"))
                .waitFor(21)
                .andEmit(new WatchEvent(cellD, "ERROR"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(WATCH_COMPOSITE_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1320)
                .andEmit(new WatchEvent(compositeA, "ADDED"))
                .waitFor(503)
                .andEmit(new WatchEvent(compositeB, "MODIFIED"))
                .waitFor(923)
                .andEmit(new WatchEvent(compositeC, "DELETED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 16, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 15);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 6);
            String[] ingressTypes;
            if (StringUtils.isNotEmpty((String) data[4])) {
                ingressTypes = ((String) data[4]).split(",");
            } else {
                ingressTypes = new String[]{};
            }
            if ("cell-a".equals(data[0]) && "portal".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], -1L);
                Assert.assertEquals(data[5], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.WEB});
            } else if ("cell-b".equals(data[0]) && "test-a".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.TCP});
            } else if ("cell-b".equals(data[0]) && "test-b".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("cell-c".equals(data[0]) && "controller".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("cell-c".equals(data[0]) && "customers".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("cell-c".equals(data[0]) && "orders".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("cell-c".equals(data[0]) && "catalog".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("cell-d".equals(data[0]) && "test-component".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ERROR");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.GRPC});
            } else if ("cell-d".equals(data[0]) && "test-component-alt".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ERROR");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("composite-a".equals(data[0]) && "test-component-a".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.GRPC});
            } else if ("composite-a".equals(data[0]) && "test-component-alt".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("composite-b".equals(data[0]) && "test-component-b".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("composite-b".equals(data[0]) && "test-component-alt".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("composite-c".equals(data[0]) && "test-r".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.TCP});
            } else if ("composite-c".equals(data[0]) && "test-s".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.GRPC});
            } else {
                Assert.fail("Received unexpect component " + data[1] + " from " + data[1] + " " + data[0]);
            }
        }
    }

    @Test
    public void testComponentEventsWithNoIngresses() throws Exception {
        Destination cellAHttpIngressADestination = new Destination();
        cellAHttpIngressADestination.setHost("controller");
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setDestination(cellAHttpIngressADestination);

        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate(null, null, null, null),
                Arrays.asList(generateServicesTemplate("controller", "HTTP"),
                        generateServicesTemplate("customers", "GRPC")));

        Composite compositeA = generateComposite("employee-comp",
                Arrays.asList(generateServicesTemplate("salary-job", null),
                        generateServicesTemplate("employee-job", null)));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(WATCH_COMPOSITE_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(451)
                .andEmit(new WatchEvent(compositeA, "MODIFIED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 5, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 4);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 6);
            String[] ingressTypes;
            if (StringUtils.isNotEmpty((String) data[4])) {
                ingressTypes = ((String) data[4]).split(",");
            } else {
                ingressTypes = new String[]{};
            }
            if ("cell-a".equals(data[0]) && "controller".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("cell-a".equals(data[0]) && "customers".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("employee-comp".equals(data[0]) && "employee-job".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("employee-comp".equals(data[0]) && "salary-job".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else {
                Assert.fail("Received unexpect component " + data[2] + " from " + data[1] + " " + data[0]);
            }
        }
    }

    @Test
    public void testComponentEventsWithNoComponents() throws Exception {
        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .done()
                .once();
        k8sServer.expect()
                .withPath(WATCH_COMPOSITE_URL)
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
        Destination cellAHttpIngressADestination = new Destination();
        cellAHttpIngressADestination.setHost("controller");
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setDestination(cellAHttpIngressADestination);

        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate(null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller", "HTTP"),
                        generateServicesTemplate("customers", "HTTP")));

        Composite compositeA = generateComposite("composite-a",
                Arrays.asList(generateServicesTemplate("employee", "GRPC"),
                        generateServicesTemplate("salary", "HTTP")));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(WATCH_COMPOSITE_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(723)
                .andEmit(new WatchEvent(compositeA, "MODIFIED"))
                .done()
                .once();

        k8sClient.getConfiguration().setMasterUrl("https://localhost");
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testPersistence() throws Exception {
        Destination cellAHttpIngressADestination = new Destination();
        cellAHttpIngressADestination.setHost("controller");
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setDestination(cellAHttpIngressADestination);

        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate(null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller", "HTTP"),
                        generateServicesTemplate("customers", "HTTP")));

        Composite compositeA = generateComposite("composite-a",
                Arrays.asList(generateServicesTemplate("hr", "HTTP"),
                        generateServicesTemplate("stock", "TCP")));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(WATCH_COMPOSITE_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(412)
                .andEmit(new WatchEvent(compositeA, "DELETED"))
                .done()
                .once();

        initializeSiddhiAppRuntime();
        Thread.sleep(1200);
        siddhiAppRuntime.persist();
        Thread.sleep(100);
        siddhiAppRuntime.restoreLastRevision();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 5, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 4);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 6);
            String[] ingressTypes;
            if (StringUtils.isNotEmpty((String) data[4])) {
                ingressTypes = ((String) data[4]).split(",");
            } else {
                ingressTypes = new String[]{};
            }
            if ("cell-a".equals(data[0]) && "controller".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("cell-a".equals(data[0]) && "customers".equals(data[2])) {
                Assert.assertEquals(data[1], "Cell");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("composite-a".equals(data[0]) && "hr".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("composite-a".equals(data[0]) && "stock".equals(data[2])) {
                Assert.assertEquals(data[1], "Composite");
                Assert.assertEquals(data[3], creationTimestamp);
                Assert.assertEquals(data[5], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.TCP});
            } else {
                Assert.fail("Received unexpect component " + data[1] + " from cell " + data[0]);
            }
        }
    }

    @Test
    public void testTimestampParseFailure() throws Exception {
        Destination cellAHttpIngressADestination = new Destination();
        cellAHttpIngressADestination.setHost("controller");
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setDestination(cellAHttpIngressADestination);

        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate(null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller", "HTTP"),
                        generateServicesTemplate("customers", "HTTP")));
        cellA.getMetadata().setCreationTimestamp("invalid-date-1");

        Composite compositeA = generateComposite("composite-a",
                Arrays.asList(generateServicesTemplate("tester", "GRPC"),
                        generateServicesTemplate("test-store", "TCP")));
        compositeA.getMetadata().setCreationTimestamp("invalid-date-2");

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(WATCH_COMPOSITE_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1212)
                .andEmit(new WatchEvent(compositeA, "MODIFIED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testShutdownWithFailure() throws Exception {
        Destination cellAHttpIngressADestination = new Destination();
        cellAHttpIngressADestination.setHost("controller");
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setDestination(cellAHttpIngressADestination);

        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate(null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller", "HTTP"),
                        generateServicesTemplate("customers", "HTTP")));

        Composite compositeA = generateComposite("composite-a",
                Arrays.asList(generateServicesTemplate("test-component", "TCP"),
                        generateServicesTemplate("test-component-alt", null)));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        k8sServer.expect()
                .withPath(WATCH_COMPOSITE_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(971)
                .andEmit(new WatchEvent(compositeA, "DELETED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        for (List<Source> siddhiAppSources : siddhiAppRuntime.getSources()) {
            for (Source source : siddhiAppSources) {
                if (source instanceof ComponentsEventSource) {
                    ComponentsEventSource componentsEventSource = (ComponentsEventSource) source;
                    List<Watch> k8sWatches = Whitebox.getInternalState(componentsEventSource, "k8sWatches");

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
                "@source(type=\"k8s-components\", @map(type=\"keyvalue\", " +
                "fail.on.missing.attribute=\"true\"))\n" +
                "define stream K8sComponentPodsStream (instance string, instanceKind string, component string, " +
                "creationTimestamp long, ingressTypes string, action string);";
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

}
