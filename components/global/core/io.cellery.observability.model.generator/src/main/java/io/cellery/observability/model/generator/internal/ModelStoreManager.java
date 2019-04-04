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

import com.google.common.graph.MutableNetwork;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.cellery.observability.model.generator.Node;
import io.cellery.observability.model.generator.Utils;
import io.cellery.observability.model.generator.exception.GraphStoreException;
import io.cellery.observability.model.generator.model.Model;
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
    private static final Type STRING_SET_TYPE = new TypeToken<HashSet<String>>() {
    }.getType();
    private DataSource dataSource;
    private Gson gson;
    private Model lastModel;


    ModelStoreManager() {
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

    private void createTable() throws SQLException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + TABLE_NAME +
                " (MODEL_TIME TIMESTAMP, NODES TEXT, EDGES TEXT)");
        statement.execute();
        cleanupConnection(null, statement, connection);
    }

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
        Set<String> edgeSet = gson.fromJson(edges, STRING_SET_TYPE);
        return new Model(nodesSet, Utils.getEdges(edgeSet));
    }

    public List<Model> loadModel(long fromTime, long toTime) throws GraphStoreException {
        try {
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + TABLE_NAME
                    + " WHERE MODEL_TIME >= ? AND MODEL_TIME <= ? ORDER BY MODEL_TIME");
            statement.setTimestamp(1, new Timestamp(fromTime));
            statement.setTimestamp(2, new Timestamp(toTime));
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
                    if (timestamp.getTime() < fromTime) {
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


    private Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
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

    public Model persistModel(MutableNetwork<Node, String> graph) throws GraphStoreException {
        try {
            String nodes = gson.toJson(graph.nodes(), NODE_SET_TYPE);
            String edges = gson.toJson(graph.edges(), STRING_SET_TYPE);
            Connection connection = getConnection();
            PreparedStatement statement = connection.prepareStatement("INSERT INTO " + TABLE_NAME
                    + " VALUES (?, ?, ?)");
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setString(2, nodes);
            statement.setString(3, edges);
            statement.executeUpdate();
            connection.commit();
            Model model = new Model(gson.fromJson(nodes, NODE_SET_TYPE),
                    Utils.getEdges(gson.fromJson(edges, STRING_SET_TYPE)));
            cleanupConnection(null, statement, connection);
            return model;
        } catch (SQLException ex) {
            throw new GraphStoreException("Unable to persist the graph to the datasource: " + DATASOURCE_NAME, ex);
        }
    }

    public void storeCurrentModel() {
        try {
            MutableNetwork<Node, String> currentModel = ServiceHolder.getModelManager().getDependencyGraph();
            if (lastModel == null) {
                this.lastModel = loadLastModel();
            }
            if (this.lastModel == null) {
                if (currentModel.nodes().size() != 0) {
                    lastModel = ServiceHolder.getModelStoreManager().persistModel(currentModel);
                }
            } else {
                Set<Node> currentNodes = currentModel.nodes();
                Set<String> currentEdges = currentModel.edges();
                Set<Node> lastNodes = this.lastModel.getNodes();
                Set<String> lastEdges = Utils.getEdgesString(this.lastModel.getEdges());
                if (currentEdges.size() == lastEdges.size() && currentNodes.size() == lastNodes.size()) {
                    if (isSameNodes(currentNodes, lastNodes) && isSameEdges(currentEdges, lastEdges)) {
                        return;
                    }
                }
                lastModel = ServiceHolder.getModelStoreManager().persistModel(currentModel);
            }
        } catch (GraphStoreException e) {
            log.error("Error occurred while handling the dependency graph persistence. ", e);
        }
    }

    private boolean isSameNodes(Set<Node> currentNodes, Set<Node> lastNodes) {
        boolean isSameModel = true;
        for (Node currentNode : currentNodes) {
            Node lastNode = null;
            //Find the node from the last persisted graph
            for (Node node : lastNodes) {
                if (node.equals(currentNode)) {
                    lastNode = node;
                    break;
                }
            }
            // Check the services within the node is also same.
            if (lastNode != null) {
                if (lastNode.getComponents().size() == currentNode.getComponents().size()) {
                    if (!lastNode.getComponents().containsAll(currentNode.getComponents())) {
                        isSameModel = false;
                        break;
                    }
                } else {
                    isSameModel = false;
                    break;
                }
                if (lastNode.getEdges().size() == currentNode.getEdges().size()) {
                    if (!lastNode.getEdges().containsAll(currentNode.getEdges())) {
                        isSameModel = false;
                        break;
                    }
                } else {
                    isSameModel = false;
                    break;
                }
            } else {
                isSameModel = false;
                break;
            }
        }
        return isSameModel;
    }

    private boolean isSameEdges(Set<String> currentEdges, Set<String> lastEdges) {
        return currentEdges.containsAll(lastEdges);
    }
}
