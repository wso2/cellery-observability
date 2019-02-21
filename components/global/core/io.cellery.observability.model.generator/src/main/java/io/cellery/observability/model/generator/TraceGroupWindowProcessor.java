/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.siddhi.core.event.ComplexEvent;
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
 * Implementation of Stream Processor that groups the trace based on the trace key, and releases the trace
 * together after the idle timeout.
 */
@Extension(
        name = "traceGroupWindow",
        namespace = "observe",
        description = "This window groups the spans of the specific trace key and sends as expired events after " +
                "the window. The spans are collected until idle time configured in window.period, and if no spans are "
                + "received within the idle time, the spans are grouped together and released from window as expired "
                + "events.",
        parameters = {
                @Parameter(name = "window.period",
                        description = "The time period for which the trace considered is valid. This is specified" +
                                " in seconds, minutes, or milliseconds (i.e., 'min', 'sec', or 'ms'.",
                        type = {DataType.INT, DataType.LONG, DataType.TIME}),
                @Parameter(name = "window.key",
                        description = "The grouping attribute for events. ie, trace-key attribute",
                        type = {DataType.STRING})
        },
        examples = {
                @Example(
                        syntax = "from ProcessedZipkinStream#observe:traceGroupWindow(60 sec,traceId,startTime) \n"
                                + "select * \n"
                                + "insert all events into OutputStream;",
                        description = "This will send all events to output stream immediately, and also collect the " +
                                "events until the configured window.time, and group all events as a single event chunk "
                                + "as expired events."
                )
        }
)
public class TraceGroupWindowProcessor extends StreamProcessor implements SchedulingProcessor, FindableProcessor {

    private static final Logger log = Logger.getLogger(TraceGroupWindowProcessor.class);

    private long idleTimeGap = 0;
    private VariableExpressionExecutor tracekeyExecutor;
    private Scheduler scheduler;
    private Map<String, TraceGroup> traceGroupMap;
    private TraceGroup traceGroup;
    private ComplexEventChunk<StreamEvent> expiredEventChunk;

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
        this.traceGroupMap = new ConcurrentHashMap<>();
        this.traceGroup = new TraceGroup();
        this.expiredEventChunk = new ComplexEventChunk<>(false);

        if (attributeExpressionExecutors.length == 2) {

            if (attributeExpressionExecutors[0] instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.INT ||
                        attributeExpressionExecutors[0].getReturnType() == Attribute.Type.LONG) {
                    idleTimeGap = (Long) ((ConstantExpressionExecutor) attributeExpressionExecutors[0]).getValue();
                } else {
                    throw new SiddhiAppValidationException("Tracegroup window's idle time gap parameter should be" +
                            " either int or long, but found " + attributeExpressionExecutors[0].getReturnType());
                }
            } else {
                throw new SiddhiAppValidationException("Tracegroup window's 1st parameter, idle time gap"
                        + " should be a constant parameter attribute but "
                        + "found a dynamic attribute " + attributeExpressionExecutors[0].getClass().getCanonicalName());
            }

