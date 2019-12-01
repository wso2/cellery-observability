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

import io.cellery.observability.model.generator.datasource.FailureDatasource;
import io.cellery.observability.model.generator.datasource.FailureDatasourceServiceImpl;
import io.cellery.observability.model.generator.exception.GraphStoreException;
import io.cellery.observability.model.generator.exception.ModelException;
import io.cellery.observability.model.generator.internal.ModelStoreManager;
import io.cellery.observability.model.generator.internal.ServiceHolder;
import io.cellery.observability.model.generator.model.Model;
import io.cellery.observability.model.generator.model.ModelManager;
import io.cellery.observability.model.generator.model.Node;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import org.wso2.carbon.config.ConfigProviderFactory;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.datasource.core.DataSourceManager;
import org.wso2.carbon.datasource.core.impl.DataSourceServiceImpl;
import org.wso2.carbon.secvault.SecureVault;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * This test case simulates database connection, and SQL failures.
 */
public class DatabaseFailureTestCase {
    private static final String CARBON_HOME_ENV = "carbon.home";
    private static final String WSO2_RUNTIME_ENV = "wso2.runtime";

    @BeforeClass
    public void initTestCase() {
        String carbonHomePath = this.getClass().getResource("/").getFile();
        System.setProperty(CARBON_HOME_ENV, carbonHomePath);
        System.setProperty(WSO2_RUNTIME_ENV, "worker");
    }

    @AfterClass
    public void cleanUpTestCase() {
        System.setProperty(CARBON_HOME_ENV, "");
        System.setProperty(WSO2_RUNTIME_ENV, "");
    }

    @BeforeGroups(groups = "connection-failure")
    public void initTempConnFailureGroups() throws Exception {
        // Initialize data source service
        DataSourceServiceImpl dataSourceService = new DataSourceServiceImpl();
        SecureVault secureVault = PowerMockito.mock(SecureVault.class);
        ConfigProvider configProvider = ConfigProviderFactory.getConfigProvider(
                Paths.get(System.getProperty("carbon.home"), "conf" + File.separator + "deployment.yaml"),
                secureVault);
        DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        dataSourceManager.initDataSources(configProvider);
        ServiceHolder.setDataSourceService(dataSourceService);

        ModelStoreManager modelStoreManager = Mockito.spy(new ModelStoreManager());
        Whitebox.setInternalState(modelStoreManager, "dataSource", new FailureDatasource(true));
        ServiceHolder.setModelStoreManager(modelStoreManager);

        Mockito.doReturn(new Model(Collections.emptySet(), Collections.emptySet())).when(modelStoreManager)
                .loadLastModel();
        ServiceHolder.setModelManager(new ModelManager());
        Mockito.doCallRealMethod().when(modelStoreManager).loadLastModel();

        Node nodeA = new Node("default", "instance-a", "gateway");
        ServiceHolder.getModelManager().addNode(nodeA);
        Node nodeB = new Node("default", "instance-a", "component-a");
        ServiceHolder.getModelManager().addNode(nodeB);
        Node nodeC = new Node("default", "instance-a", "component-b");
        ServiceHolder.getModelManager().addNode(nodeC);
        ServiceHolder.getModelManager().addEdge(nodeA, nodeB);
        ServiceHolder.getModelManager().addEdge(nodeA, nodeC);
    }

    @AfterGroups(groups = "connection-failure")
    public void cleanUpTempConnFailureGroups() {
        ServiceHolder.setDataSourceService(null);
        ServiceHolder.setModelStoreManager(null);
        ServiceHolder.setModelManager(null);
    }

    @Test(groups = "connection-failure", expectedExceptions = GraphStoreException.class)
    public void testLoadModelWithFailingDatasource() throws GraphStoreException {
        ServiceHolder.getModelManager().getDependencyModel(0, System.currentTimeMillis());
    }

    @Test(groups = "connection-failure", expectedExceptions = GraphStoreException.class)
    public void testPersistsModelWithFailingDatasource() throws GraphStoreException {
        ServiceHolder.getModelStoreManager().persistModel(
                ServiceHolder.getModelManager().getCurrentNodes(), ServiceHolder.getModelManager().getCurrentEdges());
    }

    @Test(groups = "connection-failure", expectedExceptions = GraphStoreException.class)
    public void testStoreCurrentModelWithFailingDatasource() throws GraphStoreException {
        Whitebox.setInternalState(ServiceHolder.getModelStoreManager(), "lastModel", (Object) null);
        ServiceHolder.getModelStoreManager().storeCurrentModel();
    }

    @Test(dependsOnGroups = "connection-failure", expectedExceptions = ModelException.class)
    public void testLoadWithUndefinedDatasource() throws ModelException {
        ServiceHolder.setDataSourceService(new FailureDatasourceServiceImpl(true, null));
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());

        ServiceHolder.setDataSourceService(null);
        ServiceHolder.setModelStoreManager(null);
        ServiceHolder.setModelManager(null);
    }

    @Test(dependsOnGroups = "connection-failure", expectedExceptions = ModelException.class)
    public void testLoadDatabaseWithConnectionException() throws ModelException {
        ServiceHolder.setDataSourceService(new FailureDatasourceServiceImpl(false,
                new FailureDatasource(true)));
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());

        ServiceHolder.setDataSourceService(null);
        ServiceHolder.setModelStoreManager(null);
        ServiceHolder.setModelManager(null);
    }

    @Test(dependsOnGroups = "connection-failure", expectedExceptions = ModelException.class)
    public void testLoadDatabaseWithSQLException() throws ModelException {
        ServiceHolder.setDataSourceService(new FailureDatasourceServiceImpl(false,
                new FailureDatasource(false)));
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());

        ServiceHolder.setDataSourceService(null);
        ServiceHolder.setModelStoreManager(null);
        ServiceHolder.setModelManager(null);
    }
}
