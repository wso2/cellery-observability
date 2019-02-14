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
package io.cellery.observability.model.generator;

import io.cellery.observability.model.generator.internal.ServiceHolder;
import org.apache.log4j.Logger;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the Siddhi extension which generates the dependency graph for the spans created.
 */
@Extension(
        name = "modelGenerator",
        namespace = "observe",
        description = "This generates the dependency model of the spans",
        examples = @Example(description = "TBD"
                , syntax = "from inputStream#tracing:dependencyTree(componentName, spanId, parentId, serviceName," +
                " tags) \" +\n" +
                "                \"select * \n" +
                "                \"insert into outputStream;")
)
public class ModelGenerationExtension extends StreamProcessor {

    private static final Logger log = Logger.getLogger(ModelGenerationExtension.class);

    private ExpressionExecutor cellNameExecutor;
    private ExpressionExecutor serviceNameExecutor;
    private ExpressionExecutor operationNameExecutor;
    private ExpressionExecutor traceIdExecutor;
    private ExpressionExecutor spanIdExecutor;
    private ExpressionExecutor parentIdExecutor;
    private ExpressionExecutor spanKindExecutor;
    private Map<String, Node> cellCache = new ConcurrentHashMap<>();

    @Override
    protected void process(ComplexEventChunk<StreamEvent> complexEventChunk, Processor processor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        if (complexEventChunk.getFirst() != null && complexEventChunk.getFirst().getType().
                equals(ComplexEvent.Type.EXPIRED)) {
            HashMap<String, Node> nodeCache = new HashMap<>();
            while (complexEventChunk.hasNext()) {
                StreamEvent streamEvent = complexEventChunk.next();
                String cellName = (String) cellNameExecutor.execute(streamEvent);
                String serviceName = (String) serviceNameExecutor.execute(streamEvent);
                String operationName = (String) operationNameExecutor.execute(streamEvent);
                String spanId = (String) spanIdExecutor.execute(streamEvent);
                String parentId = (String) parentIdExecutor.execute(streamEvent);
                log.info("SpanId: " + spanId + ", parentId: " + parentId + " , serviceName: " + serviceName +
                        " , opName: " + operationName);
                if (cellName != null && !cellName.isEmpty()
                        && !operationName.equalsIgnoreCase(Constants.IGNORE_OPERATION_NAME)) {
                    Node node = new Node(cellName);
                    Node cachedValue;
                    if ((cachedValue = cellCache.putIfAbsent(cellName, node)) != null) {
                        node = cachedValue;
                    }
                    node.addService(serviceName);
                    ServiceHolder.getModelManager().addNode(node);
                    nodeCache.put(spanId, node);
                    if (parentId != null) {
                        Node parentNode = nodeCache.get(parentId);
                        ServiceHolder.getModelManager().addLink(parentNode, node, serviceName);
                    }
                }
            }
        } else {
            processor.process(complexEventChunk);
        }
    }

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
                                   ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        if (expressionExecutors.length != 6) {
            throw new SiddhiAppCreationException("Minimum number of attributes is six");
        } else {
            if (expressionExecutors[0].getReturnType() == Attribute.Type.STRING) {
                cellNameExecutor = expressionExecutors[0];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the component name "
                        + "field, but found a field with return type - " + expressionExecutors[0].getReturnType());
            }

            if (expressionExecutors[1].getReturnType() == Attribute.Type.STRING) {
                serviceNameExecutor = expressionExecutors[1];
            } else {
                throw new SiddhiAppCreationException("Expected a field with Long return type for the span id field," +
                        "but found a field with return type - " + expressionExecutors[1].getReturnType());
            }

            if (expressionExecutors[2].getReturnType() == Attribute.Type.STRING) {
                operationNameExecutor = expressionExecutors[2];
            } else {
                throw new SiddhiAppCreationException("Expected a field with Long return type for the parent id field,"
                        + "but found a field with return type - " + expressionExecutors[2].getReturnType());
            }

            if (expressionExecutors[3].getReturnType() == Attribute.Type.STRING) {
                spanIdExecutor = expressionExecutors[3];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the service name" +
                        " field, but found a field with return type - " + expressionExecutors[3].getReturnType());
            }

            if (expressionExecutors[4].getReturnType() == Attribute.Type.STRING) {
                parentIdExecutor = expressionExecutors[4];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the tags field," +
                        "but found a field with return type - " + expressionExecutors[4].getReturnType());
            }

            if (expressionExecutors[5].getReturnType() == Attribute.Type.STRING) {
                spanKindExecutor = expressionExecutors[5];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the " +
                        "spanKind field, but found a field with return type - "
                        + expressionExecutors[5].getReturnType());
            }

            if (expressionExecutors[6].getReturnType() == Attribute.Type.STRING) {
                traceIdExecutor = expressionExecutors[6];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the " +
                        "traceId field, but found a field with return type - "
                        + expressionExecutors[5].getReturnType());
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public Map<String, Object> currentState() {
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> map) {

    }
}
