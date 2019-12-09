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
import com.google.gson.reflect.TypeToken;
import io.cellery.observability.model.generator.exception.GraphStoreException;
import io.cellery.observability.model.generator.model.Edge;
import io.cellery.observability.model.generator.model.Model;
import io.cellery.observability.model.generator.model.Node;
import org.apache.log4j.Logger;
import org.wso2.carbon.datasource.core.exception.DataSourceException;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;

/**
 * This handles the communication between the Model datasource, and the rest of the other components.
 */
public class ModelStoreManager {
    private static final Logger log = Logger.getLogger(ModelStoreManager.class);

    private static final String TABLE_NAME = "DependencyModelTable";
    private static final String DATASOURCE_NAME = "CELLERY_OBSERVABILITY_DB";

    private static final Gson gson = new Gson();
    private static final Type NODE_SET_TYPE = new TypeToken<HashSet<Node>>() {
    }.getType();
    private static final Type STRING_SET_TYPE = new TypeToken<HashSet<Edge>>() {
    }.getType();

    private DataSource dataSource;
    private Map<String, Model> lastModels;

    public ModelStoreManager() {
        try {
            this.dataSource = (DataSource) ServiceHolder.getDataSourceService().getDataSource(DATASOURCE_NAME);
            createTable();
            this.lastModels = loadLastModels();
        } catch (DataSourceException | GraphStoreException | SQLException e) {
            log.error("Unable to load the datasource : " + DATASOURCE_NAME +
                    " , and hence unable to schedule the periodic dependency persistence.", e);
        }
    }

