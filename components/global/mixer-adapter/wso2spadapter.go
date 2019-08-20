// nolint:lll
// Generates the mygrpcadapter adapter's resource yaml. It contains the adapter's configuration, name,
// supported template names (metric in this case), and whether it is session or no-session based.
//go:generate $GOPATH/src/istio.io/istio/bin/mixer_codegen.sh -a mixer/adapter/wso2spadapter/config/config.proto -x "-s=false -n wso2spadapter -t metric"

package wso2spadapter

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"io/ioutil"
	"net"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"

	"bytes"
	"os"

	"github.com/wso2-cellery/mesh-observability/components/global/mixer-adapter/config"
	"istio.io/api/mixer/adapter/model/v1beta1"
	policy "istio.io/api/policy/v1beta1"
	"istio.io/istio/mixer/template/metric"
	"istio.io/pkg/log"
)

type (
	// Server is basic server interface
	Server interface {
		Addr() string
		Close() error
		Run(shutdown chan error)
	}

	// Wso2SpAdapter supports metric template.
	Wso2SpAdapter struct {
		listener net.Listener
		server   *grpc.Server
	}
)

// HandleMetric records metric entries
func (s *Wso2SpAdapter) HandleMetric(ctx context.Context, r *metric.HandleMetricRequest) (*v1beta1.ReportResult, error) {

	log.Infof("received request %v\n", *r)

	var buffer bytes.Buffer
	cfg := &config.Params{}

	buffer.WriteString(fmt.Sprintf("HandleMetric invoked with:\n  Adapter config: %s\n  Instances: %s\n",
		cfg.String(), instances(r.Instances)))

	log.Infof(fmt.Sprintf("Instances: %s\n", instances(r.Instances)))

	if cfg.FilePath == "" {
		fmt.Println(buffer.String())
	} else {
		_, err := os.OpenFile("out.txt", os.O_RDONLY|os.O_CREATE, 0666)
		if err != nil {
			log.Errorf("error creating file: %v", err)
		}
		file, err := os.OpenFile(cfg.FilePath, os.O_APPEND|os.O_WRONLY, 0600)
		if err != nil {
			log.Errorf("error opening file for append: %v", err)
		}

		defer file.Close()

		if _, err = file.Write(buffer.Bytes()); err != nil {
			log.Errorf("error writing to file: %v", err)
		}
	}

	return &v1beta1.ReportResult{}, nil
}

func decodeDimensions(in map[string]*policy.Value) map[string]interface{} {
	out := make(map[string]interface{}, len(in))
	for k, v := range in {
		out[k] = decodeValue(v.GetValue())
	}
	return out
}

func decodeValue(in interface{}) interface{} {
	switch t := in.(type) {
	case *policy.Value_StringValue:
		return t.StringValue
	case *policy.Value_Int64Value:
		return t.Int64Value
	case *policy.Value_DoubleValue:
		return t.DoubleValue
	default:
		return fmt.Sprintf("%v", in)
	}
}

func instances(in []*metric.InstanceMsg) string {
	var b bytes.Buffer
	for _, inst := range in {
		b.WriteString(fmt.Sprintf("'%s':\n"+
			"  {\n"+
			"		Value = %v\n"+
			"		Dimensions = %v\n"+
			"  }", inst.Name, decodeValue(inst.Value.GetValue()), decodeDimensions(inst.Dimensions)))
	}
	return b.String()
}

// Addr returns the listening address of the server
func (s *Wso2SpAdapter) Addr() string {
	return s.listener.Addr().String()
}

// Run starts the server run
func (s *Wso2SpAdapter) Run(shutdown chan error) {
	shutdown <- s.server.Serve(s.listener)
}

// Close gracefully shuts down the server; used for testing
func (s *Wso2SpAdapter) Close() error {
	if s.server != nil {
		s.server.GracefulStop()
	}

	if s.listener != nil {
		_ = s.listener.Close()
	}

	return nil
}

func getServerTLSOption(credential, privateKey, caCertificate string) (grpc.ServerOption, error) {
	certificate, err := tls.LoadX509KeyPair(
		credential,
		privateKey,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to load key cert pair")
	}
	certPool := x509.NewCertPool()
	bs, err := ioutil.ReadFile(caCertificate)
	if err != nil {
		return nil, fmt.Errorf("failed to read client ca cert: %s", err)
	}

	ok := certPool.AppendCertsFromPEM(bs)
	if !ok {
		return nil, fmt.Errorf("failed to append client certs")
	}

	tlsConfig := &tls.Config{
		Certificates: []tls.Certificate{certificate},
		ClientCAs:    certPool,
	}
	tlsConfig.ClientAuth = tls.RequireAndVerifyClientCert

	return grpc.Creds(credentials.NewTLS(tlsConfig)), nil
}

// NewWso2SpAdapter creates a new IBP adapter that listens at provided port.
func NewWso2SpAdapter(addr string) (Server, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%s", addr))
	if err != nil {
		return nil, fmt.Errorf("unable to listen on socket: %v", err)
	}
	adapter := &Wso2SpAdapter{
		listener: listener,
	}
	fmt.Printf("listening on \"%v\"\n", adapter.Addr())

	/* Mutual TLS feature to secure connection between workloads
	   This is optional. */
	credential := os.Getenv("GRPC_ADAPTER_CREDENTIAL")   // adapter.crt
	privateKey := os.Getenv("GRPC_ADAPTER_PRIVATE_KEY")  // adapter.key
	certificate := os.Getenv("GRPC_ADAPTER_CERTIFICATE") // ca.pem
	if credential != "" {
		serverOption, err := getServerTLSOption(credential, privateKey, certificate)
		if err != nil {
			return nil, err
		}
		adapter.server = grpc.NewServer(serverOption)
	} else {
		adapter.server = grpc.NewServer()
	}
	metric.RegisterHandleMetricServiceServer(adapter.server, adapter)
	return adapter, nil
}
