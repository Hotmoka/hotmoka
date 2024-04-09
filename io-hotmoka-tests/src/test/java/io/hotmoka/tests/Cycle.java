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
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

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
	void callFoo() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference cycle = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			ConstructorSignatures.of("io.hotmoka.examples.cycle.Cycle"));

		var result = (IntValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(),
			MethodSignatures.of("io.hotmoka.examples.cycle.Cycle", "foo", StorageTypes.INT), cycle);

		assertEquals(42, result.getValue());
	}
}