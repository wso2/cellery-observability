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
	"reflect"
	"strings"
	"testing"
)

func TestRegistry_Register_Double(t *testing.T) {
	defer func() {
		r := recover()
		if r == nil {
			t.Fatalf("Double register should have panicked.")
		}
	}()

	r := &Registry{
		blocks: make(map[string]*blockState),
	}

	cv := newTestCovVar(10)

	r.Register(10, "ctx", cv.readPos, cv.readStmt, cv.readCount, cv.clearCount)
	r.Register(10, "ctx", cv.readPos, cv.readStmt, cv.readCount, cv.clearCount)
}

func TestRegistry_Clear(t *testing.T) {
	r := &Registry{
		blocks: make(map[string]*blockState),
	}

	cv := newTestCovVar(10)

	r.Register(10, "ctx", cv.readPos, cv.readStmt, cv.readCount, cv.clearCount)

	cv.initSampleData()

	r.Clear()

	for i := 0; i < len(cv.count); i++ {
		if cv.count[i] != 0 {
			t.Fatalf("expected count to be cleared: %+v", cv.count)
		}
	}
}

func TestRegistry_Snapshot(t *testing.T) {
	r := &Registry{
		blocks: make(map[string]*blockState),
	}

	cv := newTestCovVar(10)
	r.Register(10, "ctx", cv.readPos, cv.readStmt, cv.readCount, cv.clearCount)

	cv.initSampleData()

	r.Snapshot()

	c := r.GetCoverage()
	if len(c.Blocks) != 1 {
		t.Fatalf("was expecting a single block: %v", c.Blocks)
	}

	b := c.Blocks[0]
	if !reflect.DeepEqual(cv.stmt, b.NumStmt) {
		t.Fatalf("NumStmt mismatch. Expected: %v, Actual: %v", cv.stmt, b.NumStmt)
	}
	if !reflect.DeepEqual(cv.count, b.Count) {
		t.Fatalf("Count mismatch. Expected: %v, Actual: %v", cv.count, b.Count)
	}
	if !reflect.DeepEqual(cv.pos, b.Pos) {
		t.Fatalf("Pod mismatch. Expected: %v, Actual: %v", cv.pos, b.Pos)
	}
}

func TestGetRegistry(t *testing.T) {
	r1 := GetRegistry()
	r2 := GetRegistry()
	if r1 != r2 {
		t.Fatal("GetRegistry returned different results")
	}
}

func TestCoverage_ProfileText(t *testing.T) {
	r := &Registry{
		blocks: make(map[string]*blockState),
	}

	cv := newTestCovVar(10)
	r.Register(10, "foo.go", cv.readPos, cv.readStmt, cv.readCount, cv.clearCount)

	cv.initSampleData()

	r.Snapshot()

	c := r.GetCoverage()
	prof := c.ProfileText()
	expected := `mode: atomic
foo.go:20.22,21.0 30 10
foo.go:23.25,24.0 31 11
foo.go:26.28,27.0 32 12
foo.go:29.31,30.0 33 13
foo.go:32.34,33.0 34 14
foo.go:35.37,36.0 35 15
foo.go:38.40,39.0 36 16
foo.go:41.43,42.0 37 17
foo.go:44.46,45.0 38 18
foo.go:47.49,48.0 39 19`

	if strings.TrimSpace(prof) != strings.TrimSpace(expected) {
		t.Fatalf("Prof mismatch. Actual:\n%s\nExpected:\n%s\n", prof, expected)
	}

}

type testCovVar struct {
	count []uint32
	pos   []uint32
	stmt  []uint16
}

func newTestCovVar(length int) *testCovVar { // nolint:unparam
	return &testCovVar{
		count: make([]uint32, length),
		pos:   make([]uint32, length*3),
		stmt:  make([]uint16, length),
	}
}

func (v *testCovVar) readPos(o []uint32) {
	for i := 0; i < len(v.pos); i++ {
		o[i] = v.pos[i]
	}
}

func (v *testCovVar) readStmt(o []uint16) {
	for i := 0; i < len(v.stmt); i++ {
		o[i] = v.stmt[i]
	}
}

func (v *testCovVar) readCount(o []uint32) {
	for i := 0; i < len(v.count); i++ {
		o[i] = v.count[i]
	}
}

func (v *testCovVar) clearCount() {
	for i := 0; i < len(v.count); i++ {
		v.count[i] = 0
	}
}

func (v *testCovVar) initSampleData() {

	for i := 0; i < len(v.count); i++ {
		v.count[i] = uint32(i + 10)
	}

	for i := 0; i < len(v.pos); i++ {
		v.pos[i] = uint32(i + 20)
	}

	for i := 0; i < len(v.stmt); i++ {
		v.stmt[i] = uint16(i + 30)
	}

}
