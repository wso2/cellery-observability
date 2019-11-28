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

package tracing_receiver

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/gorilla/handlers"
	"github.com/gorilla/mux"
	"github.com/jaegertracing/jaeger/cmd/collector/app"
	sanitizerzipkin "github.com/jaegertracing/jaeger/cmd/collector/app/sanitizer/zipkin"
	appzipkin "github.com/jaegertracing/jaeger/cmd/collector/app/zipkin"
	"github.com/jaegertracing/jaeger/model"
	thriftzipkin "github.com/jaegertracing/jaeger/model/converter/thrift/zipkin"
	"github.com/jaegertracing/jaeger/thrift-gen/zipkincore"
	"go.uber.org/zap"
)

type (
	TracingReceiver struct {
		logger *zap.SugaredLogger
		buffer chan string
	}
	ProcessedSpan struct {
		TraceID     string `json:"traceId"`
		ParentID    string `json:"parentId,omitempty"`
		ID          string `json:"id"`
		Name        string `json:"operationName"`
		ServiceName string `json:"serviceName"`
		SpanKind    string `json:"spanKind"`
		Timestamp   int64  `json:"timestamp"`
		Duration    int64  `json:"duration"`
		Tags        string `json:"tags"`
	}
	ZipkinSpanHandler struct {
		logger    *zap.SugaredLogger
		sanitizer sanitizerzipkin.Sanitizer
		receiver  *TracingReceiver
	}
	// zapRecoveryWrapper wraps a zap logger into a gorilla RecoveryLogger
	zapRecoveryWrapper struct {
		logger *zap.Logger
	}
)

const (
	tracingReceiverPort int = 9411
)

// Println logs an error message with the given fields
func (wrapper zapRecoveryWrapper) Println(args ...interface{}) {
	wrapper.logger.Error(fmt.Sprint(args...))
}

func New(logger *zap.SugaredLogger, buffer chan string) *TracingReceiver {
	tracingReceiver := &TracingReceiver{
		logger: logger,
		buffer: buffer,
	}
	return tracingReceiver
}

func (receiver *TracingReceiver) Run(errCh chan error) {
	zipkinSpansHandler := &ZipkinSpanHandler{
		logger:    receiver.logger,
		sanitizer: sanitizerzipkin.NewChainedSanitizer(sanitizerzipkin.StandardSanitizers...),
		receiver:  receiver,
	}
	zWrapper := zapRecoveryWrapper{receiver.logger.Desugar()}
	recoveryHandler := handlers.RecoveryHandler(handlers.RecoveryLogger(zWrapper), handlers.PrintRecoveryStack(true))
	startZipkinHTTPAPI(receiver.logger, tracingReceiverPort, zipkinSpansHandler, recoveryHandler, errCh)
}

func (receiver *TracingReceiver) convertSpan(span *model.Span) ProcessedSpan {
	spanKind := ""
	processedTags := make(map[string]interface{})
	tags := span.Tags
	for _, tag := range tags {
		processedTags[tag.Key] = getTagValue(tag)
	}
	if processedTags["span.kind"] != nil {
		spanKind = processedTags["span.kind"].(string)
	}
	processedTagsStr, err := json.Marshal(processedTags)
	if err != nil {
		receiver.logger.Errorf("Error when marshalling the tags : %v", err)
	}
	processedSpan := ProcessedSpan{
		TraceID:     span.TraceID.String(),
		ID:          span.SpanID.String(),
		Name:        span.OperationName,
		ServiceName: span.Process.ServiceName,
		SpanKind:    strings.ToUpper(spanKind),
		Timestamp:   span.StartTime.UnixNano() / 1000000,
		Duration:    span.Duration.Nanoseconds() / 1000000,
		Tags:        string(processedTagsStr),
	}
	parentSpanId := span.ParentSpanID().String()
	if parentSpanId != "0" {
		processedSpan.ParentID = parentSpanId
	}
	return processedSpan
}

func getTagValue(tag model.KeyValue) interface{} {
	valueType := tag.VType
	switch valueType {
	case model.ValueType_STRING:
		return tag.VStr
	case model.ValueType_BOOL:
		return tag.VBool
	case model.ValueType_INT64:
		return tag.VInt64
	case model.ValueType_FLOAT64:
		return tag.VFloat64
	case model.ValueType_BINARY:
		return tag.VBinary
	default:
		return ""
	}
}

func startZipkinHTTPAPI(
	logger *zap.SugaredLogger,
	zipkinPort int,
	zipkinSpansHandler app.ZipkinSpansHandler,
	recoveryHandler func(http.Handler) http.Handler,
	errCh chan error,
) {
	zHandler := appzipkin.NewAPIHandler(zipkinSpansHandler)
	r := mux.NewRouter()
	zHandler.RegisterRoutes(r)
	httpPortStr := ":" + strconv.Itoa(zipkinPort)
	logger.Info("Listening for Zipkin HTTP traffic", zap.Int("zipkin.http-port", zipkinPort))
	errCh <- http.ListenAndServe(httpPortStr, recoveryHandler(r))
}

func (handler *ZipkinSpanHandler) SubmitZipkinBatch(spans []*zipkincore.Span, options app.SubmitBatchOptions) ([]*zipkincore.Response, error) {
	responses := make([]*zipkincore.Response, len(spans))
	for _, span := range spans {
		sanitized := handler.sanitizer.Sanitize(span)
		processedSpans, err := thriftzipkin.ToDomainSpan(sanitized)
		if err != nil {
			handler.receiver.logger.Warnf("Warning while converting zipkin to domain span", zap.Error(err))
		}

		for _, span := range processedSpans {
			processedSpan := handler.receiver.convertSpan(span)
			jsonStr, err := json.Marshal(processedSpan)
			if err != nil {
				return nil, fmt.Errorf("could not marshal span struct : %v", err)
			}
			handler.receiver.buffer <- string(jsonStr)
			handler.logger.Debugf("received span : %s", string(jsonStr))
		}
	}
	return responses, nil
}