    /**
     * Create the table required by the model manager.
     *
     * @throws SQLException if creating table failed
     * @throws GraphStoreException if getting a connection failed
     */
    private void createTable() throws SQLException, GraphStoreException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (RUNTIME VARCHAR(255) NOT NULL, " +
                "MODEL_TIMESTAMP TIMESTAMP NOT NULL, " +
                "NODES TEXT NOT NULL, " +
                "EDGES TEXT NOT NULL)");
        statement.execute();
        cleanupConnection(null, statement, connection);
    }

    /**
     * Load the last saved model.
     *
     * @return The last saved runtime models map
     * @throws GraphStoreException If loading the model failed
     */
    public Map<String, Model> loadLastModels() throws GraphStoreException {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT a.RUNTIME, a.NODES, a.EDGES FROM " +
                    TABLE_NAME + " AS a INNER JOIN  (" +
                    "SELECT RUNTIME, MAX(MODEL_TIMESTAMP) AS MAX_TIMESTAMP, NODES, EDGES FROM " + TABLE_NAME +
                    " GROUP BY RUNTIME, NODES, EDGES) AS b ON a.MODEL_TIMESTAMP = b.MAX_TIMESTAMP");
            ResultSet resultSet = statement.executeQuery();

            Map<String, Model> models = new HashMap<>();
            while (resultSet.next()) {
                String runtime = resultSet.getString(1);
                String nodes = resultSet.getString(2);
                String edges = resultSet.getString(3);
                Set<Node> nodesSet = gson.fromJson(nodes, NODE_SET_TYPE);
                Set<Edge> edgeSet = gson.fromJson(edges, STRING_SET_TYPE);
                models.put(runtime, new Model(nodesSet, edgeSet));
            }
            cleanupConnection(resultSet, statement, connection);
            return models.size() > 0 ? models : null;
        } catch (SQLException ex) {
            throw new GraphStoreException("Unable to load the graph from datasource : " + DATASOURCE_NAME, ex);
        }
    }

    /**
     * Load a list of models stored within a given time period.
     *
     * @param startTime The start of the time period
     * @param endTime   The end of the time period
     * @param runtime   The runtime of which the models should be fetched
     * @return The runtime model lists that were stored within the period
     * @throws GraphStoreException If loading the model failed
     */
    public List<Model> loadModels(long startTime, long endTime, String runtime) throws GraphStoreException {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT NODES, EDGES FROM " + TABLE_NAME +
                            " WHERE MODEL_TIMESTAMP >= ? AND MODEL_TIMESTAMP <= ? AND RUNTIME = ?" +
                            " ORDER BY MODEL_TIMESTAMP");
            statement.setTimestamp(1, new Timestamp(startTime));
            statement.setTimestamp(2, new Timestamp(endTime));
            statement.setString(3, runtime);
            ResultSet resultSet = statement.executeQuery();

            List<Model> models = new ArrayList<>();
            while (resultSet.next()) {
                String nodes = resultSet.getString(1);
                String edges = resultSet.getString(2);
                Set<Node> nodesSet = gson.fromJson(nodes, NODE_SET_TYPE);
                Set<Edge> edgeSet = gson.fromJson(edges, STRING_SET_TYPE);

                models.add(new Model(nodesSet, edgeSet));
            }
            cleanupConnection(resultSet, statement, connection);
            return models;
        } catch (SQLException ex) {
            throw new GraphStoreException("Unable to load the graph from datasource : " + DATASOURCE_NAME, ex);
        }
    }

    /**
     * Get a connection to the datasource which acts as the model persistence medium.
     *
     * @return The connection to the datasource
     * @throws SQLException if getting a connection failed
     * @throws GraphStoreException if datasource is not available.
     */
    private Connection getConnection() throws SQLException, GraphStoreException {
        if (this.dataSource !=  null) {
            return this.dataSource.getConnection();
        } else {
            throw new GraphStoreException("Datasource is not available");
        }
    }

    /**
     * Cleanup a conneciton to the datasource.
     *
     * @param rs The result set retrieved if any or null
     * @param stmt The statement used if any or null
     * @param conn The connection used if any or null
     */
    private void cleanupConnection(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Error on closing resultSet " + e.getMessage(), e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("Error on closing statement " + e.getMessage(), e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Error on closing connection " + e.getMessage(), e);
            }
        }
    }

    /**
     * Persist a particular model.
     *
     * @param models The runtime models to be saved
     * @throws GraphStoreException If storing the model failed
     */
    public void storeModel(Map<String, Model> models) throws GraphStoreException {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + TABLE_NAME
                    + " VALUES (?, ?, ?, ?)");
            Timestamp timestamp = Timestamp.from(Instant.now());
            for (Map.Entry<String, Model> modelEntry : models.entrySet()) {
                String nodesJson = gson.toJson(modelEntry.getValue().getNodes(), NODE_SET_TYPE);
                String edgesJson = gson.toJson(modelEntry.getValue().getEdges(), STRING_SET_TYPE);

                statement.setString(1, modelEntry.getKey());
                statement.setTimestamp(2, timestamp);
                statement.setString(3, nodesJson);
                statement.setString(4, edgesJson);
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
            cleanupConnection(null, statement, connection);

            Map<String, Model> newRuntimeModels = new HashMap<>();
            for (Map.Entry<String, Model> modelEntry : models.entrySet()) {
                newRuntimeModels.put(modelEntry.getKey(),
                        new Model(new HashSet<>(modelEntry.getValue().getNodes()),
                                new HashSet<>(modelEntry.getValue().getEdges())));
            }
            this.lastModels = newRuntimeModels;
        } catch (SQLException ex) {
            throw new GraphStoreException("Unable to persist the graph to the datasource: " + DATASOURCE_NAME, ex);
        }
    }

    /**
     * Store the current model in the Model Manager.
     *
     * @throws GraphStoreException If a failure occurs while storing
     */
    public void storeCurrentModel() throws GraphStoreException {
        try {
            Map<String, Model> currentRuntimeModels = ServiceHolder.getModelManager().getCurrentRuntimeModels();
            if (this.lastModels == null) {
                this.lastModels = loadLastModels();
            }
            if (currentRuntimeModels.size() != 0) {
                if (this.lastModels != null && Objects.equals(this.lastModels, currentRuntimeModels)) {
                    return;
                }
                this.storeModel(currentRuntimeModels);
            }
        } catch (GraphStoreException e) {
            log.error("Error occurred while handling the dependency graph persistence", e);
            throw e;
        }
    }

    /**
     * Clear the stored model.
     */
    public void clear() throws GraphStoreException {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE_NAME);
            statement.executeUpdate();
            connection.commit();
            cleanupConnection(null, statement, connection);
            this.lastModels = null;
        } catch (SQLException e) {
            throw new GraphStoreException("Failed to clear stored models", e);
        }
    }
}
