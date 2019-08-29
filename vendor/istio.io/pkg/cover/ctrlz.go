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
	"net/http"
	"strconv"

	"istio.io/pkg/cover/assets"
	"istio.io/pkg/ctrlz"
	"istio.io/pkg/ctrlz/fw"
)

func init() {
	ctrlz.RegisterTopic(&topic{})
}

type topic struct {
	tmpl *template.Template
}

// Title returns the title for the area, which will be used in the sidenav and window title.
func (t *topic) Title() string {
	return "Code Coverage"
}

// Prefix is the name used to reference this functionality in URLs.
func (t *topic) Prefix() string {
	return "coverage"
}

// Activate triggers a topic to register itself to receive traffic.
func (t *topic) Activate(ctx fw.TopicContext) {

	l := template.Must(ctx.Layout().Clone())
	t.tmpl = template.Must(l.Parse(string(assets.MustAsset("templates/index.html"))))

	_ = ctx.HTMLRouter().
		StrictSlash(true).
		NewRoute().
		Path("/").
		HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
			fw.RenderHTML(w, t.tmpl, GetRegistry().GetCoverage())
		})

	_ = ctx.HTMLRouter().
		StrictSlash(true).
		NewRoute().
		Path("/download").
		HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
			b := []byte(GetRegistry().GetCoverage().ProfileText())
			w.Header().Set("Content-Type", "application/text; charset=utf-8")
			w.Header().Add("Content-Disposition", `attachment; filename="cover.profile"`)
			w.Header().Set("Content-Transfer-Encoding", "binary")
			w.Header().Set("Content-Length", strconv.Itoa(len(b)))
			w.WriteHeader(http.StatusOK)
			_, _ = w.Write(b)
		})

	_ = ctx.JSONRouter().
		StrictSlash(true).
		NewRoute().
		Methods("POST").
		Path("/snapshot").
		HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
			GetRegistry().Snapshot()

		})

	_ = ctx.JSONRouter().
		StrictSlash(true).
		NewRoute().
		Methods("POST").
		Path("/clear").
		HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
			GetRegistry().Clear()
		})
}
