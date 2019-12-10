module cellery.io/cellery-observability

go 1.12

replace (
	cellery.io/cellery-controller => cellery.io/cellery-controller v0.5.1-0.20191210065848-9219c69059bb
	github.com/apache/thrift => github.com/apache/thrift v0.0.0-20151001171628-53dd39833a08
	github.com/jaegertracing/jaeger => github.com/jaegertracing/jaeger v1.14.0
	istio.io/api => istio.io/api v0.0.0-20190517041403-820986f2947c
	istio.io/istio => istio.io/istio v0.0.0-20190627161235-cd4a148f37dc
	k8s.io/api => k8s.io/api v0.0.0-20190805141119-fdd30b57c827
	k8s.io/apimachinery => k8s.io/apimachinery v0.0.0-20190612205821-1799e75a0719
	k8s.io/client-go => k8s.io/client-go v0.0.0-20190620085101-78d2af792bab
	k8s.io/klog => k8s.io/klog v0.3.3
)

require (
	cellery.io/cellery-controller v0.5.1-0.20191210065848-9219c69059bb
	github.com/DATA-DOG/go-sqlmock v1.3.3
	github.com/apache/thrift v0.13.0 // indirect
	github.com/go-openapi/runtime v0.19.8 // indirect
	github.com/go-openapi/spec v0.19.4 // indirect
	github.com/go-openapi/validate v0.19.5 // indirect
	github.com/go-sql-driver/mysql v1.4.1
	github.com/gofrs/flock v0.7.1
	github.com/gogo/protobuf v1.2.1
	github.com/google/go-cmp v0.3.0
	github.com/gorilla/handlers v1.4.2
	github.com/gorilla/mux v1.7.3
	github.com/grpc-ecosystem/grpc-gateway v1.12.1 // indirect
	github.com/jaegertracing/jaeger v0.0.0-00010101000000-000000000000
	github.com/rs/cors v1.7.0 // indirect
	github.com/rs/xid v1.2.1
	github.com/uber/tchannel-go v1.16.0 // indirect
	go.uber.org/zap v1.13.0
	google.golang.org/grpc v1.25.1
	istio.io/api v0.0.0-20190517041403-820986f2947c
	istio.io/istio v0.0.0-00010101000000-000000000000
	k8s.io/api v0.0.0-20190805141119-fdd30b57c827
	k8s.io/apimachinery v0.0.0-20190612205821-1799e75a0719
	k8s.io/client-go v12.0.0+incompatible
	k8s.io/klog v0.3.3
)
