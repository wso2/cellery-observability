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
import java.util.HashSet;
import java.util.List;
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

    private static final Type NODE_SET_TYPE = new TypeToken<HashSet<Node>>() {
    }.getType();
    private static final Type STRING_SET_TYPE = new TypeToken<HashSet<Edge>>() {
    }.getType();

    private DataSource dataSource;
    private Gson gson;
    private Model lastModel;

    public ModelStoreManager() {
        try {
            this.dataSource = (DataSource) ServiceHolder.getDataSourceService().getDataSource(DATASOURCE_NAME);
            createTable();
            this.gson = new Gson();
            this.lastModel = loadLastModel();
        } catch (DataSourceException | GraphStoreException | SQLException e) {
            log.error("Unable to load the datasource : " + DATASOURCE_NAME +
                    " , and hence unable to schedule the periodic dependency persistence.", e);
        }
    }

    private void createTable() throws SQLException, GraphStoreException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (MODEL_TIME TIMESTAMP, NODES TEXT, EDGES TEXT)");
        statement.execute();
        cleanupConnection(null, statement, connection);
    }

    /**
     * Load the last saved model.
     *
     * @return The last saved model
     * @throws GraphStoreException If loading the model failed
     */
    public Model loadLastModel() throws GraphStoreException {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + TABLE_NAME
                    + " ORDER BY MODEL_TIME DESC LIMIT 1");
            ResultSet resultSet = statement.executeQuery();
            Model model = null;
            if (resultSet.next()) {
                model = getModel(resultSet);
            }
            cleanupConnection(resultSet, statement, connection);
            return model;
        } catch (SQLException ex) {
            throw new GraphStoreException("Unable to load the graph from datasource : " + DATASOURCE_NAME, ex);
        }
    }

    private Model getModel(ResultSet resultSet) throws SQLException {
        String nodes = resultSet.getString(2);
        String edges = resultSet.getString(3);
        Set<Node> nodesSet = gson.fromJson(nodes, NODE_SET_TYPE);
        Set<Edge> edgeSet = gson.fromJson(edges, STRING_SET_TYPE);
        return new Model(nodesSet, edgeSet);
    }

    /**
     * Load a list of models stored within a given time period.
     *
     * @param startTime The start of the time period
     * @param endTime The end of the time period
     * @return The list of model that were stored within the period
     * @throws GraphStoreException If loading the model failed
     */
    public List<Model> loadModel(long startTime, long endTime) throws GraphStoreException {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + TABLE_NAME
                    + " WHERE MODEL_TIME >= ? AND MODEL_TIME <= ? ORDER BY MODEL_TIME");
            statement.setTimestamp(1, new Timestamp(startTime));
            statement.setTimestamp(2, new Timestamp(endTime));
            ResultSet resultSet = statement.executeQuery();
            List<Model> models = new ArrayList<>();
            while (resultSet.next()) {
                models.add(getModel(resultSet));
            }
            if (models.isEmpty()) {
                cleanupConnection(resultSet, statement, null);
                statement = connection.prepareStatement("SELECT * FROM " + TABLE_NAME
                        + " ORDER BY MODEL_TIME DESC LIMIT 1");
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    Timestamp timestamp = resultSet.getTimestamp(1);
                    if (timestamp.getTime() < startTime) {
                        models.add(getModel(resultSet));
                    }
                }
                cleanupConnection(resultSet, statement, connection);
            } else {
                cleanupConnection(resultSet, statement, connection);
            }
            return models;
        } catch (SQLException ex) {
            throw new GraphStoreException("Unable to load the graph from datasource : " + DATASOURCE_NAME, ex);
        }
    }


    private Connection getConnection() throws SQLException, GraphStoreException {
        if (this.dataSource !=  null) {
            return this.dataSource.getConnection();
        } else {
            throw new GraphStoreException("Datasource is not available!");
        }
    }

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
     * @param nodes The set of nodes in the model
     * @param edges The set of edges in the model
     * @throws GraphStoreException If storing the model failed
     */
    public void persistModel(Set<Node> nodes, Set<Edge> edges) throws GraphStoreException {
        try {
            String nodesJson = gson.toJson(nodes, NODE_SET_TYPE);
            String edgesJson = gson.toJson(edges, STRING_SET_TYPE);
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + TABLE_NAME
                    + " VALUES (?, ?, ?)");
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setString(2, nodesJson);
            statement.setString(3, edgesJson);
            statement.executeUpdate();
            connection.commit();
            cleanupConnection(null, statement, connection);
            this.lastModel = new Model(new HashSet<>(nodes), new HashSet<>(edges));
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
            Set<Node> currentNodes = ServiceHolder.getModelManager().getCurrentNodes();
            Set<Edge> currentEdges = ServiceHolder.getModelManager().getCurrentEdges();
            if (this.lastModel == null) {
                this.lastModel = loadLastModel();
            }
            if (this.lastModel == null) {
                if (currentNodes.size() != 0) {
                    this.persistModel(currentNodes, currentEdges);
                }
            } else {
                if (Objects.equals(this.lastModel.getNodes(), currentNodes)
                        && Objects.equals(this.lastModel.getEdges(), currentEdges)) {
                    return;
                }
                this.persistModel(currentNodes, currentEdges);
            }
        } catch (GraphStoreException e) {
            log.error("Error occurred while handling the dependency graph persistence. ", e);
            throw e;
        }
    }

    /**
     * Clear the stored model.
     */
    public void clear() throws GraphStoreException, SQLException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement("DELETE FROM " + TABLE_NAME);
        statement.executeUpdate();
        connection.commit();
        cleanupConnection(null, statement, connection);
        this.lastModel = null;
    }
}
