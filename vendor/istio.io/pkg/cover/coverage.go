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
	"io"
	"strings"
)

// Coverage data
type Coverage struct {
	Blocks []*Block `json:"blocks"`
}

// WriteProfile generates output in the form of cover profile output file and writes to the given writer.
func (c *Coverage) WriteProfile(w io.Writer) error {
	if _, err := fmt.Fprintf(w, "mode: atomic\n"); err != nil {
		return err
	}

	for _, s := range c.Blocks {
		for i := 0; i < len(s.Count); i++ {
			line0 := s.Pos[3*i+0]
			col0 := uint16(s.Pos[3*i+2])
			line1 := s.Pos[3*i+1]
			col1 := uint16(s.Pos[3*i+2] >> 16)
			stmts := s.NumStmt[i]
			count := s.Count[i]

			if _, err := fmt.Fprintf(w, "%s:%d.%d,%d.%d %d %d\n",
				s.Name, line0, col0, line1, col1, stmts, count); err != nil {
				return err
			}
		}
	}

	return nil
}

// ProfileText generates output in the form of cover profile output file.
func (c *Coverage) ProfileText() string {
	var b strings.Builder
	_ = c.WriteProfile(&b)
	return b.String()
}
