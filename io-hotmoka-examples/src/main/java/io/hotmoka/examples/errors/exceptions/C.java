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

package io.hotmoka.examples.errors.exceptions;

import java.util.Optional;

public class C {

	public static void foo1() {
		// the following goes into an exception inside the Java library
		Optional.of(null);
	}

	public static void foo2(Object o) {
		// the following goes into an exception if o is null, but inside the Takamaka code
		o.hashCode();
	}
}