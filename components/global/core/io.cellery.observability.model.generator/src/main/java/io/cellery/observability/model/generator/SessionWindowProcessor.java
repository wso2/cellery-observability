/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.cellery.observability.model.generator;

import org.apache.log4j.Logger;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.state.StateEvent;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.executor.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.SchedulingProcessor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.core.query.processor.stream.window.FindableProcessor;
import org.wso2.siddhi.core.table.Table;
import org.wso2.siddhi.core.util.Scheduler;
import org.wso2.siddhi.core.util.collection.operator.CompiledCondition;
import org.wso2.siddhi.core.util.collection.operator.MatchingMetaInfoHolder;
import org.wso2.siddhi.core.util.collection.operator.Operator;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.core.util.parser.OperatorParser;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;
import org.wso2.siddhi.query.api.expression.Expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Stream Processor which represent a Window operating based on a session.
 */
@Extension(
        name = "traceGroupWindow",
        namespace = "observe",
        description = "This is a session window that holds events that belong to a specific session. The events " +
                "that belong to a specific session are identified by a grouping attribute (i.e., a session key). A " +
                "session gap period is specified to determine the time period after which the session is considered " +
                "to be expired. A new event that arrives with a specific value for the session key is matched with" +
                " the session window with the same session key.\n " +
                " When performing aggregations for a specific session, you can include events with the matching " +
                "session key that arrive after the session is expired if required. This is done by specifying a " +
                "latency time period that is less than the session gap period.\n" +
                "To have aggregate functions with session windows, the events need to be grouped by the " +
                "session key via a 'group by' clause.",
        parameters = {
                @Parameter(name = "window.session",
                        description = "The time period for which the session considered is valid. This is specified" +
                                " in seconds, minutes, or milliseconds (i.e., 'min', 'sec', or 'ms'.",
                        type = {DataType.INT, DataType.LONG, DataType.TIME}),
                @Parameter(name = "window.key",
                        description = "The grouping attribute for events.",
                        type = {DataType.STRING}, optional = true, defaultValue = "default-key"),
                @Parameter(name = "window.allowedlatency",
                        description = "This specifies the time period for which the session window is valid after " +
                                "the expiration of the session. The time period specified here should be less than " +
                                "the session time gap (which is specified via the 'window.session' parameter).",
                        type = {DataType.INT, DataType.LONG, DataType.TIME}, optional = true, defaultValue = "0")
        },
        examples = {
                @Example(
                        syntax = "define stream PurchaseEventStream "
                                + "(user string, item_number int, price float, quantity int);\n"
                                + "\n"
                                + "@info(name='query0) \n"
                                + "from PurchaseEventStream#window.session(5 sec, user, 2 sec) \n"
                                + "select * \n"
                                + "insert all events into OutputStream;",
                        description = "This query processes events that arrive at the PurchaseEvent input stream. " +
                                "The 'user' attribute is the session key, and the session gap is 5 " +
                                "seconds. '2 sec' is specified as the allowed latency. Therefore, events with the " +
                                "matching user name that arrive 2 seconds after the expiration of the session are " +
                                "also considered when performing aggregations for the session identified by the given" +
                                " user name."
                )
        }
)
public class SessionWindowProcessor extends StreamProcessor implements SchedulingProcessor, FindableProcessor {

    private static final Logger log = Logger.getLogger(SessionWindowProcessor.class);

    private long sessionGap = 0;
    private VariableExpressionExecutor sessionKeyExecutor;
    private VariableExpressionExecutor timestampExecutor;
    private Scheduler scheduler;
    private Map<String, SessionContainer> sessionMap;
    private SessionContainer sessionContainer;
    private ComplexEventChunk<StreamEvent> expiredEventChunk;

