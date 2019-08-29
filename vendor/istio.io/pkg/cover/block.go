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

// Block is code coverage snapshot for a particular file block.
type Block struct {
	Name    string   `json:"name"`
	Count   []uint32 `json:"count"`
	Pos     []uint32 `json:"pos"`
	NumStmt []uint16 `json:"num_stmt"`
}

// Clone the snapshot
func (s *Block) Clone() *Block {
	count := make([]uint32, len(s.Count))
	copy(count, s.Count)

	return &Block{
		Name: s.Name,

		// Use NumStmt and Pos directly as they are created/initialized only once.
		NumStmt: s.NumStmt,
		Pos:     s.Pos,

		Count: count,
	}
}
