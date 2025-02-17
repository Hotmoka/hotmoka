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

package io.hotmoka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the deserialization of a cyclic data structure.
 */
class Cycle extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("cycle.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(BigInteger.valueOf(100_000L));
	}

	@Test @DisplayName("new Cycle().foo() == 42")
	void callFoo() throws Exception {
		StorageReference cycle = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			ConstructorSignatures.of("io.hotmoka.examples.cycle.Cycle"));

		var result = (IntValue) runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(),
			MethodSignatures.ofNonVoid("io.hotmoka.examples.cycle.Cycle", "foo", StorageTypes.INT), cycle);

		assertEquals(42, result.getValue());
	}
}