/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.examples.basic;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

public class Simple extends Storage {
	private int i;

	public Simple(int i) {
		this.i = i;
	}

	// this is not a legal @View
	public @View int foo1() {
		i++;
		return 42;
	}

	// this is not a legal @View
	public @View Simple foo2() {
		return new Simple(i);
	}

	public @View int foo3() {
		return i;
	}

	public @View int foo4() {
		Simple s = new Simple(i);
		s.i++;

		return i;
	}

	public @View static int foo5() {
		Simple s = new Simple(13);
		s.i++;

		return s.i;
	}
}