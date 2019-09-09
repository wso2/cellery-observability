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

import io.cellery.observability.k8s.client.crds.Cell;
import io.cellery.observability.k8s.client.crds.CellImpl;
import io.cellery.observability.k8s.client.crds.CellSpec;
import io.cellery.observability.k8s.client.crds.model.GRPC;
import io.cellery.observability.k8s.client.crds.model.GatewayTemplate;
import io.cellery.observability.k8s.client.crds.model.GatewayTemplateSpec;
import io.cellery.observability.k8s.client.crds.model.HTTP;
import io.cellery.observability.k8s.client.crds.model.STSTemplate;
import io.cellery.observability.k8s.client.crds.model.STSTemplateSpec;
import io.cellery.observability.k8s.client.crds.model.ServicesTemplate;
import io.cellery.observability.k8s.client.crds.model.ServicesTemplateSpec;
import io.cellery.observability.k8s.client.crds.model.TCP;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
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
public class ComponentsEventSourceTestCase extends BaseTestCase {

    private static final Logger logger = Logger.getLogger(ComponentsEventSourceTestCase.class.getName());

    private AtomicInteger eventCount = new AtomicInteger(0);
    private SiddhiAppRuntime siddhiAppRuntime;
    private List<Event> receivedEvents;

    private static final String WATCH_CELL_URL = "/apis/" + Constants.CELL_CRD_GROUP + "/"
            + Constants.CELL_CRD_VERSION + "/cells?watch=true";

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
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setGlobal(true);
        cellAHttpIngressA.setContext("/");
        cellAHttpIngressA.setBackend("portal");
        cellAHttpIngressA.setAuthenticate(false);
        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate("Envoy", "pet-store.com", Collections.singletonList(cellAHttpIngressA),
                        null, null),
                Collections.singletonList(generateServicesTemplate("portal")));
        cellA.getMetadata().setCreationTimestamp(null);

        TCP cellBTcpIngressA = new TCP();
        cellBTcpIngressA.setBackendHost("test-a");
        cellBTcpIngressA.setBackendPort(8000);
        cellBTcpIngressA.setPort(7000);
        Cell cellB = generateCell("cell-b",
                generateGatewayTemplate("Envoy", null, null, Collections.singletonList(cellBTcpIngressA), null),
                Arrays.asList(generateServicesTemplate("test-a"), generateServicesTemplate("test-b")));

        HTTP cellCHttpIngressA = new HTTP();
        cellCHttpIngressA.setAuthenticate(true);
        cellCHttpIngressA.setBackend("controller");
        cellCHttpIngressA.setContext("/controller");
        cellCHttpIngressA.setGlobal(true);
        HTTP cellCHttpIngressB = new HTTP();
        cellCHttpIngressB.setAuthenticate(true);
        cellCHttpIngressB.setBackend("customers");
        cellCHttpIngressB.setContext("/customers");
        cellCHttpIngressB.setGlobal(false);
        Cell cellC = generateCell("cell-c",
                generateGatewayTemplate("MicroGateway", null, Arrays.asList(cellCHttpIngressA, cellCHttpIngressB),
                        null, null),
                Arrays.asList(generateServicesTemplate("controller"), generateServicesTemplate("customers"),
                        generateServicesTemplate("orders"), generateServicesTemplate("catalog")));
        cellC.getMetadata().setDeletionTimestamp(deletionTimestamp);

        GRPC cellDGrpcIngressA = new GRPC();
        cellDGrpcIngressA.setPort(10000);
        cellDGrpcIngressA.setBackendPort(9000);
        cellDGrpcIngressA.setBackendHost("test-component");
        Cell cellD = generateCell("cell-d",
                generateGatewayTemplate("Envoy", null, null, null, Collections.singletonList(cellDGrpcIngressA)),
                Arrays.asList(generateServicesTemplate("test-component"),
                        generateServicesTemplate("test-component-alt")));

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
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 9, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 9);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 5);
            String[] ingressTypes;
            if (StringUtils.isNotEmpty((String) data[3])) {
                ingressTypes = ((String) data[3]).split(",");
            } else {
                ingressTypes = new String[]{};
            }
            if ("cell-a".equals(data[0]) && "portal".equals(data[1])) {
                Assert.assertEquals(data[2], -1L);
                Assert.assertEquals(data[4], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.WEB});
            } else if ("cell-b".equals(data[0]) && "test-a".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.TCP});
            } else if ("cell-b".equals(data[0]) && "test-b".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "MODIFIED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("cell-c".equals(data[0]) && "controller".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("cell-c".equals(data[0]) && "customers".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("cell-c".equals(data[0]) && "orders".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("cell-c".equals(data[0]) && "catalog".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "DELETED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else if ("cell-d".equals(data[0]) && "test-component".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "ERROR");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.GRPC});
            } else if ("cell-d".equals(data[0]) && "test-component-alt".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "ERROR");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else {
                Assert.fail("Received unexpect component " + data[1] + " from cell " + data[0]);
            }
        }
    }

    @Test
    public void testComponentEventsWithNoIngresses() throws Exception {
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setAuthenticate(true);
        cellAHttpIngressA.setBackend("controller");
        cellAHttpIngressA.setContext("/controller");
        cellAHttpIngressA.setGlobal(true);
        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate("MicroGateway", null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller"), generateServicesTemplate("customers")));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1135)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 3, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 5);
            String[] ingressTypes;
            if (StringUtils.isNotEmpty((String) data[3])) {
                ingressTypes = ((String) data[3]).split(",");
            } else {
                ingressTypes = new String[]{};
            }
            if ("cell-a".equals(data[0]) && "controller".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("cell-a".equals(data[0]) && "customers".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else {
                Assert.fail("Received unexpect component " + data[1] + " from cell " + data[0]);
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
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testPodEventsWithApiServerDown() throws Exception {
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setAuthenticate(true);
        cellAHttpIngressA.setBackend("controller");
        cellAHttpIngressA.setContext("/controller");
        cellAHttpIngressA.setGlobal(true);
        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate("MicroGateway", null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller"), generateServicesTemplate("customers")));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();

        k8sClient.getConfiguration().setMasterUrl("https://localhost");
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testPersistence() throws Exception {
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setAuthenticate(true);
        cellAHttpIngressA.setBackend("controller");
        cellAHttpIngressA.setContext("/controller");
        cellAHttpIngressA.setGlobal(true);
        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate("MicroGateway", null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller"), generateServicesTemplate("customers")));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();
        Thread.sleep(1200);
        siddhiAppRuntime.persist();
        Thread.sleep(100);
        siddhiAppRuntime.restoreLastRevision();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 2);
        for (Event receivedEvent : receivedEvents) {
            Object[] data = receivedEvent.getData();
            Assert.assertEquals(data.length, 5);
            String[] ingressTypes;
            if (StringUtils.isNotEmpty((String) data[3])) {
                ingressTypes = ((String) data[3]).split(",");
            } else {
                ingressTypes = new String[]{};
            }
            if ("cell-a".equals(data[0]) && "controller".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{Constants.IngressType.HTTP});
            } else if ("cell-a".equals(data[0]) && "customers".equals(data[1])) {
                Assert.assertEquals(data[2], creationTimestamp);
                Assert.assertEquals(data[4], "ADDED");
                Assert.assertEqualsNoOrder(ingressTypes, new String[]{});
            } else {
                Assert.fail("Received unexpect component " + data[1] + " from cell " + data[0]);
            }
        }
    }

    @Test
    public void testTimestampParseFailure() throws Exception {
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setAuthenticate(true);
        cellAHttpIngressA.setBackend("controller");
        cellAHttpIngressA.setContext("/controller");
        cellAHttpIngressA.setGlobal(true);
        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate("MicroGateway", null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller"), generateServicesTemplate("customers")));
        cellA.getMetadata().setCreationTimestamp("invalid-date-1");

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 1, eventCount, TIMEOUT);
        Assert.assertEquals(eventCount.get(), 0);
    }

    @Test
    public void testShutdownWithFailure() throws Exception {
        HTTP cellAHttpIngressA = new HTTP();
        cellAHttpIngressA.setAuthenticate(true);
        cellAHttpIngressA.setBackend("controller");
        cellAHttpIngressA.setContext("/controller");
        cellAHttpIngressA.setGlobal(true);
        Cell cellA = generateCell("cell-a",
                generateGatewayTemplate("MicroGateway", null, Collections.singletonList(cellAHttpIngressA), null, null),
                Arrays.asList(generateServicesTemplate("controller"), generateServicesTemplate("customers")));

        k8sServer.expect()
                .withPath(WATCH_CELL_URL)
                .andUpgradeToWebSocket()
                .open()
                .waitFor(1195)
                .andEmit(new WatchEvent(cellA, "ADDED"))
                .done()
                .once();
        initializeSiddhiAppRuntime();

        SiddhiTestHelper.waitForEvents(WAIT_TIME, 2, eventCount, TIMEOUT);
        for (List<Source> siddhiAppSources : siddhiAppRuntime.getSources()) {
            for (Source source : siddhiAppSources) {
                if (source instanceof ComponentsEventSource) {
                    ComponentsEventSource componentsEventSource = (ComponentsEventSource) source;
                    Watch cellWatch = Whitebox.getInternalState(componentsEventSource, "cellWatch");
                    Watcher<Pod> podWatcher = Whitebox.getInternalState(cellWatch, "watcher");
                    podWatcher.onClose(new KubernetesClientException("Mock Exception"));
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
                "define stream k8sComponentPodsStream (cell String, component string, creationTimestamp long, " +
                "ingressTypes string, action string);";
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
     * Generate a K8s Cell object.
     * The returned pod can be used as one of the returned pods in K8s Mock Server in expectation mode.
     *
     * @param cellName The name of the Cell
     * @param gatewayTemplate The gateway template to be used
     * @param servicesTemplates The list of service templates
     * @return The generated Cell
     */
    protected Cell generateCell(String cellName, GatewayTemplate gatewayTemplate,
                                List<ServicesTemplate> servicesTemplates) {
        STSTemplate stsTemplate = new STSTemplate();
        stsTemplate.setMetadata(new ObjectMetaBuilder()
                .withName("sts")
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .build());
        stsTemplate.setSpec(new STSTemplateSpec());

        CellSpec cellSpec = new CellSpec();
        cellSpec.setGatewayTemplate(gatewayTemplate);
        cellSpec.setServicesTemplates(servicesTemplates);
        cellSpec.setStsTemplate(stsTemplate);

        CellImpl cell = new CellImpl();
        cell.setMetadata(new ObjectMetaBuilder()
                .withName(cellName)
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .build());
        cell.setSpec(cellSpec);
        return cell;
    }

    /**
     * Generate a K8s Gateway Template Object.
     *
     * @param type The type of Gateway used
     * @param host The host added when used with a Web Ingress
     * @param httpIngresses The HTTP ingresses used by the gateway
     * @param tcpIngresses The TCP ingresses used by the gateway
     * @param grpcIngresses The gRPC ingresses
     * @return The generated Gateway Template
     */
    protected GatewayTemplate generateGatewayTemplate(String type, String host, List<HTTP> httpIngresses,
                                                      List<TCP> tcpIngresses, List<GRPC> grpcIngresses) {
        GatewayTemplateSpec gatewayTemplateSpec = new GatewayTemplateSpec();
        gatewayTemplateSpec.setType(type);
        gatewayTemplateSpec.setHost(host);
        gatewayTemplateSpec.setTcp(tcpIngresses);
        gatewayTemplateSpec.setHttp(httpIngresses);
        gatewayTemplateSpec.setGrpc(grpcIngresses);

        GatewayTemplate gatewayTemplate = new GatewayTemplate();
        gatewayTemplate.setMetadata(new ObjectMetaBuilder()
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .withName("gateway")
                .build());
        gatewayTemplate.setSpec(gatewayTemplateSpec);
        return gatewayTemplate;
    }

    /**
     * Generate a K8s Service Template Object.
     *
     * @param serviceName The name of the service
     * @return The generated Service Template
     */
    protected ServicesTemplate generateServicesTemplate(String serviceName) {
        ServicesTemplateSpec servicesTemplateSpec = new ServicesTemplateSpec();
        servicesTemplateSpec.setContainer(new ContainerBuilder()
                .withName("test-container")
                .withNewImage("busybox")
                .withNewImagePullPolicy("IfNotPresent")
                .withCommand("tail", "-f", "/dev/null")
                .build());
        servicesTemplateSpec.setReplicas(10);
        servicesTemplateSpec.setServiceAccountName("cellery-service-account");
        servicesTemplateSpec.setServicePort(9000);

        ServicesTemplate servicesTemplate = new ServicesTemplate();
        servicesTemplate.setMetadata(new ObjectMetaBuilder()
                .withName(serviceName)
                .withCreationTimestamp(CREATION_TIMESTAMP_STRING)
                .build());
        servicesTemplate.setSpec(servicesTemplateSpec);
        return servicesTemplate;
    }

}
