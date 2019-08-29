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

import "sync"

type blockState struct {
	// registered content
	length int

	ephemeralMu sync.Mutex
	ephemeral   Block

	readPosFn    ReadPosFn
	ReadStmtFn   ReadStmtFn
	readCountFn  ReadCountFn
	clearCountFn ClearCountFn
}

func initEntry(
	length int, name string,
	readPosFn ReadPosFn, readStmtFn ReadStmtFn, readCountFn ReadCountFn, clearCountFn ClearCountFn) *blockState {

	e := &blockState{
		length: length,
		ephemeral: Block{
			Name: name,
		},
		readPosFn:    readPosFn,
		ReadStmtFn:   readStmtFn,
		readCountFn:  readCountFn,
		clearCountFn: clearCountFn,
	}

	return e
}

// Capture the coverage data in ephemeral state.
func (e *blockState) Capture() {
	e.ephemeralMu.Lock()
	defer e.ephemeralMu.Unlock()

	e.initEphemeralState()

	for i := 0; i < e.length; i++ {
		e.readCountFn(e.ephemeral.Count)
	}
}

// Clear the coverage data in ephemeral state.
func (e *blockState) Clear() {
	e.ephemeralMu.Lock()
	defer e.ephemeralMu.Unlock()

	e.initEphemeralState()

	for i := 0; i < e.length; i++ {
		e.clearCountFn()
	}
}

// Read create a snapshot from the ephemeral state.
func (e *blockState) Read() *Block {
	e.ephemeralMu.Lock()
	defer e.ephemeralMu.Unlock()

	e.initEphemeralState()

	return e.ephemeral.Clone()
}

func (e *blockState) initEphemeralState() {
	// Must be called under lock

	if len(e.ephemeral.Count) == 0 {
		e.ephemeral.Count = make([]uint32, e.length)
		e.ephemeral.Pos = make([]uint32, e.length*3)
		e.ephemeral.NumStmt = make([]uint16, e.length)

		e.ReadStmtFn(e.ephemeral.NumStmt)
		e.readPosFn(e.ephemeral.Pos)
	}
}