    private static final String DEFAULT_KEY = "default-key";

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    protected List<Attribute> init(AbstractDefinition abstractDefinition, ExpressionExecutor[] expressionExecutors,
                                   ConfigReader configReader, SiddhiAppContext siddhiAppContext) {
        this.sessionMap = new ConcurrentHashMap<>();
        this.sessionContainer = new SessionContainer();
        this.expiredEventChunk = new ComplexEventChunk<>(false);

        if (attributeExpressionExecutors.length >= 1 && attributeExpressionExecutors.length <= 3) {

            if (attributeExpressionExecutors[0] instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.INT ||
                        attributeExpressionExecutors[0].getReturnType() == Attribute.Type.LONG) {
                    sessionGap = (Long) ((ConstantExpressionExecutor) attributeExpressionExecutors[0]).getValue();
                } else {
                    throw new SiddhiAppValidationException("Session window's session gap parameter should be either "
                            + "int or long, but found " + attributeExpressionExecutors[0].getReturnType());
                }
            } else {
                throw new SiddhiAppValidationException("Session window's 1st parameter, session gap"
                        + " should be a constant parameter attribute but "
                        + "found a dynamic attribute " + attributeExpressionExecutors[0].getClass().getCanonicalName());
            }

            if (attributeExpressionExecutors.length >= 2) {
                if (attributeExpressionExecutors[1] instanceof VariableExpressionExecutor) {
                    if (attributeExpressionExecutors[1].getReturnType() == Attribute.Type.STRING) {
                        sessionKeyExecutor = (VariableExpressionExecutor) attributeExpressionExecutors[1];
                    } else {
                        throw new SiddhiAppValidationException("Session window's session key parameter type"
                                + " should be string, but found " + attributeExpressionExecutors[1].getReturnType());
                    }
                } else {
                    throw new SiddhiAppValidationException("Session window's 2nd parameter, session key"
                            + " should be a dynamic parameter attribute but "
                            + "found a constant attribute "
                            + attributeExpressionExecutors[1].getClass().getCanonicalName());
                }
            }

            if (attributeExpressionExecutors.length >= 3) {
                if (attributeExpressionExecutors[2] instanceof VariableExpressionExecutor) {
                    if (attributeExpressionExecutors[2].getReturnType() == Attribute.Type.LONG) {
                        timestampExecutor = (VariableExpressionExecutor) attributeExpressionExecutors[2];
                    } else {
                        throw new SiddhiAppValidationException("Session window's timestamp parameter type"
                                + " should be string, but found " + attributeExpressionExecutors[2].getReturnType());
                    }
                } else {
                    throw new SiddhiAppValidationException("Session window's 3rd parameter, timestamp"
                            + " should be a dynamic parameter attribute but "
                            + "found a constant attribute "
                            + attributeExpressionExecutors[2].getClass().getCanonicalName());
                }
            }
        } else {
            throw new SiddhiAppValidationException("Session window should only have one to three parameters "
                    + "(<int|long|time> sessionGap, <String> sessionKey, <long> timestamp, "
                    + "but found " + attributeExpressionExecutors.length + " input attributes");

        }
        return new ArrayList<>();
    }

    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor processor, StreamEventCloner
            streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        String key = DEFAULT_KEY;
        synchronized (this) {
            while (streamEventChunk.hasNext()) {
                StreamEvent streamEvent = streamEventChunk.next();
                long eventTimestamp = streamEvent.getTimestamp();
                long maxTimestamp = eventTimestamp + sessionGap;
                if (streamEvent.getType() == StreamEvent.Type.CURRENT) {
                    long sessionEventTimestamp = eventTimestamp;
                    if (timestampExecutor != null) {
                        sessionEventTimestamp = (Long) timestampExecutor.execute(streamEvent);
                    }
                    if (sessionKeyExecutor != null) {
                        key = (String) sessionKeyExecutor.execute(streamEvent);
                    }

                    //get the session configuration based on session key
                    //if the map doesn't contain key, then a new sessionContainer
                    //object needs to be created.
                    if ((sessionContainer = sessionMap.get(key)) == null) {
                        sessionContainer = new SessionContainer(key, eventTimestamp);
                    }
                    sessionMap.put(key, sessionContainer);

                    StreamEvent clonedStreamEvent = streamEventCloner.copyStreamEvent(streamEvent);
                    clonedStreamEvent.setType(StreamEvent.Type.EXPIRED);

                    //if current session contains events
                    if (sessionContainer.isEmpty()) {
                        sessionContainer.add(sessionEventTimestamp, clonedStreamEvent);
                        sessionContainer.setEndTimestamp(maxTimestamp);
                        scheduler.notifyAt(maxTimestamp);
                    } else {
                        if (eventTimestamp >= sessionContainer.getStartTimestamp()) {
                            //check whether the event belongs to the same session
                            if (eventTimestamp <= sessionContainer.getEndTimestamp()) {
                                sessionContainer.add(sessionEventTimestamp, clonedStreamEvent);
                                sessionContainer.setEndTimestamp(maxTimestamp);
                                scheduler.notifyAt(maxTimestamp);
                            }
                        } else {
                            //when a late event arrives
                            addLateEvent(streamEventChunk, eventTimestamp, clonedStreamEvent, sessionEventTimestamp);
                        }
                    }
                } else {
                    currentSessionTimeout(eventTimestamp);
                }
            }
        }
        nextProcessor.process(streamEventChunk);
        if (expiredEventChunk != null && expiredEventChunk.getFirst() != null) {
            nextProcessor.process(expiredEventChunk);
            expiredEventChunk.clear();
        }
    }

    /**
     * Handles when the late event arrives to the system.
     */
    private void addLateEvent(ComplexEventChunk<StreamEvent> streamEventChunk,
                              long eventTimestamp, StreamEvent streamEvent, long sessionEventTimestamp) {
        //check the late event belongs to the same session
        if (eventTimestamp >= (sessionContainer.getStartTimestamp() - sessionGap)) {
            sessionContainer.add(sessionEventTimestamp, streamEvent);
            sessionContainer.setStartTimestamp(eventTimestamp);
        } else {
            streamEventChunk.remove();
            log.info("The event, " + streamEvent + " is late and it's session window has been timeout");
        }
    }

    /**
     * Checks all the sessions and get the expired session.
     */
    private void currentSessionTimeout(long eventTimestamp) {
        Collection<SessionContainer> sessionContainerList = sessionMap.values();
        TreeSet<SessionContainer> currentEndTimestamps = new TreeSet<>(sessionContainerList);
        for (SessionContainer aSessionContainer : currentEndTimestamps) {
            long sessionEndTime = aSessionContainer.getEndTimestamp();
            if (eventTimestamp >= sessionEndTime) {
                SessionContainer currentSessionContainer = sessionMap.get(aSessionContainer.getKey());
                ComplexEventChunk<StreamEvent> events = currentSessionContainer.generateEventChunk();
                if (events.getFirst() != null) {
                    expiredEventChunk.add(currentSessionContainer.generateEventChunk().getFirst());
                    currentSessionContainer.clear();
                }
            } else {
                break;
            }
        }
    }

    @Override
    public void start() {
        //Do nothing
    }

    @Override
    public void stop() {
        //Do nothing
    }

    @Override
    public synchronized Map<String, Object> currentState() {
        Map<String, Object> state = new HashMap<>();
        state.put("sessionMap", sessionMap);
        state.put("sessionContainer", sessionContainer);
        state.put("expiredEventChunk", expiredEventChunk);
        return state;
    }

    @Override
    public synchronized void restoreState(Map<String, Object> state) {
        sessionMap = (ConcurrentHashMap<String, SessionContainer>) state.get("sessionMap");
        sessionContainer = (SessionContainer) state.get("sessionContainer");
        expiredEventChunk = (ComplexEventChunk<StreamEvent>) state.get("expiredEventChunk");
    }

    @Override
    public synchronized StreamEvent find(StateEvent matchingEvent, CompiledCondition compiledCondition) {
        return ((Operator) compiledCondition).find(matchingEvent, expiredEventChunk, streamEventCloner);
    }

    @Override
    public CompiledCondition compileCondition(Expression condition, MatchingMetaInfoHolder matchingMetaInfoHolder,
                                              SiddhiAppContext siddhiAppContext,
                                              List<VariableExpressionExecutor> variableExpressionExecutors,
                                              Map<String, Table> tableMap, String queryName) {
        return OperatorParser.constructOperator(expiredEventChunk, condition, matchingMetaInfoHolder,
                siddhiAppContext, variableExpressionExecutors, tableMap, this.queryName);

    }
}
