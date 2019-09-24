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
package io.cellery.observability.model.generator;

import io.cellery.observability.model.generator.exception.ModelException;
import io.cellery.observability.model.generator.internal.ModelStoreManager;
import io.cellery.observability.model.generator.internal.ServiceHolder;
import io.cellery.observability.model.generator.model.Edge;
import io.cellery.observability.model.generator.model.Model;
import org.apache.commons.io.IOUtils;
import org.powermock.api.mockito.PowerMockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.config.ConfigProviderFactory;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.datasource.core.DataSourceManager;
import org.wso2.carbon.datasource.core.exception.DataSourceException;
import org.wso2.carbon.datasource.core.impl.DataSourceServiceImpl;
import org.wso2.carbon.secvault.SecureVault;
import org.wso2.extension.siddhi.script.js.EvalJavaScript;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is the unit test case for the model generation usecases
 */
public class ModelGenTestCase {
    private static final String CARBON_HOME_ENV = "carbon.home";
    private static final String WSO2_RUNTIME_ENV = "wso2.runtime";
    private static final String GATEWAY_COMPONENT = "gateway";

    private SiddhiAppRuntime siddhiAppRuntime;
    private InputHandler inputHandler;

    private int publishedCount = 0;
    private int receivedCount = 0;

    private Set<Node> nodes = new HashSet<>();
    private Set<Edge> edges = new HashSet<>();

    private final String hrInstance = "hr-1-0-0-e1991fe2";
    private final String stockInstance = "stock-1-0-0-ae4d1965";
    private final String employeeInstance = "employee-1-0-0-6cc39e16";
    private final String hr = "hr";
    private final String employee = "employee";
    private final String salary = "salary";
    private final String stock = "stock";


