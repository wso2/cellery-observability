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
	"fmt"
	"sync"
)

var registry = &Registry{
	blocks: make(map[string]*blockState),
}

// Registry for code coverage blocks
type Registry struct {
	mu     sync.RWMutex
	blocks map[string]*blockState
}

// GetRegistry returns the singleton code coverage block registry.
func GetRegistry() *Registry {
	return registry
}

// Register code coverage data structure
func (r *Registry) Register(
	length int, context string,
	readPosFn ReadPosFn, readStmtFn ReadStmtFn, readCountFn ReadCountFn, clearCountFn ClearCountFn) {
	e := initEntry(length, context, readPosFn, readStmtFn, readCountFn, clearCountFn)

	r.mu.Lock()

	_, found := r.blocks[context]
	if !found {
		r.blocks[context] = e
	}

	r.mu.Unlock()

	if found {
		panic(fmt.Sprintf("Registry.Register: Name already registered: %q", context))
	}
}

// Snapshot the coverage data from registered coverage data structures
func (r *Registry) Snapshot() {
	r.mu.RLock()
	defer r.mu.RUnlock()

	for _, e := range r.blocks {
		e.Capture()
	}
}

// Clear the coverage data from registered coverage data structures
func (r *Registry) Clear() {
	r.mu.RLock()
	defer r.mu.RUnlock()

	for _, e := range r.blocks {
		e.Clear()
	}
}

// GetCoverage collects Read from all registered blocks.
func (r *Registry) GetCoverage() *Coverage {
	r.mu.RLock()
	defer r.mu.RUnlock()

	sn := make([]*Block, 0, len(r.blocks))

	for _, e := range r.blocks {
		sn = append(sn, e.Read())
	}

	return &Coverage{
		Blocks: sn,
	}
}
