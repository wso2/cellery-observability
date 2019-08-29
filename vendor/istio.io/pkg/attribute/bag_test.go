// Copyright 2016 Istio Authors
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

package attribute

import (
	"testing"
	"time"
)

var (
	t9 = time.Date(2001, 1, 1, 1, 1, 1, 9, time.UTC)

	d1 = 42 * time.Second
)

func TestMerge(t *testing.T) {
	mb := GetMutableBag(empty)
	mb.Set("STRING0", "@")

	c1 := GetMutableBag(mb)
	c2 := GetMutableBag(mb)

	c1.Set("STRING0", "Z")
	c1.Set("STRING1", "A")
	c2.Set("STRING2", "B")

	mb.Merge(c1)
	mb.Merge(c2)

	if v, ok := mb.Get("STRING0"); !ok || v.(string) != "@" {
		t.Errorf("Got %v, expected @", v)
	}

	if v, ok := mb.Get("STRING1"); !ok || v.(string) != "A" {
		t.Errorf("Got %v, expected A", v)
	}

	if v, ok := mb.Get("STRING2"); !ok || v.(string) != "B" {
		t.Errorf("Got %v, expected B", v)
	}
}

func TestEmpty(t *testing.T) {
	b := &emptyBag{}

	if names := b.Names(); len(names) > 0 {
		t.Errorf("Get len %d, expected 0", len(names))
	}

	if _, ok := b.Get("XYZ"); ok {
		t.Errorf("Got true, expected false")
	}

	if s := b.String(); s != "" {
		t.Errorf("Got '%s', expecting an empty string", s)
	}

	b.Done()
}

func TestCopyBag(t *testing.T) {
	refBag := GetMutableBag(nil)
	refBag.Set("M1", WrapStringMap(map[string]string{"M7": "M6"}))
	refBag.Set("M2", t9)
	refBag.Set("M3", d1)
	refBag.Set("M4", []byte{11})
	refBag.Set("M5", WrapStringMap(map[string]string{"M7": "M6"}))
	refBag.Set("G4", "G5")
	refBag.Set("G6", int64(142))
	refBag.Set("G7", 142.0)

	copyBag := CopyBag(refBag)

	if !compareBags(copyBag, refBag) {
		t.Error("Bags don't match")
	}
}

func TestNil(t *testing.T) {
	var mb *MutableBag
	a := mb.Names()
	if len(a) != 0 {
		t.Errorf("Got %v, expected 0", len(a))
	}
}

func TestUseAfterFree(t *testing.T) {
	b := GetMutableBag(nil)
	b.Done()

	if err := withPanic(func() { _, _ = b.Get("XYZ") }); err == nil {
		t.Error("Expected panic")
	}

	if err := withPanic(func() { _ = b.Names() }); err == nil {
		t.Error("Expected panic")
	}

	if err := withPanic(func() { b.Done() }); err == nil {
		t.Error("Expected panic")
	}
}

func withPanic(f func()) (ret interface{}) {
	defer func() {
		ret = recover()
	}()

	f()
	return ret
}

func compareBags(b1 Bag, b2 Bag) bool {
	b1Names := b1.Names()
	b2Names := b2.Names()

	if len(b1Names) != len(b2Names) {
		return false
	}

	for _, name := range b1Names {
		v1, _ := b1.Get(name)
		v2, _ := b2.Get(name)

		if !Equal(v1, v2) {
			return false
		}
	}

	return true
}

func TestMutableBagForTesting(t *testing.T) {
	m := map[string]interface{}{
		"A": int64(1),
		"B": int64(2),
	}

	mb := GetMutableBagForTesting(m)
	if v, found := mb.Get("A"); !found {
		t.Errorf("Didn't find A")
	} else if v.(int64) != 1 {
		t.Errorf("Got %d, expecting 1", v)
	}
}

func TestReset(t *testing.T) {
	mb := GetMutableBag(nil)
	defer mb.Done()

	mb.Set("some", "value")
	mb.Reset()

	if len(mb.Names()) != 0 {
		t.Errorf("Got %v, expected %v", mb.Names(), []string{})
	}
}

func TestDelete(t *testing.T) {
	parent := GetMutableBag(nil)
	defer parent.Done()
	child := GetMutableBag(parent)
	defer child.Done()

	parent.Set("parent", true)
	child.Set("parent", false)

	if len(child.Names()) != 1 {
		t.Errorf("Got %v, expected %v", len(child.Names()), 1)
	}

	v, found := child.Get("parent")
	if !found {
		t.Error("Failed to retrieve attribute")
	}
	isParent, isBool := v.(bool)
	if !isBool || isParent {
		t.Error("Failed to retrieve bool attribute from child")
	}

	child.Delete("parent")

	v, found = child.Get("parent")
	if !found {
		t.Error("Failed to retrieve attribute after Delete")
	}
	isParent, isBool = v.(bool)
	if !isBool || !isParent {
		t.Error("Failed to retrieve bool attribute from parent after Delete")
	}
}

func TestTypes(t *testing.T) {
	if Equal(List{}, &List{}) {
		t.Error("unexpected equality")
	}
	if CheckType(List{}) {
		t.Error("expect a list pointer")
	}
}