            if (attributeExpressionExecutors[1] instanceof VariableExpressionExecutor) {
                if (attributeExpressionExecutors[1].getReturnType() == Attribute.Type.STRING) {
                    tracekeyExecutor = (VariableExpressionExecutor) attributeExpressionExecutors[1];
                } else {
                    throw new SiddhiAppValidationException("Tracegroup window's trace key parameter type"
                            + " should be string, but found " + attributeExpressionExecutors[1].getReturnType());
                }
            } else {
                throw new SiddhiAppValidationException("Tracegroup window's 2nd parameter, trace key"
                        + " should be a dynamic parameter attribute but "
                        + "found a constant attribute "
                        + attributeExpressionExecutors[1].getClass().getCanonicalName());
            }
        } else {
            throw new SiddhiAppValidationException("Tracegroup window should have two parameters "
                    + "(<int|long|time> idleTimeGap, <String> traceKey"
                    + "but found " + attributeExpressionExecutors.length + " input attributes");

        }
        return new ArrayList<>();
    }

    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor processor, StreamEventCloner
            streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        boolean isTimerEvent = false;
        synchronized (this) {
            while (streamEventChunk.hasNext()) {
                StreamEvent streamEvent = streamEventChunk.next();
                long eventTimestamp = streamEvent.getTimestamp();
                long maxTimestamp = eventTimestamp + idleTimeGap;

                if (streamEvent.getType() == StreamEvent.Type.CURRENT) {
                    String key = (String) tracekeyExecutor.execute(streamEvent);
                    //get the trace configuration based on trace key
                    //if the map doesn't contain key, then a new traceGroup
                    //object needs to be created.
                    if ((traceGroup = traceGroupMap.get(key)) == null) {
                        traceGroup = new TraceGroup(key, eventTimestamp);
                    }
                    traceGroupMap.put(key, traceGroup);

                    StreamEvent clonedStreamEvent = streamEventCloner.copyStreamEvent(streamEvent);
                    clonedStreamEvent.setType(StreamEvent.Type.EXPIRED);

                    //if current trace contains events
                    synchronized (key.intern()) {
                        if (traceGroup.isEmpty() || eventTimestamp >= traceGroup.getStartTimestamp()) {
                            traceGroup.add(clonedStreamEvent);
                            traceGroup.setEndTimestamp(maxTimestamp);
                            scheduler.notifyAt(maxTimestamp);
                        } else {
                            addLateEvent(streamEventChunk, eventTimestamp, clonedStreamEvent);
                        }
                    }
                } else if (streamEvent.getType() == ComplexEvent.Type.TIMER) {
                    isTimerEvent = true;
                    currentTraceTimeout(eventTimestamp);
                }
            }
        }
        if (!isTimerEvent) {
            nextProcessor.process(streamEventChunk);
        }
        if (expiredEventChunk != null && expiredEventChunk.getFirst() != null) {
            nextProcessor.process(expiredEventChunk);
            expiredEventChunk.clear();
        }
    }

    /**
     * Handles when the late event arrives to the system.
     */
    private void addLateEvent(ComplexEventChunk<StreamEvent> streamEventChunk,
                              long eventTimestamp, StreamEvent streamEvent) {
        //check the late event belongs to the same trace
        if (eventTimestamp >= (traceGroup.getStartTimestamp() - idleTimeGap)) {
            traceGroup.add(streamEvent);
            traceGroup.setStartTimestamp(eventTimestamp);
        } else {
            streamEventChunk.remove();
            log.info("The event, " + streamEvent + " is late and it's tracegroup window has been timeout");
        }
    }

    /**
     * Checks all the traces and get the expired trace.
     */
    private void currentTraceTimeout(long eventTimestamp) {
        Collection<TraceGroup> traceGroupList = traceGroupMap.values();
        TreeSet<TraceGroup> currentEndTimestamps = new TreeSet<>(traceGroupList);
        for (TraceGroup aTraceGroup : currentEndTimestamps) {
            long traceEndTime = aTraceGroup.getEndTimestamp();
            if (eventTimestamp >= traceEndTime) {
                synchronized (aTraceGroup.getKey().intern()) {
                    TraceGroup currentTraceGroup = traceGroupMap.get(aTraceGroup.getKey());
                    ComplexEventChunk<StreamEvent> events = currentTraceGroup.getCurrentTraceGroup();
                    if (events.getFirst() != null) {
                        expiredEventChunk.add(events.getFirst());
                        currentTraceGroup.clear();
                    }
                    traceGroupMap.remove(aTraceGroup.getKey());
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
        state.put("traceGroupMap", traceGroupMap);
        state.put("traceGroup", traceGroup);
        state.put("expiredEventChunk", expiredEventChunk);
        return state;
    }

    @Override
    public synchronized void restoreState(Map<String, Object> state) {
        traceGroupMap = (ConcurrentHashMap<String, TraceGroup>) state.get("traceGroupMap");
        traceGroup = (TraceGroup) state.get("traceGroup");
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
