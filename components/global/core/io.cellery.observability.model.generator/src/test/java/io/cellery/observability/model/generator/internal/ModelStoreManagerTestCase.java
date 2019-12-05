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

package io.cellery.observability.model.generator.internal;

import com.google.gson.Gson;
import io.cellery.observability.model.generator.exception.GraphStoreException;
import io.cellery.observability.model.generator.model.Edge;
import io.cellery.observability.model.generator.model.Model;
import io.cellery.observability.model.generator.model.ModelManager;
import io.cellery.observability.model.generator.model.Node;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.exception.DataSourceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

/**
 * Model Store Manager test cases.
 */
public class ModelStoreManagerTestCase {
    private static final String DATASOURCE_NAME = "CELLERY_OBSERVABILITY_DB";
    private static final Gson gson = new Gson();

    @AfterMethod
    public void cleanUp() {
        ServiceHolder.setDataSourceService(null);
    }

    @Test
    public void testInitialization() throws Exception {
        Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
        nodeA.setInstanceKind("Cell");
        Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
        nodeB.setInstanceKind("Composite");
        Node nodeC = new Node("runtime-a", "namespace-a", "instance-c", "component-a");
        nodeC.setInstanceKind("Cell");

        Edge edgeA = new Edge(nodeA, nodeB);
        Edge edgeB = new Edge(nodeA, nodeC);

        Set<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC));
        Set<Edge> edges = new HashSet<>(Arrays.asList(edgeA, edgeB));
        mockDataSourceService(Collections.singletonList(new Model(nodes, edges)));

        ModelStoreManager modelStoreManager = new ModelStoreManager();
        Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
        Assert.assertNotNull(lastModel);
        Assert.assertEquals(lastModel.getNodes(), nodes);
        Assert.assertEquals(lastModel.getEdges(), edges);
        Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
    }

    @Test
    public void testInitializationWithNoSavedModel() throws Exception {
        mockDataSourceService(Collections.emptyList());
        ModelStoreManager modelStoreManager = new ModelStoreManager();
        Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
        Assert.assertNull(lastModel);
        Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
    }

    @Test
    public void testInitializationWithGetDataSourceThrowingException() throws Exception {
        DataSourceService dataSourceService = Mockito.mock(DataSourceService.class);
        Mockito.when(dataSourceService.getDataSource(DATASOURCE_NAME)).thenThrow(
                new DataSourceException("Test Exception"));
        ServiceHolder.setDataSourceService(dataSourceService);

        ModelStoreManager modelStoreManager = new ModelStoreManager();
        Assert.assertNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        Assert.assertNull(Whitebox.getInternalState(modelStoreManager, "lastModel"));
    }

    @Test
    public void testInitializationWithGetDataSourceReturningNull() throws Exception {
        DataSourceService dataSourceService = Mockito.mock(DataSourceService.class);
        Mockito.when(dataSourceService.getDataSource(DATASOURCE_NAME)).thenReturn(null);
        ServiceHolder.setDataSourceService(dataSourceService);

        ModelStoreManager modelStoreManager = new ModelStoreManager();
        Assert.assertNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        Assert.assertNull(Whitebox.getInternalState(modelStoreManager, "lastModel"));
    }

    @Test
    public void testInitializationWithCreateTableThrowingException() throws Exception {
        DataSource dataSource = Mockito.mock(DataSource.class);
        Mockito.when(dataSource.getConnection()).thenThrow(new SQLException("Test Exception"));

        DataSourceService dataSourceService = Mockito.mock(DataSourceService.class);
        Mockito.when(dataSourceService.getDataSource(DATASOURCE_NAME)).thenReturn(dataSource);
        ServiceHolder.setDataSourceService(dataSourceService);

        ModelStoreManager modelStoreManager = new ModelStoreManager();
        Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        Assert.assertNull(Whitebox.getInternalState(modelStoreManager, "lastModel"));
    }

    @Test
    public void testInitializationWithLoadLastModelThrowingException() throws Exception {
        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        Mockito.when(statement.executeQuery()).thenReturn(Mockito.mock(ResultSet.class));

        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);

        DataSource dataSource = Mockito.mock(DataSource.class);
        final int[] invocationCount = {0};
        Mockito.when(dataSource.getConnection()).then(invocationOnMock -> {
            invocationCount[0]++;
            if (invocationCount[0] == 2) {  // Second call loads the model
                throw new SQLException("Test Exception");
            }
            return connection;
        });

        DataSourceService dataSourceService = Mockito.mock(DataSourceService.class);
        Mockito.when(dataSourceService.getDataSource(DATASOURCE_NAME)).thenReturn(dataSource);
        ServiceHolder.setDataSourceService(dataSourceService);

        ModelStoreManager modelStoreManager = new ModelStoreManager();
        Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        Assert.assertNull(Whitebox.getInternalState(modelStoreManager, "lastModel"));
    }

    @Test
    public void testLoadModel() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("runtime-a", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);
            Edge edgeB = new Edge(nodeA, nodeC);

            Set<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC));
            Set<Edge> edges = new HashSet<>(Arrays.asList(edgeA, edgeB));
            DataSource dataSource = mockDataSource(Collections.singletonList(new Model(nodes, edges)));
            Whitebox.setInternalState(modelStoreManager, "dataSource", dataSource);

            List<Model> models = modelStoreManager.loadModel(1233542342, 1233572342);
            Assert.assertEquals(models.size(), 1);
            Assert.assertEquals(models.get(0).getNodes(), nodes);
            Assert.assertEquals(models.get(0).getEdges(), edges);

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
    }

    @Test(expectedExceptions = GraphStoreException.class)
    public void testLoadModelWithSqlException() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            DataSource dataSource = Mockito.mock(DataSource.class);
            Mockito.when(dataSource.getConnection()).thenThrow(new SQLException("Test Exception"));
            Whitebox.setInternalState(modelStoreManager, "dataSource", dataSource);
            modelStoreManager.loadModel(1233542342, 1233572342);
        }
    }

    @Test
    public void testPersistModel() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("runtime-a", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);
            Edge edgeB = new Edge(nodeA, nodeC);

            Set<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC));
            Set<Edge> edges = new HashSet<>(Arrays.asList(edgeA, edgeB));

            PreparedStatement statement;
            {
                statement = Mockito.mock(PreparedStatement.class);
                Mockito.when(statement.executeUpdate()).thenReturn(1);

                Connection connection = Mockito.mock(Connection.class);
                Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);

                DataSource dataSource = Whitebox.getInternalState(modelStoreManager, "dataSource");
                Mockito.when(dataSource.getConnection()).thenReturn(connection);
            }

            modelStoreManager.persistModel(nodes, edges);
            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNotNull(lastModel);
            Assert.assertEquals(lastModel.getNodes(), nodes);
            Assert.assertEquals(lastModel.getEdges(), edges);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));

            Mockito.verify(statement, Mockito.times(1))
                    .setTimestamp(Mockito.eq(1), Mockito.any(Timestamp.class));
            Mockito.verify(statement, Mockito.times(1))
                    .setString(Mockito.eq(2), Mockito.eq(gson.toJson(nodes)));
            Mockito.verify(statement, Mockito.times(1))
                    .setString(Mockito.eq(3), Mockito.eq(gson.toJson(edges)));
        }
    }

    @Test(expectedExceptions = GraphStoreException.class)
    public void testPersistModelWithSqlException() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            DataSource dataSource = Mockito.mock(DataSource.class);
            Mockito.when(dataSource.getConnection()).thenThrow(new SQLException("Test Exception"));
            Whitebox.setInternalState(modelStoreManager, "dataSource", dataSource);
            modelStoreManager.persistModel(Collections.emptySet(), Collections.emptySet());
        }
    }

    @Test
    public void testStoreCurrentModel() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("runtime-a", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);
            Edge edgeB = new Edge(nodeA, nodeC);

            Set<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC));
            Set<Edge> edges = new HashSet<>(Arrays.asList(edgeA, edgeB));
            ModelManager modelManager = Mockito.mock(ModelManager.class);
            Mockito.when(modelManager.getCurrentNodes()).thenReturn(nodes);
            Mockito.when(modelManager.getCurrentEdges()).thenReturn(edges);

            modelStoreManager = Mockito.spy(modelStoreManager);
            Mockito.doReturn(null).when(modelStoreManager).loadLastModel();
            Mockito.doNothing().when(modelStoreManager).persistModel(nodes, edges);
            ServiceHolder.setModelManager(modelManager);

            modelStoreManager.storeCurrentModel();
            Mockito.verify(modelStoreManager, Mockito.times(1)).persistModel(nodes, edges);
        }
    }

    @Test
    public void testStoreCurrentModelWithEmptyNodes() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            Set<Node> nodes = Collections.emptySet();
            Set<Edge> edges = Collections.emptySet();
            ModelManager modelManager = Mockito.mock(ModelManager.class);
            Mockito.when(modelManager.getCurrentNodes()).thenReturn(nodes);
            Mockito.when(modelManager.getCurrentEdges()).thenReturn(edges);

            modelStoreManager = Mockito.spy(modelStoreManager);
            Mockito.doReturn(null).when(modelStoreManager).loadLastModel();
            Mockito.doNothing().when(modelStoreManager).persistModel(nodes, edges);
            ServiceHolder.setModelManager(modelManager);

            modelStoreManager.storeCurrentModel();
            Mockito.verify(modelStoreManager, Mockito.times(0)).persistModel(nodes, edges);
        }
    }

    @Test
    public void testStoreCurrentModelWithUpdatesFromLastModel() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            {
                Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
                nodeA.setInstanceKind("Cell");
                Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
                nodeB.setInstanceKind("Composite");
                Node nodeC = new Node("runtime-a", "namespace-a", "instance-c", "component-a");
                nodeC.setInstanceKind("Cell");

                Edge edgeA = new Edge(nodeA, nodeB);
                Edge edgeB = new Edge(nodeA, nodeC);

                Model lastModel = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC)),
                        new HashSet<>(Arrays.asList(edgeA, edgeB)));
                modelStoreManager = Mockito.spy(modelStoreManager);
                Mockito.doReturn(lastModel).when(modelStoreManager).loadLastModel();
            }
            {
                Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
                nodeA.setInstanceKind("Cell");
                Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
                nodeB.setInstanceKind("Composite");

                Edge edgeA = new Edge(nodeA, nodeB);

                Set<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB));
                Set<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
                ModelManager modelManager = Mockito.mock(ModelManager.class);
                Mockito.when(modelManager.getCurrentNodes()).thenReturn(nodes);
                Mockito.when(modelManager.getCurrentEdges()).thenReturn(edges);

                Mockito.doNothing().when(modelStoreManager).persistModel(nodes, edges);
                ServiceHolder.setModelManager(modelManager);

                modelStoreManager.storeCurrentModel();
                Mockito.verify(modelStoreManager, Mockito.times(1)).persistModel(nodes, edges);
            }
        }
    }

    @Test
    public void testStoreCurrentModelWithNoUpdatesFromLastModel() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            {
                Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
                nodeA.setInstanceKind("Cell");
                Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
                nodeB.setInstanceKind("Composite");

                Edge edgeA = new Edge(nodeA, nodeB);

                Model lastModel = new Model(new HashSet<>(Arrays.asList(nodeA, nodeB)),
                        new HashSet<>(Collections.singletonList(edgeA)));
                modelStoreManager = Mockito.spy(modelStoreManager);
                Mockito.doReturn(lastModel).when(modelStoreManager).loadLastModel();
            }
            {
                Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
                nodeA.setInstanceKind("Cell");
                Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
                nodeB.setInstanceKind("Composite");

                Edge edgeA = new Edge(nodeA, nodeB);

                Set<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB));
                Set<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
                ModelManager modelManager = Mockito.mock(ModelManager.class);
                Mockito.when(modelManager.getCurrentNodes()).thenReturn(nodes);
                Mockito.when(modelManager.getCurrentEdges()).thenReturn(edges);

                Mockito.doNothing().when(modelStoreManager).persistModel(nodes, edges);
                ServiceHolder.setModelManager(modelManager);

                modelStoreManager.storeCurrentModel();
                Mockito.verify(modelStoreManager, Mockito.times(0)).persistModel(nodes, edges);
            }
        }
    }

    @Test(expectedExceptions = GraphStoreException.class)
    public void testStoreCurrentModelWithPersistModelThrowingException() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            modelStoreManager = Mockito.spy(modelStoreManager);
            Mockito.doReturn(null).when(modelStoreManager).loadLastModel();
            {
                Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
                nodeA.setInstanceKind("Cell");
                Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
                nodeB.setInstanceKind("Composite");

                Edge edgeA = new Edge(nodeA, nodeB);

                Set<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB));
                Set<Edge> edges = new HashSet<>(Collections.singletonList(edgeA));
                ModelManager modelManager = Mockito.mock(ModelManager.class);
                Mockito.when(modelManager.getCurrentNodes()).thenReturn(nodes);
                Mockito.when(modelManager.getCurrentEdges()).thenReturn(edges);

                Mockito.doThrow(new GraphStoreException("Test Exception")).when(modelStoreManager)
                        .persistModel(nodes, edges);
                ServiceHolder.setModelManager(modelManager);

                modelStoreManager.storeCurrentModel();
            }
        }
    }

    @Test
    public void testClear() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            Node nodeA = new Node("runtime-a", "namespace-a", "instance-a", "component-a");
            nodeA.setInstanceKind("Cell");
            Node nodeB = new Node("runtime-a", "namespace-a", "instance-b", "component-a");
            nodeB.setInstanceKind("Composite");
            Node nodeC = new Node("runtime-a", "namespace-a", "instance-c", "component-a");
            nodeC.setInstanceKind("Cell");

            Edge edgeA = new Edge(nodeA, nodeB);
            Edge edgeB = new Edge(nodeA, nodeC);

            HashSet<Node> nodes = new HashSet<>(Arrays.asList(nodeA, nodeB, nodeC));
            HashSet<Edge> edges = new HashSet<>(Arrays.asList(edgeA, edgeB));
            List<Model> models = Collections.singletonList(new Model(nodes, edges));
            mockDataSourceService(models);
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNotNull(lastModel);
            Assert.assertEquals(lastModel.getNodes(), nodes);
            Assert.assertEquals(lastModel.getEdges(), edges);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            PreparedStatement statement = Mockito.mock(PreparedStatement.class);
            Mockito.when(statement.executeUpdate()).thenReturn(1);

            Connection connection = Mockito.mock(Connection.class);
            Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);

            DataSource dataSource = Mockito.mock(DataSource.class);
            Mockito.when(dataSource.getConnection()).thenReturn(connection);
            Whitebox.setInternalState(modelStoreManager, "dataSource", dataSource);

            modelStoreManager.clear();
            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
    }

    @Test(expectedExceptions = GraphStoreException.class)
    public void testClearWithGraphStoreException() throws Exception {
        ModelStoreManager modelStoreManager;
        {
            mockDataSourceService(Collections.emptyList());
            modelStoreManager = new ModelStoreManager();

            Model lastModel = Whitebox.getInternalState(modelStoreManager, "lastModel");
            Assert.assertNull(lastModel);
            Assert.assertNotNull(Whitebox.getInternalState(modelStoreManager, "dataSource"));
        }
        {
            DataSource dataSource = Mockito.mock(DataSource.class);
            Mockito.when(dataSource.getConnection()).thenThrow(new SQLException("Test Exception"));
            Whitebox.setInternalState(modelStoreManager, "dataSource", dataSource);
            modelStoreManager.clear();
        }
    }

    /**
     * Mock data source service to return models.
     *
     * @param models Models to return
     * @throws Exception If mocking fails
     */
    private void mockDataSourceService(List<Model> models) throws Exception {
        DataSource dataSource = mockDataSource(models);
        DataSourceService dataSourceService = Mockito.mock(DataSourceService.class);
        Mockito.when(dataSourceService.getDataSource(DATASOURCE_NAME)).thenReturn(dataSource);
        ServiceHolder.setDataSourceService(dataSourceService);
    }

    /**
     * Mock a data source to return model.
     *
     * @param models Model to return
     * @return mocked data source
     * @throws Exception If mocking fails
     */
    private DataSource mockDataSource(List<Model> models) throws Exception {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        final int[] nextCallCount = {0};
        Mockito.when(resultSet.getString(Mockito.eq(2))).then(
                invocationOnMock -> gson.toJson(models.get(nextCallCount[0] - 1).getNodes()));
        Mockito.when(resultSet.getString(Mockito.eq(3))).then(
                invocationOnMock -> gson.toJson(models.get(nextCallCount[0] - 1).getEdges()));
        Mockito.when(resultSet.next()).then(invocationOnMock -> {
            nextCallCount[0]++;
            return nextCallCount[0] <= models.size();
        });

        PreparedStatement statement = Mockito.mock(PreparedStatement.class);
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);

        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.prepareStatement(Mockito.anyString())).thenReturn(statement);

        DataSource dataSource = Mockito.mock(DataSource.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        return dataSource;
    }
}
