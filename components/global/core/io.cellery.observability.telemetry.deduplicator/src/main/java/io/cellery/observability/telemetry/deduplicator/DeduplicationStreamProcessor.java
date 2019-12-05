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

package io.cellery.observability.telemetry.deduplicator;

import io.cellery.observability.telemetry.deduplicator.internal.Constants;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.executor.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.SchedulingProcessor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.core.util.Scheduler;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The class representing deduplication stream processor implementation.
 */
@Extension(
        name = "deduplicate",
        namespace = "telemetry",
        description = "This is a sliding time window that holds the latest unique events"
                + " that arrived during the last window time period. The unique events are determined"
                + " based on some attributes (spanId, parentSpanId, etc..)."
                + " The window is updated with each event arrival and expiry."
                + " When a new duplicate event that arrives within a window time period,"
                + " the previous event and the new event will be merged into one"
                + " and processed forward",
        examples = {
                @Example(
                        syntax = "from TelemetryStream#telemetry:deduplicate(60 sec, requestId, traceId, spanId, "
                                + "parentSpanId, sourceNamespace, sourceInstance, sourceComponent, "
                                + "destinationNamespace, destinationInstance, destinationComponent, requestSizeBytes, "
                                + "responseDuration, responseSizeBytes)"
                                + "insert into ProcessedTelemetryStream;",
                        description = "This window will hold every event from Telemetry stream for 60 seconds and "
                                + "remove duplicate events received within the time interval."
                )
        }
)
public class DeduplicationStreamProcessor extends StreamProcessor implements SchedulingProcessor {
    private long windowTimeMilliSeconds;
    private ComplexEventChunk<StreamEvent> expiredEventChunk;
    private Scheduler scheduler;
    private SiddhiAppContext siddhiAppContext;
    private volatile long lastTimestamp = 0;

    private ExpressionExecutor traceIdExecutor;
    private ExpressionExecutor spanIdExecutor;
    private ExpressionExecutor parentSpanIdExecutor;
    private ExpressionExecutor sourceNamespaceExecutor;
    private ExpressionExecutor sourceInstanceExecutor;
    private ExpressionExecutor sourceComponentExecutor;
    private ExpressionExecutor destinationNamespaceExecutor;
    private ExpressionExecutor destinationInstanceExecutor;
    private ExpressionExecutor destinationComponentExecutor;
    private ExpressionExecutor requestSizeExecutor;
    private ExpressionExecutor responseDurationExecutor;
    private ExpressionExecutor responseSizeExecutor;

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
            StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {
        synchronized (this) {
            while (streamEventChunk.hasNext()) {
                StreamEvent streamEvent = streamEventChunk.next();
                long currentTime = siddhiAppContext.getTimestampGenerator().currentTime();
                StreamEvent clonedEvent = null;
                if (streamEvent.getType() == StreamEvent.Type.CURRENT) {
                    clonedEvent = streamEventCloner.copyStreamEvent(streamEvent);
                    clonedEvent.setType(StreamEvent.Type.EXPIRED);
                }
                expiredEventChunk.reset();
                boolean isDuplicationEventFound = false;
                while (expiredEventChunk.hasNext()) {
                    StreamEvent expiredEvent = expiredEventChunk.next();
                    long timeDiff = expiredEvent.getTimestamp() + windowTimeMilliSeconds - currentTime;
                    if (streamEvent.getType() == StreamEvent.Type.CURRENT) {
                        if (isDuplicate(expiredEvent, clonedEvent)) {
                            isDuplicationEventFound = true;
                            this.expiredEventChunk.remove();
                            clonedEvent.setTimestamp(currentTime);
                            streamEventChunk.insertBeforeCurrent(mergeEvents(expiredEvent, clonedEvent));
                        }
                    } else if (timeDiff <= 0) {
                        expiredEventChunk.remove();
                        expiredEvent.setTimestamp(currentTime);
                        streamEventChunk.insertBeforeCurrent(mergeEvents(null, expiredEvent));
                    } else {
                        break;
                    }
                }
                if (!isDuplicationEventFound && streamEvent.getType() == StreamEvent.Type.CURRENT) {
                    this.expiredEventChunk.add(clonedEvent);
                    if (lastTimestamp < clonedEvent.getTimestamp()) {
                        if (scheduler != null) {
                            scheduler.notifyAt(clonedEvent.getTimestamp() + windowTimeMilliSeconds);
                            lastTimestamp = clonedEvent.getTimestamp();
                        }
                    }
                }
                expiredEventChunk.reset();
            }
        }
        nextProcessor.process(streamEventChunk);
    }

