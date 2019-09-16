/*
 * Copyright (c) ${year} WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 *  under the License.
 *
 */

package io.cellery.observability.telemetry.receiver.internal;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a POJO.
 */
public class Metric {
    @SerializedName("apiOperation") @Expose private String apiOperation;
    @SerializedName("apiProtocol") @Expose private String apiProtocol;
    @SerializedName("apiService") @Expose private String apiService;
    @SerializedName("apiVersion") @Expose private String apiVersion;
    @SerializedName("checkCasheHit") @Expose private Boolean checkCasheHit;
    @SerializedName("checkErrorCode") @Expose private Integer checkErrorCode;
    @SerializedName("checkErrorMessage") @Expose private String checkErrorMessage;
    @SerializedName("connectionEvent") @Expose private String connectionEvent;
    @SerializedName("connectionID") @Expose private String connectionID;
    @SerializedName("connectionMTLS") @Expose private Boolean connectionMTLS;
    @SerializedName("connectionReceivedBytes") @Expose private Integer connectionReceivedBytes;
    @SerializedName("connectionReceivedBytesTotal") @Expose private Integer connectionReceivedBytesTotal;
    @SerializedName("connectionRequestedServerName") @Expose private String connectionRequestedServerName;
    @SerializedName("connectionSentBytes") @Expose private Integer connectionSentBytes;
    @SerializedName("connectionSentBytesTotal") @Expose private Integer connectionSentBytesTotal;
    @SerializedName("contextProtocol") @Expose private String contextProtocol;
    @SerializedName("contextReporterKind") @Expose private String contextReporterKind;
    @SerializedName("contextReporterLocal") @Expose private Boolean contextReporterLocal;
    @SerializedName("contextReporterUID") @Expose private String contextReporterUID;
    @SerializedName("destinationContainerName") @Expose private String destinationContainerName;
    @SerializedName("destinationName") @Expose
    private String destinationName;
    @SerializedName("destinationNamespace") @Expose private String destinationNamespace;
    @SerializedName("destinationOwner") @Expose
    private String destinationOwner;
    @SerializedName("destinationPort") @Expose private Integer destinationPort;
    @SerializedName("destinationPrincipal") @Expose private String destinationPrincipal;
    @SerializedName("destinationServiceHost") @Expose
    private String destinationServiceHost;
    @SerializedName("destinationServiceName") @Expose
    private String destinationServiceName;
    @SerializedName("destinationServiceNamespace") @Expose private String destinationServiceNamespace;
    @SerializedName("destinationServiceUID") @Expose private String destinationServiceUID;
    @SerializedName("destinationUID") @Expose private String destinationUID;
    @SerializedName("destinationWorkload") @Expose private String destinationWorkload;
    @SerializedName("destinationWorkloadNamespace") @Expose private String destinationWorkloadNamespace;
    @SerializedName("destinationWorkloadUID") @Expose private String destinationWorkloadUID;
    @SerializedName("quotaCacheHit") @Expose private Boolean quotaCacheHit;
    @SerializedName("requestApiKey") @Expose private String requestApiKey;
    @SerializedName("requestAuthAudiences") @Expose private String requestAuthAudiences;
    @SerializedName("requestAuthPresenter") @Expose private String requestAuthPresenter;
    @SerializedName("requestAuthPrincipal") @Expose private String requestAuthPrincipal;
    @SerializedName("requestHost") @Expose
    private String requestHost;
    @SerializedName("requestID") @Expose private String requestID;
    @SerializedName("requestMethod") @Expose private String requestMethod;
    @SerializedName("requestPath") @Expose private String requestPath;
    @SerializedName("requestReason") @Expose private String requestReason;
    @SerializedName("requestReferer") @Expose private String requestReferer;
    @SerializedName("requestScheme") @Expose private String requestScheme;
    @SerializedName("requestSize") @Expose private Integer requestSize;
    @SerializedName("requestTotalSize") @Expose private Integer requestTotalSize;
    @SerializedName("requestUserAgent") @Expose
    private String requestUserAgent;
    @SerializedName("responseCode") @Expose private Integer responseCode;
    @SerializedName("responseDurationNanoSec") @Expose private Integer responseDurationNanoSec;
    @SerializedName("responseGrpcMessage") @Expose private String responseGrpcMessage;
    @SerializedName("responseGrpcStatus") @Expose private String responseGrpcStatus;
    @SerializedName("responseSize") @Expose private Integer responseSize;
    @SerializedName("responseTotalSize") @Expose private Integer responseTotalSize;
    @SerializedName("sourceLabelsApp") @Expose private String sourceLabelsApp;
    @SerializedName("sourceLabelsCell") @Expose private String sourceLabelsCell;
    @SerializedName("sourceLabelsCellGateway") @Expose private String sourceLabelsCellGateway;
    @SerializedName("sourceLabelsCellSlash") @Expose private String sourceLabelsCellSlash;
    @SerializedName("sourceLabelsPodTemplateHash") @Expose private String sourceLabelsPodTemplateHash;
    @SerializedName("sourceLabelsSvc") @Expose private String sourceLabelsSvc;
    @SerializedName("sourceLabelsVersion") @Expose private String sourceLabelsVersion;
    @SerializedName("sourceName") @Expose private String sourceName;
    @SerializedName("sourceNamespace") @Expose private String sourceNamespace;
    @SerializedName("sourceOwner") @Expose private String sourceOwner;
    @SerializedName("sourcePrincipal") @Expose private String sourcePrincipal;
    @SerializedName("sourceUID") @Expose private String sourceUID;
    @SerializedName("sourceWorkloadName") @Expose private String sourceWorkloadName;
    @SerializedName("sourceWorkloadNamespace") @Expose private String sourceWorkloadNamespace;
    @SerializedName("sourceWorkloadUID") @Expose private String sourceWorkloadUID;

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

