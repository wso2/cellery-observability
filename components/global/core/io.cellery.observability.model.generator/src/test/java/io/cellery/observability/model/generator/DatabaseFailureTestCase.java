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
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import java.lang.reflect.Field;

/**
 * This test case simulates database connection, and SQL failures.
 */
public class DatabaseFailureTestCase {

    @BeforeGroups(groups = "tempConnFailure")
    public void setFailureDatasource() throws IllegalAccessException, NoSuchFieldException {
        Field field = ServiceHolder.getModelStoreManager().getClass().getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(ServiceHolder.getModelStoreManager(), new FailureDatasource(true));
    }

    @Test(groups = "tempConnFailure", expectedExceptions = GraphStoreException.class)
    public void loadModelWithDatasource() throws GraphStoreException {
        ServiceHolder.getModelManager().getGraph(System.currentTimeMillis() - 10000, System.currentTimeMillis());
    }

    @Test(groups = "tempConnFailure", expectedExceptions = GraphStoreException.class)
    public void persistsModel() throws GraphStoreException {
        ServiceHolder.getModelStoreManager().persistModel(ServiceHolder.getModelManager().getDependencyGraph());
    }

    @Test(groups = "tempConnFailure", expectedExceptions = GraphStoreException.class)
    public void storeCurrentModel() throws GraphStoreException, NoSuchFieldException, IllegalAccessException {
        Field field = ServiceHolder.getModelStoreManager().getClass().getDeclaredField("lastModel");
        field.setAccessible(true);
        field.set(ServiceHolder.getModelStoreManager(), null);
        ServiceHolder.getModelStoreManager().storeCurrentModel();
    }

    @Test(dependsOnGroups = "tempConnFailure", expectedExceptions = ModelException.class)
    public void loadWithUndefinedDatasource() throws ModelException {
        ServiceHolder.setDataSourceService(new FailureDatasourceServiceImpl(true, null));
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());
    }

    @Test(dependsOnGroups = "tempConnFailure", expectedExceptions = ModelException.class)
    public void loadDatabaseWithConnectionException() throws ModelException {
        ServiceHolder.setDataSourceService(new FailureDatasourceServiceImpl(false,
                new FailureDatasource(true)));
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());
    }

    @Test(dependsOnGroups = "tempConnFailure", expectedExceptions = ModelException.class)
    public void loadDatabaseWithSQLException() throws ModelException {
        ServiceHolder.setDataSourceService(new FailureDatasourceServiceImpl(false,
                new FailureDatasource(false)));
        ServiceHolder.setModelStoreManager(new ModelStoreManager());
        ServiceHolder.setModelManager(new ModelManager());
    }
}