    private boolean isDuplicate(StreamEvent oldEvent, StreamEvent currentEvent) {
        String oldEventTraceId = (String) traceIdExecutor.execute(oldEvent);
        String oldEventSpanId = (String) spanIdExecutor.execute(oldEvent);
        String oldEventParentSpanId = (String) parentSpanIdExecutor.execute(oldEvent);
        String oldEventSourceNamespace = (String) sourceNamespaceExecutor.execute(oldEvent);
        String oldEventSourceInstance = (String) sourceInstanceExecutor.execute(oldEvent);
        String oldEventSourceComponent = (String) sourceComponentExecutor.execute(oldEvent);
        String oldEventDestinationNamespace = (String) destinationNamespaceExecutor.execute(oldEvent);
        String oldEventDestinationInstance = (String) destinationInstanceExecutor.execute(oldEvent);
        String oldEventDestinationComponent = (String) destinationComponentExecutor.execute(oldEvent);

        String currentEventTraceId = (String) traceIdExecutor.execute(currentEvent);
        String currentEventSpanId = (String) spanIdExecutor.execute(currentEvent);
        String currentEventParentSpanId = (String) parentSpanIdExecutor.execute(currentEvent);
        String currentEventSourceNamespace = (String) sourceNamespaceExecutor.execute(currentEvent);
        String currentEventSourceInstance = (String) sourceInstanceExecutor.execute(currentEvent);
        String currentEventSourceComponent = (String) sourceComponentExecutor.execute(currentEvent);
        String currentEventDestinationNamespace = (String) destinationNamespaceExecutor.execute(currentEvent);
        String currentEventDestinationInstance = (String) destinationInstanceExecutor.execute(currentEvent);
        String currentEventDestinationComponent = (String) destinationComponentExecutor.execute(currentEvent);

        return Objects.equals(currentEventSourceNamespace, oldEventSourceNamespace)
                && Objects.equals(currentEventSourceInstance, oldEventSourceInstance)
                && Objects.equals(currentEventSourceComponent, oldEventSourceComponent)
                && Objects.equals(currentEventDestinationNamespace, oldEventDestinationNamespace)
                && Objects.equals(currentEventDestinationInstance, oldEventDestinationInstance)
                && Objects.equals(currentEventDestinationComponent, oldEventDestinationComponent)
                && Objects.equals(currentEventTraceId, oldEventTraceId)
                && (Objects.equals(currentEventParentSpanId, oldEventSpanId)
                || Objects.equals(currentEventSpanId, oldEventParentSpanId));
    }

