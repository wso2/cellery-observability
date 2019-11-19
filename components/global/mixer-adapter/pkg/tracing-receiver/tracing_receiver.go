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

	"github.com/gorilla/handlers"
	"github.com/gorilla/mux"
	"github.com/jaegertracing/jaeger/cmd/collector/app"
	sanitizerzipkin "github.com/jaegertracing/jaeger/cmd/collector/app/sanitizer/zipkin"
	appzipkin "github.com/jaegertracing/jaeger/cmd/collector/app/zipkin"
	"github.com/jaegertracing/jaeger/model"
	thriftzipkin "github.com/jaegertracing/jaeger/model/converter/thrift/zipkin"
	"github.com/jaegertracing/jaeger/thrift-gen/zipkincore"
	"github.com/rs/cors"
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
		Timestamp   int    `json:"timestamp"`
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
	defaultTracingReceiverPort int = 9411
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
	recoveryHandler := NewRecoveryHandler(receiver.logger.Desugar(), true)
	startZipkinHTTPAPI(receiver.logger, defaultTracingReceiverPort, zipkinSpansHandler, recoveryHandler, errCh)
}

func (receiver *TracingReceiver) convertSpan(span *model.Span) ProcessedSpan {
	spanKind := ""
	processedTags := make(map[string]string)
	tags := span.Tags
	for _, tag := range tags {
		processedTags[tag.Key] = tag.VStr
		if tag.Key == "span.kind" {
			spanKind = tag.VStr
		}
	}
	processedTagsStr, err := json.Marshal(processedTags)
	if err != nil {
		receiver.logger.Errorf("Error when marshalling the tags : %v", err)
	}
	processedSpan := ProcessedSpan{
		TraceID:     span.TraceID.String(),
		ParentID:    span.ParentSpanID().String(),
		ID:          span.SpanID.String(),
		Name:        span.OperationName,
		ServiceName: span.Process.ServiceName,
		SpanKind:    spanKind,
		Timestamp:   span.StartTime.Nanosecond(),
		Duration:    span.Duration.Nanoseconds(),
		Tags:        string(processedTagsStr),
	}
	return processedSpan
}

func startZipkinHTTPAPI(
	logger *zap.SugaredLogger,
	zipkinPort int,
	zipkinSpansHandler app.ZipkinSpansHandler,
	recoveryHandler func(http.Handler) http.Handler,
	errCh chan error,
) {
	if zipkinPort != 0 {
		zHandler := appzipkin.NewAPIHandler(zipkinSpansHandler)
		r := mux.NewRouter()
		zHandler.RegisterRoutes(r)

		c := cors.New(cors.Options{
			AllowedMethods: []string{"POST"}, // Allowing only POST, because that's the only handled one
		})

		httpPortStr := ":" + strconv.Itoa(zipkinPort)
		logger.Info("Listening for Zipkin HTTP traffic", zap.Int("zipkin.http-port", zipkinPort))

		errCh <- http.ListenAndServe(httpPortStr, c.Handler(recoveryHandler(r)))
	}
}

func (handler *ZipkinSpanHandler) SubmitZipkinBatch(spans []*zipkincore.Span, options app.SubmitBatchOptions) ([]*zipkincore.Response, error) {
	mSpans := make([]*model.Span, 0, len(spans))
	responses := make([]*zipkincore.Response, len(spans))
	for _, span := range spans {
		sanitized := handler.sanitizer.Sanitize(span)
		mSpans = append(mSpans, convertZipkinToModel(sanitized, handler.logger)...)
	}
	for _, span := range mSpans {
		processedSpan := handler.receiver.convertSpan(span)
		jsonStr, err := json.Marshal(processedSpan)
		if err != nil {
			return nil, fmt.Errorf("could not marshal span struct : %v", err)
		}
		handler.receiver.writeToBuffer(string(jsonStr))
		handler.logger.Debugf("received span : %s", string(jsonStr))
	}
	return responses, nil
}

// ConvertZipkinToModel is a helper function that logs warnings during conversion
func convertZipkinToModel(zSpan *zipkincore.Span, logger *zap.SugaredLogger) []*model.Span {
	mSpans, err := thriftzipkin.ToDomainSpan(zSpan)
	if err != nil {
		logger.Warnf("Warning while converting zipkin to domain span", zap.Error(err))
	}
	return mSpans
}

func NewRecoveryHandler(logger *zap.Logger, printStack bool) func(handler http.Handler) http.Handler {
	zWrapper := zapRecoveryWrapper{logger}
	return handlers.RecoveryHandler(handlers.RecoveryLogger(zWrapper), handlers.PrintRecoveryStack(printStack))
}

func (receiver *TracingReceiver) writeToBuffer(span string) {
	receiver.buffer <- span
}
