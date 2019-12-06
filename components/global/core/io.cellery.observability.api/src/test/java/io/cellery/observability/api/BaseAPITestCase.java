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

package io.cellery.observability.api;

import io.cellery.observability.api.auth.OIDCOauthManager;
import io.cellery.observability.api.exception.APIInvocationException;
import io.cellery.observability.api.exception.InvalidParamException;
import io.cellery.observability.api.interceptor.AuthInterceptor;
import io.cellery.observability.api.interceptor.CORSInterceptor;
import io.cellery.observability.api.internal.ServiceHolder;
import io.cellery.observability.api.siddhi.SiddhiStoreQueryManager;
import io.cellery.observability.model.generator.internal.ModelStoreManager;
import io.cellery.observability.model.generator.model.ModelManager;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.IObjectFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.ObjectFactory;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.config.provider.ConfigProviderImpl;
import org.wso2.carbon.config.reader.ConfigFileReader;
import org.wso2.carbon.config.reader.YAMLBasedConfigFileReader;
import org.wso2.carbon.datasource.core.DataSourceManager;
import org.wso2.carbon.datasource.core.DataSourceRepository;
import org.wso2.carbon.datasource.core.beans.DataSourceMetadata;
import org.wso2.carbon.datasource.core.impl.DataSourceServiceImpl;
import org.wso2.carbon.secvault.SecureVault;
import org.wso2.carbon.secvault.internal.SecureVaultImpl;
import org.wso2.extension.siddhi.store.rdbms.RDBMSEventTable;
import org.wso2.msf4j.MicroservicesRunner;
import org.wso2.siddhi.core.SiddhiManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sql.DataSource;

/**
 * Base Test Cases for APIs.
 */
public class BaseAPITestCase {

    private static final Logger logger = Logger.getLogger(BaseAPITestCase.class);

    private static final String CARBON_HOME_ENV = "carbon.home";
    private static final String WSO2_RUNTIME_ENV = "wso2.runtime";
    private static final int API_SERVER_PORT = 18123;

    protected static final String AUTH_HEADER_TOKEN = "token-part-1";
    protected static final String COOKIE_TOKEN = "token-part-2";
    protected static final String CLIENT_ID = "test-client-id";
    protected static final String CLIENT_SECRET = "test-client-secret";

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @BeforeClass
    public void initBaseTestCase() throws Exception {
        initializeEnvironment();
        initializeConfigProvider();
        initializeDataSources();
        initializeOidcOauthManager();
        initializeModelManager();
        if (logger.isDebugEnabled()) {
            logger.debug("Initialized Model Manager");
        }
        initializeSiddhiManager();
        if (logger.isDebugEnabled()) {
            logger.debug("Started Siddhi Apps");
        }
        startAPIServer();
        if (logger.isDebugEnabled()) {
            logger.debug("Started API Server listening on port " + API_SERVER_PORT);
        }
    }

    @AfterClass
    public void cleanUpBaseTestCase() {
        if (logger.isDebugEnabled()) {
            logger.debug("Stopping the microservice runner");
        }
        ServiceHolder.getMicroservicesRunner().stop();
        if (logger.isDebugEnabled()) {
            logger.debug("Stopping the Siddhi Apps");
        }
        ServiceHolder.getSiddhiStoreQueryManager().stop();

        ServiceHolder.setConfigProvider(null);
        ServiceHolder.setOidcOauthManager(null);
        ServiceHolder.setSiddhiManager(null);
        ServiceHolder.setMicroservicesRunner(null);
        ServiceHolder.setSiddhiStoreQueryManager(null);

        io.cellery.observability.model.generator.internal.ServiceHolder.setDataSourceService(null);
        io.cellery.observability.model.generator.internal.ServiceHolder.setModelStoreManager(null);
        io.cellery.observability.model.generator.internal.ServiceHolder.setModelManager(null);
    }