    private StreamEvent mergeEvents(StreamEvent oldEvent, StreamEvent currentEvent) {
        Long maxRequestSizeBytes = (Long) requestSizeExecutor.execute(currentEvent);
        Long maxResponseDuration = (Long) responseDurationExecutor.execute(currentEvent);
        Long maxResponseSizeBytes = (Long) responseSizeExecutor.execute(currentEvent);

        if (oldEvent != null) {
            Long oldEventRequestSizeBytes = (Long) requestSizeExecutor.execute(oldEvent);
            Long oldEventResponseDuration = (Long) responseDurationExecutor.execute(oldEvent);
            Long oldEventResponseSizeBytes = (Long) responseSizeExecutor.execute(oldEvent);
            maxRequestSizeBytes = Long.max(oldEventRequestSizeBytes, maxRequestSizeBytes);
            maxResponseDuration = Long.max(oldEventResponseDuration, maxResponseDuration);
            maxResponseSizeBytes = Long.max(oldEventResponseSizeBytes, maxResponseSizeBytes);
        }

        Object[] newData = new Object[3];
        newData[0] = maxRequestSizeBytes;
        newData[1] = maxResponseDuration;
        newData[2] = maxResponseSizeBytes;
        complexEventPopulater.populateComplexEvent(currentEvent, newData);
        return currentEvent;
    }

    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition,
            ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
            SiddhiAppContext siddhiAppContext) {
        this.siddhiAppContext = siddhiAppContext;
        this.expiredEventChunk = new ComplexEventChunk<>(false);
        if (attributeExpressionExecutors.length != Constants.NUM_OF_PARAMETERS) {
            throw new SiddhiAppValidationException(Constants.NUM_OF_PARAMETERS + " arguments are required, but "
                    + attributeExpressionExecutors.length + " given");
        } else {
            if (attributeExpressionExecutors[Constants.TIME_INTERVAL_EXECUTOR_INDEX]
                    instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[Constants.TIME_INTERVAL_EXECUTOR_INDEX].getReturnType()
                        == Attribute.Type.INT) {
                    windowTimeMilliSeconds = (Integer) ((ConstantExpressionExecutor)
                            attributeExpressionExecutors[Constants.TIME_INTERVAL_EXECUTOR_INDEX])
                            .getValue();

                } else if (attributeExpressionExecutors[Constants.TIME_INTERVAL_EXECUTOR_INDEX].getReturnType()
                        == Attribute.Type.LONG) {
                    windowTimeMilliSeconds = (Long) ((ConstantExpressionExecutor)
                            attributeExpressionExecutors[Constants.TIME_INTERVAL_EXECUTOR_INDEX])
                            .getValue();
                } else {
                    throw new SiddhiAppValidationException(
                            "UniqueTime window's parameter time should be either" + " int or long, but found "
                                    + attributeExpressionExecutors[Constants.TIME_INTERVAL_EXECUTOR_INDEX]
                                    .getReturnType());
                }
            } else {
                throw new SiddhiAppValidationException(
                        "UniqueTime window should have constant for time parameter but " + "found a dynamic attribute "
                                + attributeExpressionExecutors[Constants.TIME_INTERVAL_EXECUTOR_INDEX]
                                .getClass().getCanonicalName());
            }

            if (attributeExpressionExecutors[Constants.TRACE_ID_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                traceIdExecutor = attributeExpressionExecutors[Constants.TRACE_ID_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the traceId "
                        + "field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.TRACE_ID_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.SPAN_ID_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                spanIdExecutor = attributeExpressionExecutors[Constants.SPAN_ID_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the spanId "
                        + "field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.SPAN_ID_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.PARENT_SPAN_ID_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                parentSpanIdExecutor = attributeExpressionExecutors[Constants.PARENT_SPAN_ID_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the parentId "
                        + "field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.PARENT_SPAN_ID_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.SOURCE_NAMESPACE_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                sourceNamespaceExecutor = attributeExpressionExecutors[Constants.SOURCE_NAMESPACE_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the"
                        + " sourceNamespace field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.SOURCE_NAMESPACE_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.SOURCE_INSTANCE_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                sourceInstanceExecutor = attributeExpressionExecutors[Constants.SOURCE_INSTANCE_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the"
                        + " sourceInstance field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.SOURCE_INSTANCE_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.SOURCE_COMPONENT_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                sourceComponentExecutor = attributeExpressionExecutors[Constants.SOURCE_COMPONENT_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the "
                        + " sourceComponent field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.SOURCE_COMPONENT_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.DESTINATION_NAMESPACE_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                destinationNamespaceExecutor
                        = attributeExpressionExecutors[Constants.DESTINATION_NAMESPACE_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the "
                        + "destinationNamespace field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.DESTINATION_NAMESPACE_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.DESTINATION_INSTANCE_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                destinationInstanceExecutor
                        = attributeExpressionExecutors[Constants.DESTINATION_INSTANCE_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the "
                        + "destinationInstance field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.DESTINATION_INSTANCE_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.DESTINATION_COMPONENT_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.STRING) {
                destinationComponentExecutor
                        = attributeExpressionExecutors[Constants.DESTINATION_COMPONENT_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with String return type for the "
                        + "destinationComponent field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.DESTINATION_COMPONENT_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.REQUEST_SIZE_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.LONG) {
                requestSizeExecutor = attributeExpressionExecutors[Constants.REQUEST_SIZE_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with long return type for the " +
                        "requestSizeBytes field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.REQUEST_SIZE_EXECUTOR_INDEX].getReturnType());
            }

            if (attributeExpressionExecutors[Constants.RESPONSE_DURATION_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.LONG) {
                responseDurationExecutor
                        = attributeExpressionExecutors[Constants.RESPONSE_DURATION_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with long return type for the " +
                        "responseDuration field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.RESPONSE_DURATION_EXECUTOR_INDEX]
                        .getReturnType());
            }

            if (attributeExpressionExecutors[Constants.RESPONSE_SIZE_EXECUTOR_INDEX].getReturnType()
                    == Attribute.Type.LONG) {
                responseSizeExecutor = attributeExpressionExecutors[Constants.RESPONSE_SIZE_EXECUTOR_INDEX];
            } else {
                throw new SiddhiAppValidationException("Expected a field with long return type for the " +
                        "responseSizeBytes field, but found a field with return type - "
                        + attributeExpressionExecutors[Constants.RESPONSE_SIZE_EXECUTOR_INDEX].getReturnType());
            }
        }
        List<Attribute> appendedAttributes = new ArrayList<>(3);
        appendedAttributes.add(new Attribute(Constants.MAX_REQUEST_SIZE, Attribute.Type.LONG));
        appendedAttributes.add(new Attribute(Constants.MAX_RESPONSE_DURATION, Attribute.Type.LONG));
        appendedAttributes.add(new Attribute(Constants.MAX_RESPONSE_SIZE, Attribute.Type.LONG));
        return appendedAttributes;
    }

    @Override
    public synchronized Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public synchronized void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
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
    public Map<String, Object> currentState() {
        Map<String, Object> state = new HashMap<>();
        state.put("expiredEventchunck", expiredEventChunk.getFirst());
        return state;
    }

    @Override
    public void restoreState(Map<String, Object> map) {
        expiredEventChunk.clear();
        expiredEventChunk.add((StreamEvent) map.get("expiredEventchunck"));
    }
}
