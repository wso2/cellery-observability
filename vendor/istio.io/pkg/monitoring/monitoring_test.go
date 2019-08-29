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

package monitoring_test

import (
	"errors"
	"fmt"
	"strings"
	"sync"
	"testing"
	"time"

	"go.opencensus.io/stats/view"
	"go.opencensus.io/tag"

	"istio.io/pkg/monitoring"
)

var (
	name = monitoring.MustCreateLabel("name")
	kind = monitoring.MustCreateLabel("kind")

	testSum = monitoring.NewSum(
		"events_total",
		"Number of events observed, by name and kind",
		monitoring.WithLabels(name, kind),
	)

	goofySum = testSum.With(kind.Value("goofy"))

	testDistribution = monitoring.NewDistribution(
		"test_buckets",
		"Testing distribution functionality",
		[]float64{0, 2.5, 7, 8, 10, 154.3, 99},
		monitoring.WithLabels(name),
		monitoring.WithUnit(monitoring.Seconds),
	)

	testGauge = monitoring.NewGauge(
		"test_gauge",
		"Testing gauge functionality",
	)
)

func init() {
	monitoring.MustRegister(testSum, testDistribution, testGauge)
}

func TestSum(t *testing.T) {
	exp := &testExporter{rows: make(map[string][]*view.Row)}
	view.RegisterExporter(exp)
	view.SetReportingPeriod(1 * time.Millisecond)

	testSum.With(name.Value("foo"), kind.Value("bar")).Increment()
	goofySum.With(name.Value("baz")).Record(45)
	goofySum.With(name.Value("baz")).Decrement()

	err := retry(
		func() error {
			exp.Lock()
			defer exp.Unlock()
			if len(exp.rows[testSum.Name()]) < 2 {
				// we should have two values goofySum (which is a dimensioned testSum) and
				// testSum.
				return errors.New("no values recorded for sum, want 2")
			}

			// only check the final values to ensure that the sum has been properly calculated
			goofySumVal := float64(0)
			testSumVal := float64(0)
			for _, r := range exp.rows[testSum.Name()] {
				if findTagWithValue("kind", "goofy", r.Tags) {
					if sd, ok := r.Data.(*view.SumData); ok {
						goofySumVal = sd.Value
					}
				} else if findTagWithValue("kind", "bar", r.Tags) {
					if sd, ok := r.Data.(*view.SumData); ok {
						testSumVal = sd.Value
					}
				} else {
					return fmt.Errorf("unknown row in results: %v", r)
				}
			}
			if got, want := goofySumVal, 44.0; got != want {
				return fmt.Errorf("bad value for %q: %f, want %f", goofySum.Name(), got, want)
			}
			if got, want := testSumVal, 1.0; got != want {
				return fmt.Errorf("bad value for %q: %f, want %f", testSum.Name(), got, want)
			}
			return nil
		},
	)

	if err != nil {
		t.Errorf("failure recording sum values: %v", err)
	}
}

func TestGauge(t *testing.T) {
	exp := &testExporter{rows: make(map[string][]*view.Row)}
	view.RegisterExporter(exp)
	view.SetReportingPeriod(1 * time.Millisecond)

	testGauge.Record(42)
	testGauge.Record(77)

	err := retry(
		func() error {
			exp.Lock()
			defer exp.Unlock()

			if len(exp.rows[testGauge.Name()]) < 1 {
				return errors.New("no values recorded for gauge, want 1")
			}

			// we only want to verify that the last value was exported
			found := false
			for _, r := range exp.rows[testGauge.Name()] {
				if lvd, ok := r.Data.(*view.LastValueData); ok {
					found = lvd.Value == 77.0
				}
			}
			if !found {
				return fmt.Errorf("expected value for gauge %q not found; expected 77.0", testGauge.Name())
			}
			return nil
		},
	)

	if err != nil {
		t.Errorf("failure recording gauge values: %v", err)
	}
}

