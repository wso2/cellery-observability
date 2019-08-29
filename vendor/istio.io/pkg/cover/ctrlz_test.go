// Copyright 2019 Istio Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package cover

import (
	"html/template"
	"io/ioutil"
	"net"
	"net/http"
	"strings"
	"testing"

	"github.com/gorilla/mux"
)

func TestTopic_Title(t *testing.T) {
	to := &topic{}
	actual := to.Title()
	if actual != "Code Coverage" {
		t.Fatalf("unexpected title: %v", actual)
	}
}

func TestTopic_Prefix(t *testing.T) {
	to := &topic{}
	actual := to.Prefix()
	if actual != "coverage" {
		t.Fatalf("unexpected prefix: %v", actual)
	}
}

func TestTopic_Activate(t *testing.T) {
	r := GetRegistry()

	to := &topic{}

	ctx := &topicContext{}
	to.Activate(ctx)

	// Check each call path
	cv := newTestCovVar(10)
	r.Register(10, "some_bizzare_file", cv.readPos, cv.readStmt, cv.readCount, cv.clearCount)

	cv.initSampleData()

	r.Snapshot()

	l, err := net.Listen("tcp", "localhost:0")
	if err != nil {
		t.Fatalf("Unable to listen: %v", err)
	}
	defer func() { _ = l.Close() }()

	go func() { _ = http.Serve(l, ctx.htmlRouter) }()

	baseURL := "http://" + l.Addr().String()

	resp := getOrFail(t, baseURL)
	if resp != "" {
		t.Fatalf("foo")
	}

	resp = getOrFail(t, baseURL+"/download")
	expected := `mode: atomic
some_bizzare_file:20.22,21.0 30 10
some_bizzare_file:23.25,24.0 31 11
some_bizzare_file:26.28,27.0 32 12
some_bizzare_file:29.31,30.0 33 13
some_bizzare_file:32.34,33.0 34 14
some_bizzare_file:35.37,36.0 35 15
some_bizzare_file:38.40,39.0 36 16
some_bizzare_file:41.43,42.0 37 17
some_bizzare_file:44.46,45.0 38 18
some_bizzare_file:47.49,48.0 39 19`

	if strings.TrimSpace(resp) != strings.TrimSpace(expected) {
		t.Fatalf("Unexpected response:  %v", resp)
	}

	lj, err := net.Listen("tcp", "localhost:0")
	if err != nil {
		t.Fatalf("Unable to listen: %v", err)
	}

	defer func() { _ = lj.Close() }()

	go func() { _ = http.Serve(lj, ctx.jsonRouter) }()

	// clear and download again
	baseJSONUrl := "http://" + lj.Addr().String()
	_ = postOrFail(t, baseJSONUrl+"/clear")

	baseJSONUrl = "http://" + lj.Addr().String()
	_ = postOrFail(t, baseJSONUrl+"/snapshot")

	resp = getOrFail(t, baseURL+"/download")
	expected = `mode: atomic
some_bizzare_file:20.22,21.0 30 0
some_bizzare_file:23.25,24.0 31 0
some_bizzare_file:26.28,27.0 32 0
some_bizzare_file:29.31,30.0 33 0
some_bizzare_file:32.34,33.0 34 0
some_bizzare_file:35.37,36.0 35 0
some_bizzare_file:38.40,39.0 36 0
some_bizzare_file:41.43,42.0 37 0
some_bizzare_file:44.46,45.0 38 0
some_bizzare_file:47.49,48.0 39 0`

	if strings.TrimSpace(resp) != strings.TrimSpace(expected) {
		t.Fatalf("Unexpected response:  %v", resp)
	}

	cv.count[0] = 255
	cv.count[1] = 255
	cv.count[2] = 255
	cv.count[3] = 255

	// snapshot and download again
	baseJSONUrl = "http://" + lj.Addr().String()
	_ = postOrFail(t, baseJSONUrl+"/snapshot")

	resp = getOrFail(t, baseURL+"/download")
	expected = `mode: atomic
some_bizzare_file:20.22,21.0 30 255
some_bizzare_file:23.25,24.0 31 255
some_bizzare_file:26.28,27.0 32 255
some_bizzare_file:29.31,30.0 33 255
some_bizzare_file:32.34,33.0 34 0
some_bizzare_file:35.37,36.0 35 0
some_bizzare_file:38.40,39.0 36 0
some_bizzare_file:41.43,42.0 37 0
some_bizzare_file:44.46,45.0 38 0
some_bizzare_file:47.49,48.0 39 0`

	if strings.TrimSpace(resp) != strings.TrimSpace(expected) {
		t.Fatalf("Unexpected response:  %v", resp)
	}
}

func getOrFail(t *testing.T, url string) string {
	t.Helper()

	r, err := http.Get(url)
	if err != nil {
		t.Fatalf("Unexpected Get response for %q: %v", url, r)
	}

	if r.StatusCode != http.StatusOK {
		t.Fatalf("Unexpected Get status for %q: %v", url, r.StatusCode)
	}

	b, err := ioutil.ReadAll(r.Body)
	if err != nil {
		t.Fatalf("Error reading body for %q: %v", url, err)
	}

	return string(b)
}

func postOrFail(t *testing.T, url string) string {
	t.Helper()

	r, err := http.Post(url, "", nil)
	if err != nil {
		t.Fatalf("Unexpected Post response for %q: %v", url, r)
	}

	if r.StatusCode != http.StatusOK {
		t.Fatalf("Unexpected Post status for %q: %v", url, r.StatusCode)
	}

	b, err := ioutil.ReadAll(r.Body)
	if err != nil {
		t.Fatalf("Error reading body for %q: %v", url, err)
	}

	return string(b)
}

type topicContext struct {
	htmlRouter *mux.Router
	jsonRouter *mux.Router
}

func (t *topicContext) HTMLRouter() *mux.Router {
	if t.htmlRouter == nil {
		t.htmlRouter = mux.NewRouter()
	}

	return t.htmlRouter
}

// JSONRouter is used to control HTML traffic delivered to this topic.
func (t *topicContext) JSONRouter() *mux.Router {
	if t.jsonRouter == nil {
		t.jsonRouter = mux.NewRouter()
	}

	return t.jsonRouter
}

// Layout is the template used as the primary layout for the topic's HTML content.
func (t *topicContext) Layout() *template.Template {
	te := template.Must(template.New("layout").Parse(""))
	return te
}
