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

import io.cellery.observability.model.generator.exception.GraphStoreException;
import io.cellery.observability.model.generator.internal.ServiceHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.core.config.SiddhiAppContext;
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
import java.util.List;
import java.util.Map;

/**
 * This is the Siddhi extension which remove nodes from the dependency model.
 */
@Extension(
        name = "removeNode",
        namespace = "model",
        description = "This remove nodes from the dependency model",
        examples = @Example(
                description = "This updates the dependency model based on the request",
                syntax = "model:removeNode(runtime, namespace, instance, component)\n"
                        + "select *\n"
                        + "insert into outputStream;"
        )
)
public class ModelRemoveNodeStreamProcessor extends StreamProcessor {
    private static final Logger logger = Logger.getLogger(ModelAddNodeStreamProcessor.class);

    private ExpressionExecutor runtimeExecutor;
    private ExpressionExecutor namespaceExecutor;
    private ExpressionExecutor instanceExecutor;
    private ExpressionExecutor componentExecutor;

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        synchronized (this) {
            while (streamEventChunk.hasNext()) {
                try {
                    StreamEvent incomingStreamEvent = streamEventChunk.next();
                    String runtime = (String) runtimeExecutor.execute(incomingStreamEvent);
                    String namespace = (String) namespaceExecutor.execute(incomingStreamEvent);
                    String instance = (String) instanceExecutor.execute(incomingStreamEvent);
                    String component = (String) componentExecutor.execute(incomingStreamEvent);

                    if (StringUtils.isNotEmpty(runtime) && StringUtils.isNotEmpty(namespace)
                            && StringUtils.isNotEmpty(instance) && StringUtils.isNotEmpty(component)) {
                        ServiceHolder.getModelManager().removeNode(runtime, namespace, instance, component);
                    }
                } catch (Throwable throwable) {
                    logger.error("Unexpected error occurred while processing the event " +
                            "in the model add node processor", throwable);
                }
            }
            try {
                ServiceHolder.getModelStoreManager().storeCurrentModel();
            } catch (GraphStoreException e) {
                logger.error("Failed to persist current dependency model", e);
            }
        }
        if (streamEventChunk.getFirst() != null) {
            nextProcessor.process(streamEventChunk);
        }
    }

    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition, ExpressionExecutor[] expressionExecutors,
                                   ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        if (expressionExecutors.length != 4) {
            throw new SiddhiAppCreationException("Four arguments are required");
        } else {
            if (expressionExecutors[0].getReturnType() == Attribute.Type.STRING) {
                runtimeExecutor = expressionExecutors[0];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the runtime "
                        + "field, but found a field with return type - "
                        + expressionExecutors[0].getReturnType());
            }

            if (expressionExecutors[1].getReturnType() == Attribute.Type.STRING) {
                namespaceExecutor = expressionExecutors[1];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the namespace " +
                        "field, but found a field with return type - "
                        + expressionExecutors[1].getReturnType());
            }

            if (expressionExecutors[2].getReturnType() == Attribute.Type.STRING) {
                instanceExecutor = expressionExecutors[2];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the instance " +
                        "field, but found a field with return type - "
                        + expressionExecutors[2].getReturnType());
            }

            if (expressionExecutors[3].getReturnType() == Attribute.Type.STRING) {
                componentExecutor = expressionExecutors[3];
            } else {
                throw new SiddhiAppCreationException("Expected a field with String return type for the component " +
                        "field, but found a field with return type - "
                        + expressionExecutors[3].getReturnType());
            }
        }
        return new ArrayList<>(0);
    }

    @Override
    public void start() {   // Do Nothing
    }

    @Override
    public void stop() {    // Do Nothing
    }

    @Override
    public Map<String, Object> currentState() {     // Do Nothing
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> map) { // Do Nothing
    }
}
