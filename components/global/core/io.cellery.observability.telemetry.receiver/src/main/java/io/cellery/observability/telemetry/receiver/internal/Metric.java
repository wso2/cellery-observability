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

package io.cellery.observability.telemetry.receiver.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a POJO.
 */
public class Metric {
    private String apiOperation;
    private String apiProtocol;
    private String apiService;
    private String apiVersion;
    private Boolean checkCasheHit;
    private Long checkErrorCode;
    private String checkErrorMessage;
    private String connectionEvent;
    private String connectionID;
    private Boolean connectionMTLS;
    private Long connectionReceivedBytes;
    private Long connectionReceivedBytesTotal;
    private String connectionRequestedServerName;
    private Long connectionSentBytes;
    private Long connectionSentBytesTotal;
    private String contextProtocol;
    private String contextReporterKind;
    private Boolean contextReporterLocal;
    private String contextReporterUID;
    private String destinationContainerName;
    private String destinationName;
    private String destinationNamespace;
    private String destinationOwner;
    private Long destinationPort;
    private String destinationPrincipal;
    private String destinationServiceHost;
    private String destinationServiceName;
    private String destinationServiceNamespace;
    private String destinationServiceUID;
    private String destinationUID;
    private String destinationWorkload;
    private String destinationWorkloadNamespace;
    private String destinationWorkloadUID;
    private Boolean quotaCacheHit;
    private String requestApiKey;
    private String requestAuthAudiences;
    private String requestAuthPresenter;
    private String requestAuthPrincipal;
    private String requestHost;
    private String requestID;
    private String requestMethod;
    private String requestPath;
    private String requestReason;
    private String requestReferer;
    private String requestScheme;
    private Long requestSize;
    private Long requestTotalSize;
    private String requestUserAgent;
    private Long responseCode;
    private Integer responseDurationNanoSec;
    private String responseGrpcMessage;
    private String responseGrpcStatus;
    private Long responseSize;
    private Long responseTotalSize;
    private String sourceLabelsApp;
    private String sourceLabelsCell;
    private String sourceLabelsCellGateway;
    private String sourceLabelsCellSlash;
    private String sourceLabelsPodTemplateHash;
    private String sourceLabelsSvc;
    private String sourceLabelsVersion;
    private String sourceName;
    private String sourceNamespace;
    private String sourceOwner;
    private String sourcePrincipal;
    private String sourceUID;
    private String sourceWorkloadName;
    private String sourceWorkloadNamespace;
    private String sourceWorkloadUID;

    public String getApiOperation() {
        return apiOperation;
    }

    public void setApiOperation(String apiOperation) {
        this.apiOperation = apiOperation;
    }

    public String getApiProtocol() {
        return apiProtocol;
    }

    public void setApiProtocol(String apiProtocol) {
        this.apiProtocol = apiProtocol;
    }

    public String getApiService() {
        return apiService;
    }

