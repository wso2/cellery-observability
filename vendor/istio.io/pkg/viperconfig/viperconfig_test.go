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

package viperconfig

import (
	"testing"

	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
)

func TestViperConfig(t *testing.T) {
	v := viper.GetViper()
	var foo string
	hasRun := false
	c := cobra.Command{Run: func(c *cobra.Command, args []string) {
		ProcessViperConfig(c, v)
		assert.Equal(t, foo, "expected")
		hasRun = true
	}}
	AddConfigFlag(&c, v)
	c.PersistentFlags().StringVar(&foo, "foo", "notempty", "foo is a fake flag")
	v.BindPFlags(c.PersistentFlags())
	c.SetArgs([]string{"--config", "testconfig.yaml"})
	c.Execute()
	if !hasRun {
		assert.True(t, hasRun, "command never ran")
	}
}