    /**
     * Initialize the Environment.
     */
    private void initializeEnvironment() {
        String carbonHomePath = this.getClass().getResource("/").getFile();
        System.setProperty(CARBON_HOME_ENV, carbonHomePath);
        System.setProperty(WSO2_RUNTIME_ENV, "worker");
    }

    /**
     * Initialize the Cellery Configuration using the mock deployment.yaml.
     */
    private void initializeConfigProvider() {
        Path configPath = Paths.get(System.getProperty(CARBON_HOME_ENV), "conf", "deployment.yaml");
        ConfigFileReader configFileReader = new YAMLBasedConfigFileReader(configPath);
        SecureVault secureVault = new SecureVaultImpl();
        ConfigProvider configProvider = new ConfigProviderImpl(configFileReader, secureVault);
        ServiceHolder.setConfigProvider(configProvider);
    }

    /**
     * Initialize Data Sources.
     *
     * @throws Exception if initializing data sources fails
     */
    private void initializeDataSources() throws Exception {
        DataSourceServiceImpl dataSourceService = new DataSourceServiceImpl();
        io.cellery.observability.model.generator.internal.ServiceHolder.setDataSourceService(dataSourceService);
        DataSourceManager dataSourceManager = DataSourceManager.getInstance();
        dataSourceManager.initDataSources(ServiceHolder.getConfigProvider());
    }

    /**
     * Initialize Open ID Connect OAuth Manager to allow all requests.
     *
     * @throws Exception if stubbing validate token fails
     */
    private void initializeOidcOauthManager() throws Exception {
        OIDCOauthManager oidcOauthManager = Mockito.mock(OIDCOauthManager.class);
        Mockito.when(oidcOauthManager.validateToken(AUTH_HEADER_TOKEN + COOKIE_TOKEN)).thenReturn(true);
        Mockito.when(oidcOauthManager.getClientId()).thenReturn(CLIENT_ID);
        Mockito.when(oidcOauthManager.getClientSecret()).thenReturn(CLIENT_SECRET);
        ServiceHolder.setOidcOauthManager(oidcOauthManager);
    }

    /**
     * Initialize the Model Manager.
     *
     * @throws Exception if creating model manager fails
     */
    private void initializeModelManager() throws Exception {
        ModelStoreManager modelStoreManager = new ModelStoreManager();
        io.cellery.observability.model.generator.internal.ServiceHolder.setModelStoreManager(modelStoreManager);
        ModelManager modelManager = new ModelManager();
        io.cellery.observability.model.generator.internal.ServiceHolder.setModelManager(modelManager);
    }

    /**
     * Initialize the Siddhi Manager.
     */
    private void initializeSiddhiManager() {
        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("rdbms:store", RDBMSEventTable.class);

        DataSourceRepository dataSourceRepository = DataSourceManager.getInstance().getDataSourceRepository();
        for (DataSourceMetadata metadatum : dataSourceRepository.getMetadata()) {
            String dataSourceName = metadatum.getName();
            siddhiManager.setDataSource(dataSourceName,
                    (DataSource) dataSourceRepository.getDataSource(dataSourceName).getDataSourceObject());
        }

        ServiceHolder.setSiddhiManager(siddhiManager);
    }

    /**
     * Start the Observability Portal API Server.
     */
    private void startAPIServer() {
        ServiceHolder.setMicroservicesRunner(new MicroservicesRunner(API_SERVER_PORT)
                .addGlobalRequestInterceptor(new CORSInterceptor(), new AuthInterceptor())
                .addExceptionMapper(new APIInvocationException.Mapper(), new InvalidParamException.Mapper())
                .deploy(
                        new DependencyModelAPI(), new AggregatedRequestsAPI(), new DistributedTracingAPI(),
                        new KubernetesAPI(), new InstanceAPI(), new AuthenticationAPI(), new UsersAPI()
                )
        );
        ServiceHolder.getMicroservicesRunner().start();
        ServiceHolder.setSiddhiStoreQueryManager(new SiddhiStoreQueryManager());
    }
}