    @BeforeClass
    public void init() throws IOException, ModelException, DataSourceException, ConfigurationException {
        setEnv();
        String tracingAppContent = IOUtils.toString(this.getClass().
                getResourceAsStream(File.separator + "tracing-app.siddhi"), StandardCharsets.UTF_8.name());
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("observe:traceGroupWindow", TraceGroupWindowProcessor.class);
        siddhiManager.setExtension("observe:modelGenerator", ModelGenerationExtension.class);
        siddhiManager.setExtension("script:javascript", EvalJavaScript.class);
        siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(tracingAppContent);
        siddhiAppRuntime.addCallback("ProcessedZipkinStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                publishedCount++;
            }
        });
        siddhiAppRuntime.addCallback("AfterModelGenStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                receivedCount++;
            }
        });
        inputHandler = siddhiAppRuntime.getInputHandler("ZipkinStreamIn");
        siddhiAppRuntime.start();
        initDatasource();
    }

    private void setEnv() {
        String carbonHomePath = this.getClass().getResource("/").getFile();
        System.setProperty(CARBON_HOME_ENV, carbonHomePath);
        System.setProperty(WSO2_RUNTIME_ENV, "worker");
    }

    private void initDatasource() throws ConfigurationException, DataSourceException, ModelException {
        DataSourceServiceImpl dataSourceService = new DataSourceServiceImpl();
        SecureVault secureVault = PowerMockito.mock(SecureVault.class);
        ConfigProvider configProvider = ConfigProviderFactory.getConfigProvider(Paths.get(
                System.getProperty(CARBON_HOME_ENV), "conf" + File.separator + "deployment.yaml"), secureVault);
        DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        dataSourceManager.initDataSources(configProvider);
        ServiceHolder.setDataSourceService(dataSourceService);
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());
    }

    @Test(groups = "scenarios")
    public void testHelloWorldWebappWithInstanceName() throws Exception {
        long fromTime = System.currentTimeMillis();
        generateModel("hello-world-webapp-name.csv");
        String cellInstanceName = "hello-world-cell";
        this.nodes.add(new Node(cellInstanceName));

        Model model = ServiceHolder.getModelManager().getGraph(0, 0);
        validateHelloWorldWeb(cellInstanceName, model, nodes, edges);

        model = ServiceHolder.getModelManager().getDependencyModel(fromTime, System.currentTimeMillis(),
                cellInstanceName);
        Set<Node> timeSlicedNodes = new HashSet<>();
        timeSlicedNodes.add(new Node(cellInstanceName));
        validateHelloWorldWeb(cellInstanceName, model, timeSlicedNodes, new HashSet<>());
        resetCount();
    }

    @Test(groups = "scenarios")
    public void testHelloWorldWebappWithUUID() throws Exception {
        generateModel("hello-world-webapp-uuid.csv");
        String cellInstanceName = "hello-world-cell-0-2-0-8d31dd02";
        this.nodes.add(new Node(cellInstanceName));

        Model model = ServiceHolder.getModelManager().getGraph(0, 0);
        validateHelloWorldWeb(cellInstanceName, model, nodes, edges);
        resetCount();
    }

    @Test(groups = "scenarios")
    public void testEmployeePortal() throws Exception {
        generateModel("employee-portal.csv");
        HashSet<Node> nodes = new HashSet<>();
        HashSet<Edge> edges = new HashSet<>();
        nodes.add(new Node(hrInstance));
        nodes.add(new Node(stockInstance));
        nodes.add(new Node(employeeInstance));
        edges.add(new Edge(Utils.generateEdgeName(hrInstance, employeeInstance,
                Utils.generateServiceName("hr", GATEWAY_COMPONENT))));
        edges.add(new Edge(Utils.generateEdgeName(hrInstance, stockInstance,
                Utils.generateServiceName("hr", GATEWAY_COMPONENT))));
        this.nodes.addAll(nodes);
        this.edges.addAll(edges);

        Model model = ServiceHolder.getModelManager().getGraph(0, 0);
        validateModel(model, this.nodes, this.edges);
        validateEmployee(model, hrInstance, stockInstance, employeeInstance);
        resetCount();
    }

    @Test(groups = "scenarios")
    public void testPetstore() throws Exception {
        generateModel("pet-store-name.csv");
        String petBe = "pet-be";
        String petFe = "pet-fe";
        HashSet<Node> nodes = new HashSet<>();
        HashSet<Edge> edges = new HashSet<>();
        nodes.add(new Node(petBe));
        nodes.add(new Node(petFe));
        edges.add(new Edge(Utils.generateEdgeName(petFe, petBe,
                Utils.generateServiceName("portal", GATEWAY_COMPONENT))));
        edges.add(new Edge(Utils.generateEdgeName(petFe, petBe,
                Utils.generateServiceName(GATEWAY_COMPONENT, GATEWAY_COMPONENT))));
        this.nodes.addAll(nodes);
        this.edges.addAll(edges);

        Model model = ServiceHolder.getModelManager().getGraph(0, 0);
        validateModel(model, this.nodes, this.edges);
        validatePetstore(model, petFe, petBe);
        resetCount();
    }

    @Test(groups = "scenarios")
    public void testHelloworldAPI() throws Exception {
        generateModel("hello-world-api.csv");
        String helloApiInstance = "hello-world-api-cell-0-2-0-614412b8";
        HashSet<Node> nodes = new HashSet<>();
        nodes.add(new Node(helloApiInstance));
        this.nodes.addAll(nodes);

        Model model = ServiceHolder.getModelManager().getGraph(0, 0);
        validateModel(model, this.nodes, this.edges);

        String helloApi = "hello-api";
        List<String> components = new ArrayList<>();
        components.add(helloApi);
        components.add(GATEWAY_COMPONENT);
        List<String> edgeComponents = new ArrayList<>();
        edgeComponents.add(Utils.generateServiceName(GATEWAY_COMPONENT, helloApi));
        validateNode(model, helloApiInstance, components, edgeComponents);
        resetCount();
    }

    @Test(dependsOnGroups = "scenarios")
    public void reloadModelManager() throws Exception {
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());
        Model model = ServiceHolder.getModelManager().getGraph(0, 0);
        validateModel(model, this.nodes, this.edges);
    }

    @Test(dependsOnGroups = "scenarios")
    public void loadModelWithInvalidTimeRange() throws Exception {
        Model model = ServiceHolder.getModelManager().getGraph(System.currentTimeMillis(),
                System.currentTimeMillis() + 10000);
        validateModel(model, this.nodes, this.edges);
    }

    @Test(dependsOnGroups = "scenarios")
    public void loadServiceDependencyDiagram() throws Exception {
        Set<Node> nodes = new HashSet<>();
        nodes.add(new Node(Utils.getQualifiedServiceName(hrInstance, hr)));
        nodes.add(new Node(Utils.getQualifiedServiceName(employeeInstance, employee)));
        nodes.add(new Node(Utils.getQualifiedServiceName(employeeInstance, salary)));
        nodes.add(new Node(Utils.getQualifiedServiceName(employeeInstance, GATEWAY_COMPONENT)));
        nodes.add(new Node(Utils.getQualifiedServiceName(stockInstance, stock)));
        nodes.add(new Node(Utils.getQualifiedServiceName(stockInstance, GATEWAY_COMPONENT)));

        Set<Edge> edges = new HashSet<>();
        edges.add(new Edge(Utils.generateEdgeName(Utils.getQualifiedServiceName(stockInstance, GATEWAY_COMPONENT),
                Utils.getQualifiedServiceName(stockInstance, stock), "")));
        edges.add(new Edge(Utils.generateEdgeName(Utils.getQualifiedServiceName(hrInstance, hr),
                Utils.getQualifiedServiceName(stockInstance, GATEWAY_COMPONENT), "")));
        edges.add(new Edge(Utils.generateEdgeName(Utils.getQualifiedServiceName(hrInstance, hr),
                Utils.getQualifiedServiceName(employeeInstance, GATEWAY_COMPONENT), "")));
        edges.add(new Edge(Utils.generateEdgeName(Utils.getQualifiedServiceName(employeeInstance, employee),
                Utils.getQualifiedServiceName(employeeInstance, salary), "")));
        edges.add(new Edge(Utils.generateEdgeName(Utils.getQualifiedServiceName(employeeInstance, GATEWAY_COMPONENT),
                Utils.getQualifiedServiceName(employeeInstance, employee), "")));

        Model model = ServiceHolder.getModelManager().getDependencyModel(System.currentTimeMillis(),
                System.currentTimeMillis() + 10000, hrInstance, hr);
        validateModel(model, nodes, edges);
    }

    private void validateHelloWorldWeb(String cellInstanceName, Model model, Set<Node> nodes, Set<Edge> nodeEdges) {
        String hello = "hello";
        validateModel(model, nodes, nodeEdges);

        List<String> components = new ArrayList<>();
        components.add(GATEWAY_COMPONENT);
        components.add(hello);
        List<String> edges = new ArrayList<>();
        edges.add(Utils.generateServiceName(GATEWAY_COMPONENT, hello));
        validateNode(model, cellInstanceName, components, edges);
    }

    private void validateEmployee(Model model, String hrInstance, String stockInstance, String employeeInstance) {
        List<String> components = new ArrayList<>();
        components.add(GATEWAY_COMPONENT);
        components.add(hr);
        List<String> componentEdges = new ArrayList<>();
        componentEdges.add(Utils.generateServiceName(GATEWAY_COMPONENT, hr));
        validateNode(model, hrInstance, components, componentEdges);

        components.clear();
        components.add(GATEWAY_COMPONENT);
        components.add(stock);
        componentEdges.clear();
        componentEdges.add(Utils.generateServiceName(GATEWAY_COMPONENT, stock));
        validateNode(model, stockInstance, components, componentEdges);

        components.clear();
        components.add(GATEWAY_COMPONENT);
        components.add(employee);
        components.add(salary);
        componentEdges.clear();
        componentEdges.add(Utils.generateServiceName(GATEWAY_COMPONENT, employee));
        componentEdges.add(Utils.generateServiceName(employee, salary));
        validateNode(model, employeeInstance, components, componentEdges);
    }

    private void validatePetstore(Model model, String petFe, String petBe) {
        String controller = "controller";
        String catalog = "catalog";
        String orders = "orders";
        String customers = "customers";
        List<String> components = new ArrayList<>();
        components.add(GATEWAY_COMPONENT);
        components.add(controller);
        components.add(catalog);
        components.add(orders);
        components.add(customers);
        List<String> componentEdges = new ArrayList<>();
        componentEdges.add(Utils.generateServiceName(GATEWAY_COMPONENT, controller));
        componentEdges.add(Utils.generateServiceName(controller, orders));
        componentEdges.add(Utils.generateServiceName(controller, catalog));
        componentEdges.add(Utils.generateServiceName(controller, customers));
        validateNode(model, petBe, components, componentEdges);

        String portal = "portal";
        components.clear();
        components.add(GATEWAY_COMPONENT);
        components.add(portal);
        componentEdges.clear();
        componentEdges.add(Utils.generateServiceName(GATEWAY_COMPONENT, portal));
        validateNode(model, petFe, components, componentEdges);
    }

    private void validateModel(Model model, Set<Node> nodes, Set<Edge> edges) {
        Assert.assertEquals(model.getNodes().size(), nodes.size());
        Assert.assertEquals(model.getEdges().size(), edges.size());
        Assert.assertTrue(model.getNodes().containsAll(nodes));
        Assert.assertTrue(model.getEdges().containsAll(edges));
    }

    private void validateNode(Model model, String nodeId, List<String> components, List<String> componentEdges) {
        Iterator<Node> nodeIterator = model.getNodes().iterator();
        Node node = null;
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            if (node.getId().equals(nodeId)) {
                break;
            }
        }
        Assert.assertTrue(node != null);
        Assert.assertTrue(node.getId().equals(nodeId));
        Assert.assertEquals(node.getComponents().size(), components.size());
        Assert.assertTrue(node.getComponents().containsAll(components));
        Assert.assertEquals(node.getEdges().size(), componentEdges.size());
        Assert.assertTrue(node.getEdges().containsAll(componentEdges));
    }

    private void generateModel(String fileName) throws IOException, InterruptedException {
        publishData(fileName);
        waitForModelGen();
        Assert.assertTrue(publishedCount == receivedCount);
    }


    private void resetCount() {
        publishedCount = 0;
        receivedCount = 0;
    }

    private void waitForModelGen() {
        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
        }
    }

    private void publishData(String fileName) throws IOException, InterruptedException {
        String inputData = IOUtils.toString(
                this.getClass().getResourceAsStream(File.separator + "events" + File.separator + fileName),
                "UTF-8");
        String[] newLines = inputData.split("\n");
        for (String newLine : newLines) {
            if (!newLine.isEmpty()) {
                String[] objectData = newLine.split(",");
                Object[] eventData = new Object[objectData.length];
                for (int i = 0; i < objectData.length; i++) {
                    if (i == 6) {
                        eventData[i] = Long.parseLong(objectData[i]);
                    } else if ("null".equals(objectData[i])) {
                        eventData[i] = null;
                    } else {
                        eventData[i] = objectData[i];
                    }

                }
                inputHandler.send(eventData);
            }
        }
    }

    @AfterClass
    public void cleanup() {
        siddhiAppRuntime.shutdown();
        System.setProperty("carbon.home", "");
        System.setProperty("wso2.runtime", "");
    }
}