    public void setApiService(String apiService) {
        this.apiService = apiService;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Boolean getCheckCasheHit() {
        return checkCasheHit;
    }

    public void setCheckCasheHit(Boolean checkCasheHit) {
        this.checkCasheHit = checkCasheHit;
    }

    public Long getCheckErrorCode() {
        return checkErrorCode;
    }

    public void setCheckErrorCode(Long checkErrorCode) {
        this.checkErrorCode = checkErrorCode;
    }

    public String getCheckErrorMessage() {
        return checkErrorMessage;
    }

    public void setCheckErrorMessage(String checkErrorMessage) {
        this.checkErrorMessage = checkErrorMessage;
    }

    public String getConnectionEvent() {
        return connectionEvent;
    }

    public void setConnectionEvent(String connectionEvent) {
        this.connectionEvent = connectionEvent;
    }

    public String getConnectionID() {
        return connectionID;
    }

    public void setConnectionID(String connectionID) {
        this.connectionID = connectionID;
    }

    public Boolean getConnectionMTLS() {
        return connectionMTLS;
    }

    public void setConnectionMTLS(Boolean connectionMTLS) {
        this.connectionMTLS = connectionMTLS;
    }

    public Long getConnectionReceivedBytes() {
        return connectionReceivedBytes;
    }

    public void setConnectionReceivedBytes(Long connectionReceivedBytes) {
        this.connectionReceivedBytes = connectionReceivedBytes;
    }

    public Long getConnectionReceivedBytesTotal() {
        return connectionReceivedBytesTotal;
    }

    public void setConnectionReceivedBytesTotal(Long connectionReceivedBytesTotal) {
        this.connectionReceivedBytesTotal = connectionReceivedBytesTotal;
    }

    public String getConnectionRequestedServerName() {
        return connectionRequestedServerName;
    }

    public void setConnectionRequestedServerName(String connectionRequestedServerName) {
        this.connectionRequestedServerName = connectionRequestedServerName;
    }

    public Long getConnectionSentBytes() {
        return connectionSentBytes;
    }

    public void setConnectionSentBytes(Long connectionSentBytes) {
        this.connectionSentBytes = connectionSentBytes;
    }

    public Long getConnectionSentBytesTotal() {
        return connectionSentBytesTotal;
    }

    public void setConnectionSentBytesTotal(Long connectionSentBytesTotal) {
        this.connectionSentBytesTotal = connectionSentBytesTotal;
    }

    public String getContextProtocol() {
        return contextProtocol;
    }

    public void setContextProtocol(String contextProtocol) {
        this.contextProtocol = contextProtocol;
    }

    public String getContextReporterKind() {
        return contextReporterKind;
    }

    public void setContextReporterKind(String contextReporterKind) {
        this.contextReporterKind = contextReporterKind;
    }

    public Boolean getContextReporterLocal() {
        return contextReporterLocal;
    }

    public void setContextReporterLocal(Boolean contextReporterLocal) {
        this.contextReporterLocal = contextReporterLocal;
    }

    public String getContextReporterUID() {
        return contextReporterUID;
    }

    public void setContextReporterUID(String contextReporterUID) {
        this.contextReporterUID = contextReporterUID;
    }

    public String getDestinationContainerName() {
        return destinationContainerName;
    }

    public void setDestinationContainerName(String destinationContainerName) {
        this.destinationContainerName = destinationContainerName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public String getDestinationNamespace() {
        return destinationNamespace;
    }

    public void setDestinationNamespace(String destinationNamespace) {
        this.destinationNamespace = destinationNamespace;
    }

    public String getDestinationOwner() {
        return destinationOwner;
    }

    public void setDestinationOwner(String destinationOwner) {
        this.destinationOwner = destinationOwner;
    }

    public Long getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Long destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getDestinationPrincipal() {
        return destinationPrincipal;
    }

    public void setDestinationPrincipal(String destinationPrincipal) {
        this.destinationPrincipal = destinationPrincipal;
    }

    public String getDestinationServiceHost() {
        return destinationServiceHost;
    }

    public void setDestinationServiceHost(String destinationServiceHost) {
        this.destinationServiceHost = destinationServiceHost;
    }

    public String getDestinationServiceName() {
        return destinationServiceName;
    }

    public void setDestinationServiceName(String destinationServiceName) {
        this.destinationServiceName = destinationServiceName;
    }

    public String getDestinationServiceNamespace() {
        return destinationServiceNamespace;
    }

    public void setDestinationServiceNamespace(String destinationServiceNamespace) {
        this.destinationServiceNamespace = destinationServiceNamespace;
    }

    public String getDestinationServiceUID() {
        return destinationServiceUID;
    }

    public void setDestinationServiceUID(String destinationServiceUID) {
        this.destinationServiceUID = destinationServiceUID;
    }

    public String getDestinationUID() {
        return destinationUID;
    }

    public void setDestinationUID(String destinationUID) {
        this.destinationUID = destinationUID;
    }

    public String getDestinationWorkload() {
        return destinationWorkload;
    }

    public void setDestinationWorkload(String destinationWorkload) {
        this.destinationWorkload = destinationWorkload;
    }

    public String getDestinationWorkloadNamespace() {
        return destinationWorkloadNamespace;
    }

    public void setDestinationWorkloadNamespace(String destinationWorkloadNamespace) {
        this.destinationWorkloadNamespace = destinationWorkloadNamespace;
    }

    public String getDestinationWorkloadUID() {
        return destinationWorkloadUID;
    }

    public void setDestinationWorkloadUID(String destinationWorkloadUID) {
        this.destinationWorkloadUID = destinationWorkloadUID;
    }

    public Boolean getQuotaCacheHit() {
        return quotaCacheHit;
    }

    public void setQuotaCacheHit(Boolean quotaCacheHit) {
        this.quotaCacheHit = quotaCacheHit;
    }

    public String getRequestApiKey() {
        return requestApiKey;
    }

    public void setRequestApiKey(String requestApiKey) {
        this.requestApiKey = requestApiKey;
    }

    public String getRequestAuthAudiences() {
        return requestAuthAudiences;
    }

    public void setRequestAuthAudiences(String requestAuthAudiences) {
        this.requestAuthAudiences = requestAuthAudiences;
    }

    public String getRequestAuthPresenter() {
        return requestAuthPresenter;
    }

    public void setRequestAuthPresenter(String requestAuthPresenter) {
        this.requestAuthPresenter = requestAuthPresenter;
    }

    public String getRequestAuthPrincipal() {
        return requestAuthPrincipal;
    }

    public void setRequestAuthPrincipal(String requestAuthPrincipal) {
        this.requestAuthPrincipal = requestAuthPrincipal;
    }

    public String getRequestHost() {
        return requestHost;
    }

    public void setRequestHost(String requestHost) {
        this.requestHost = requestHost;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestReason() {
        return requestReason;
    }

    public void setRequestReason(String requestReason) {
        this.requestReason = requestReason;
    }

    public String getRequestReferer() {
        return requestReferer;
    }

    public void setRequestReferer(String requestReferer) {
        this.requestReferer = requestReferer;
    }

    public String getRequestScheme() {
        return requestScheme;
    }

    public void setRequestScheme(String requestScheme) {
        this.requestScheme = requestScheme;
    }

    public Long getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(Long requestSize) {
        this.requestSize = requestSize;
    }

    public Long getRequestTotalSize() {
        return requestTotalSize;
    }

    public void setRequestTotalSize(Long requestTotalSize) {
        this.requestTotalSize = requestTotalSize;
    }

    public String getRequestUserAgent() {
        return requestUserAgent;
    }

    public void setRequestUserAgent(String requestUserAgent) {
        this.requestUserAgent = requestUserAgent;
    }

    public Long getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Long responseCode) {
        this.responseCode = responseCode;
    }

    public Integer getResponseDurationNanoSec() {
        return responseDurationNanoSec;
    }

    public void setResponseDurationNanoSec(Integer responseDurationNanoSec) {
        this.responseDurationNanoSec = responseDurationNanoSec;
    }

    public String getResponseGrpcMessage() {
        return responseGrpcMessage;
    }

    public void setResponseGrpcMessage(String responseGrpcMessage) {
        this.responseGrpcMessage = responseGrpcMessage;
    }

    public String getResponseGrpcStatus() {
        return responseGrpcStatus;
    }

    public void setResponseGrpcStatus(String responseGrpcStatus) {
        this.responseGrpcStatus = responseGrpcStatus;
    }

    public Long getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(Long responseSize) {
        this.responseSize = responseSize;
    }

    public Long getResponseTotalSize() {
        return responseTotalSize;
    }

    public void setResponseTotalSize(Long responseTotalSize) {
        this.responseTotalSize = responseTotalSize;
    }

    public String getSourceLabelsApp() {
        return sourceLabelsApp;
    }

    public void setSourceLabelsApp(String sourceLabelsApp) {
        this.sourceLabelsApp = sourceLabelsApp;
    }

    public String getSourceLabelsCell() {
        return sourceLabelsCell;
    }

    public void setSourceLabelsCell(String sourceLabelsCell) {
        this.sourceLabelsCell = sourceLabelsCell;
    }

    public String getSourceLabelsCellGateway() {
        return sourceLabelsCellGateway;
    }

    public void setSourceLabelsCellGateway(String sourceLabelsCellGateway) {
        this.sourceLabelsCellGateway = sourceLabelsCellGateway;
    }

    public String getSourceLabelsCellSlash() {
        return sourceLabelsCellSlash;
    }

    public void setSourceLabelsCellSlash(String sourceLabelsCellSlash) {
        this.sourceLabelsCellSlash = sourceLabelsCellSlash;
    }

    public String getSourceLabelsPodTemplateHash() {
        return sourceLabelsPodTemplateHash;
    }

    public void setSourceLabelsPodTemplateHash(String sourceLabelsPodTemplateHash) {
        this.sourceLabelsPodTemplateHash = sourceLabelsPodTemplateHash;
    }

    public String getSourceLabelsSvc() {
        return sourceLabelsSvc;
    }

    public void setSourceLabelsSvc(String sourceLabelsSvc) {
        this.sourceLabelsSvc = sourceLabelsSvc;
    }

    public String getSourceLabelsVersion() {
        return sourceLabelsVersion;
    }

    public void setSourceLabelsVersion(String sourceLabelsVersion) {
        this.sourceLabelsVersion = sourceLabelsVersion;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceNamespace() {
        return sourceNamespace;
    }

    public void setSourceNamespace(String sourceNamespace) {
        this.sourceNamespace = sourceNamespace;
    }

    public String getSourceOwner() {
        return sourceOwner;
    }

    public void setSourceOwner(String sourceOwner) {
        this.sourceOwner = sourceOwner;
    }

    public String getSourcePrincipal() {
        return sourcePrincipal;
    }

    public void setSourcePrincipal(String sourcePrincipal) {
        this.sourcePrincipal = sourcePrincipal;
    }

    public String getSourceUID() {
        return sourceUID;
    }

    public void setSourceUID(String sourceUID) {
        this.sourceUID = sourceUID;
    }

    public String getSourceWorkloadName() {
        return sourceWorkloadName;
    }

    public void setSourceWorkloadName(String sourceWorkloadName) {
        this.sourceWorkloadName = sourceWorkloadName;
    }

    public String getSourceWorkloadNamespace() {
        return sourceWorkloadNamespace;
    }

    public void setSourceWorkloadNamespace(String sourceWorkloadNamespace) {
        this.sourceWorkloadNamespace = sourceWorkloadNamespace;
    }

    public String getSourceWorkloadUID() {
        return sourceWorkloadUID;
    }

    public void setSourceWorkloadUID(String sourceWorkloadUID) {
        this.sourceWorkloadUID = sourceWorkloadUID;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("sourceUID", this.sourceUID);
        map.put("sourceName", this.sourceName);
        map.put("sourceNamespace", this.sourceNamespace);
        map.put("sourcePrincipal", this.sourcePrincipal);
        map.put("sourceOwner", this.sourceOwner);
        map.put("sourceWorkloadUID", this.sourceWorkloadUID);
        map.put("sourceWorkloadName", this.sourceWorkloadName);
        map.put("sourceWorkloadNamespace", this.sourceWorkloadNamespace);
        map.put("destinationUID", this.destinationUID);
        map.put("destinationPort", this.destinationPort);
        map.put("destinationName", this.destinationName);
        map.put("destinationNamespace", this.destinationNamespace);
        map.put("destinationPrincipal", this.destinationPrincipal);
        map.put("destinationOwner", this.destinationOwner);
        map.put("destinationWorkloadUID", this.destinationWorkloadUID);
        map.put("destinationWorkloadNamespace", this.destinationWorkloadNamespace);
        map.put("destinationContainerName", this.destinationContainerName);
        map.put("destinationServiceHost", this.destinationServiceHost);
        map.put("destinationServiceUID", this.destinationServiceUID);
        map.put("destinationServiceName", this.destinationServiceName);
        map.put("destinationServiceNamespace", this.destinationServiceNamespace);
        map.put("requestID", this.requestID);
        map.put("requestPath", this.requestPath);
        map.put("requestHost", this.requestHost);
        map.put("requestMethod", this.requestMethod);
        map.put("requestReason", this.requestReason);
        map.put("requestReferer", this.requestReferer);
        map.put("requestScheme", this.requestScheme);
        map.put("requestSize", this.requestSize);
        map.put("requestTotalSize", this.requestTotalSize);
        map.put("requestUserAgent", this.requestUserAgent);
        map.put("responseSize", this.responseSize);
        map.put("responseDurationNanoSec", this.responseDurationNanoSec);
        map.put("responseCode", this.responseCode);
        map.put("connectionID", this.connectionID);
        map.put("connectionEvent", this.connectionEvent);
        map.put("connectionReceivedBytes", this.connectionReceivedBytes);
        map.put("connectionReceivedBytesTotal", this.connectionReceivedBytesTotal);
        map.put("connectionSentBytes", this.connectionSentBytes);
        map.put("connectionSentBytesTotal", this.connectionSentBytesTotal);
        map.put("connectionMTLS", this.connectionMTLS);
        map.put("connectionRequestedServerName", this.connectionRequestedServerName);
        map.put("contextProtocol", this.contextProtocol);
        map.put("contextReporterKind", this.contextReporterKind);
        map.put("contextReporterUID", this.contextReporterUID);
        map.put("apiService", this.apiService);
        map.put("apiVersion", this.apiVersion);
        map.put("apiOperation", this.apiOperation);
        map.put("apiProtocol", this.apiProtocol);
        map.put("requestAuthPrincipal", this.requestAuthPrincipal);
        map.put("requestAuthAudiences", this.requestAuthAudiences);
        map.put("requestAuthPresenter", this.requestAuthPresenter);
        map.put("checkErrorCode", this.checkErrorCode);
        map.put("checkErrorMessage", this.checkErrorMessage);
        map.put("quotaCacheHit", this.quotaCacheHit);
        map.put("contextReporterLocal", this.contextReporterLocal);
        map.put("responseTotalSize", this.responseTotalSize);
        return map;
    }
}