func TestDistribution(t *testing.T) {
	exp := &testExporter{rows: make(map[string][]*view.Row)}
	view.RegisterExporter(exp)
	view.SetReportingPeriod(1 * time.Millisecond)

	funDistribution := testDistribution.With(name.Value("fun"))
	funDistribution.Record(7.7773)
	testDistribution.With(name.Value("foo")).Record(7.4)
	testDistribution.With(name.Value("foo")).Record(6.8)
	testDistribution.With(name.Value("foo")).Record(10.2)

	err := retry(
		func() error {
			exp.Lock()
			defer exp.Unlock()
			if len(exp.rows[testDistribution.Name()]) < 2 {
				return errors.New("no values recorded for distribution, want 2")
			}

			// regardless of how the observations get batched and exported, we expect to see
			// 1 total value recorded and exported for the fun distribution and 3 for the
			// test distribution
			maxFunCount := int64(0)
			maxTestCount := int64(0)
			for _, r := range exp.rows[testDistribution.Name()] {
				if findTagWithValue("name", "fun", r.Tags) {
					if dd, ok := r.Data.(*view.DistributionData); ok {
						if dd.Count > maxFunCount {
							maxFunCount = dd.Count
						}
					}
				} else if findTagWithValue("name", "foo", r.Tags) {
					if dd, ok := r.Data.(*view.DistributionData); ok {
						if dd.Count > maxTestCount {
							maxTestCount = dd.Count
						}
					}
				} else {
					return errors.New("expected distributions not found")
				}
			}

			if got, want := maxFunCount, int64(1); got != want {
				return fmt.Errorf("bad count for %q: %d, want %d", testDistribution.Name(), got, want)
			}

			if got, want := maxTestCount, int64(3); got != want {
				return fmt.Errorf("bad count for %q: %d, want %d", testDistribution.Name(), got, want)
			}

			return nil
		},
	)

	if err != nil {
		t.Errorf("failure recording distribution values: %v", err)
	}
}

func TestMustCreateLabel(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			if !strings.Contains(r.(error).Error(), "label") {
				t.Errorf("no panic for invalid label, recovered: %q", r.(error).Error())
			}
		} else {
			t.Error("no panic for failed label creation.")
		}
	}()

	// labels must be ascii
	monitoring.MustCreateLabel("£®")
}

func TestMustRegister(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			t.Errorf("no panic for failed registration.")
		}
	}()

	monitoring.MustRegister(&registerFail{})
}

func TestViewExport(t *testing.T) {
	exp := &testExporter{rows: make(map[string][]*view.Row)}
	view.RegisterExporter(exp)
	view.SetReportingPeriod(1 * time.Millisecond)

	testSum.With(name.Value("foo"), kind.Value("bar")).Increment()
	goofySum.With(name.Value("baz")).Record(45)

	err := retry(
		func() error {
			exp.Lock()
			defer exp.Unlock()
			if exp.invalidTags {
				return errors.New("view registration includes invalid tag keys")
			}
			return nil
		},
	)

	if err != nil {
		t.Errorf("failure with view export: %v", err)
	}

}

type registerFail struct {
	monitoring.Metric
}

func (r registerFail) Register() error {
	return errors.New("fail")
}

type testExporter struct {
	sync.Mutex

	rows        map[string][]*view.Row
	invalidTags bool
}

func (t *testExporter) ExportView(d *view.Data) {
	t.Lock()
	for _, tk := range d.View.TagKeys {
		if len(tk.Name()) < 1 {
			t.invalidTags = true
		}
	}
	t.rows[d.View.Name] = append(t.rows[d.View.Name], d.Rows...)
	t.Unlock()
}

func findTagWithValue(key, value string, tags []tag.Tag) bool {
	for _, t := range tags {
		if t.Key.Name() == key && t.Value == value {
			return true
		}
	}
	return false
}

// because OC uses goroutines to async export, validating proper export
// can introduce timing problems. this helper just trys validation over
// and over until the supplied method either succeeds or it times out.
func retry(fn func() error) error {
	var lasterr error
	to := time.After(1 * time.Second)
	for {
		select {
		case <-to:
			return fmt.Errorf("timeout while waiting (last error: %v)", lasterr)
		default:
		}
		if err := fn(); err != nil {
			lasterr = err
		} else {
			return nil
		}
		<-time.After(10 * time.Millisecond)
	}
}