    public Integer getCheckErrorCode() {
        return checkErrorCode;
    }

    public void setCheckErrorCode(Integer checkErrorCode) {
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

    public Integer getConnectionReceivedBytes() {
        return connectionReceivedBytes;
    }

    public void setConnectionReceivedBytes(Integer connectionReceivedBytes) {
        this.connectionReceivedBytes = connectionReceivedBytes;
    }

    public Integer getConnectionReceivedBytesTotal() {
        return connectionReceivedBytesTotal;
    }

    public void setConnectionReceivedBytesTotal(Integer connectionReceivedBytesTotal) {
        this.connectionReceivedBytesTotal = connectionReceivedBytesTotal;
    }

    public String getConnectionRequestedServerName() {
        return connectionRequestedServerName;
    }

    public void setConnectionRequestedServerName(String connectionRequestedServerName) {
        this.connectionRequestedServerName = connectionRequestedServerName;
    }

    public Integer getConnectionSentBytes() {
        return connectionSentBytes;
    }

    public void setConnectionSentBytes(Integer connectionSentBytes) {
        this.connectionSentBytes = connectionSentBytes;
    }

    public Integer getConnectionSentBytesTotal() {
        return connectionSentBytesTotal;
    }

    public void setConnectionSentBytesTotal(Integer connectionSentBytesTotal) {
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

    public Integer getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Integer destinationPort) {
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

    public Integer getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(Integer requestSize) {
        this.requestSize = requestSize;
    }

    public Integer getRequestTotalSize() {
        return requestTotalSize;
    }

    public void setRequestTotalSize(Integer requestTotalSize) {
        this.requestTotalSize = requestTotalSize;
    }

    public String getRequestUserAgent() {
        return requestUserAgent;
    }

    public void setRequestUserAgent(String requestUserAgent) {
        this.requestUserAgent = requestUserAgent;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
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

    public Integer getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(Integer responseSize) {
        this.responseSize = responseSize;
    }

    public Integer getResponseTotalSize() {
        return responseTotalSize;
    }

    public void setResponseTotalSize(Integer responseTotalSize) {
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
        //         map.put("sourceIP", this.sourceIP);
        map.put("sourceUID", this.sourceUID);
        map.put("sourceName", this.sourceName);
        map.put("sourceNamespace", this.sourceNamespace);
        map.put("sourcePrincipal", this.sourcePrincipal);
        map.put("sourceOwner", this.sourceOwner);
        map.put("sourceWorkloadUID", this.sourceWorkloadUID);
        map.put("sourceWorkloadName", this.sourceWorkloadName);
        map.put("sourceWorkloadNamespace", this.sourceWorkloadNamespace);
        map.put("destinationUID", this.destinationUID);
        //         map.put("destinationIP", this.destinationIP);
        map.put("destinationPort", this.destinationPort);
        map.put("destinationName", this.destinationName);
        map.put("destinationNamespace", this.destinationNamespace);
        map.put("destinationPrincipal", this.destinationPrincipal);
        map.put("destinationOwner", this.destinationOwner);
        map.put("destinationWorkloadUID", this.destinationWorkloadUID);
        //         map.put("destinationWorkloadName", this.destinationWorkloadName);
        map.put("destinationWorkloadNamespace", this.destinationWorkloadNamespace);
        //         map.put("destinationContainerImage", this.destinationContainerImage);
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
        //         map.put("requestTimeNanoSec", this.requestTimeNanoSec);
        map.put("requestTotalSize", this.requestTotalSize);
        //         map.put("requestTimeSec", this.requestTimeSec);
        //         map.put("requestTimeNanoSec", this.requestTimeNanoSec);
        map.put("requestUserAgent", this.requestUserAgent);
        map.put("responseSize", this.responseSize);
        //         map.put("responseTimeSec", this.responseTimeSec);
        //         map.put("responseTimeNanoSec", this.responseTimeNanoSec);
        //         map.put("responseDurationSec", this.responseDurationSec);
        map.put("responseDurationNanoSec", this.responseDurationNanoSec);
        map.put("responseCode", this.responseCode);
        //         map.put("responseGRPCStatus", this.responseGRPCStatus);
        //         map.put("responseGRPCMessage", this.responseGRPCMessage);
        map.put("connectionID", this.connectionID);
        map.put("connectionEvent", this.connectionEvent);
        map.put("connectionReceivedBytes", this.connectionReceivedBytes);
        map.put("connectionReceivedBytesTotal", this.connectionReceivedBytesTotal);
        map.put("connectionSentBytes", this.connectionSentBytes);
        map.put("connectionSentBytesTotal", this.connectionSentBytesTotal);
        //         map.put("connectionDurationSec", this.connectionDurationSec);
        //         map.put("connectionDurationNanoSec", this.connectionDurationNanoSec);
        map.put("connectionMTLS", this.connectionMTLS);
        map.put("connectionRequestedServerName", this.connectionRequestedServerName);
        map.put("contextProtocol", this.contextProtocol);
        //         map.put("contextTimeSec", this.contextTimeSec);
        //         map.put("contextTimeNanoSec", this.contextTimeNanoSec);
        map.put("contextReporterKind", this.contextReporterKind);
        map.put("contextReporterUID", this.contextReporterUID);
        map.put("apiService", this.apiService);
        map.put("apiVersion", this.apiVersion);
        map.put("apiOperation", this.apiOperation);
        map.put("apiProtocol", this.apiProtocol);
        map.put("requestAuthPrincipal", this.requestAuthPrincipal);
        map.put("requestAuthAudiences", this.requestAuthAudiences);
        map.put("requestAuthPresenter", this.requestAuthPresenter);
        //         map.put("requestAuthClaims", this.requestAuthClaims);
        //         map.put("requestAPIKey", this.requestAPIKey);
        map.put("checkErrorCode", this.checkErrorCode);
        map.put("checkErrorMessage", this.checkErrorMessage);
        //         map.put("checkCacheHit", this.checkCacheHit);
        map.put("quotaCacheHit", this.quotaCacheHit);
        map.put("contextReporterLocal", this.contextReporterLocal);
        map.put("responseTotalSize", this.responseTotalSize);
        return map;
    }
}
