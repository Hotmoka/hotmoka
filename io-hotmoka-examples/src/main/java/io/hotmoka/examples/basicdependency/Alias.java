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

package io.hotmoka.examples.basicdependency;

import java.math.BigInteger;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

// a test on equality of deserialized values
public class Alias extends Storage {

	@View
	public boolean test(Alias a1, Alias a2) {
		return a1 == a2;
	}

	@View
	public boolean test(String s1, String s2) {
		return s1 == s2;
	}

	@View
	public boolean test(BigInteger bi1, BigInteger bi2) {
		return bi1 == bi2;
	}
}