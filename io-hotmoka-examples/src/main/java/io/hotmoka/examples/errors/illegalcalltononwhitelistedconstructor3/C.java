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

package io.hotmoka.examples.errors.illegalcalltononwhitelistedconstructor3;

import java.util.Random;
import java.util.function.Supplier;

public class C {

	public int foo() {
		return test(() -> new Random()); //// KO: this goes inside a method called by a bootstrap method
	}

	private int test(Supplier<Random> supplier) {
		return supplier.get().nextInt();
	}
}