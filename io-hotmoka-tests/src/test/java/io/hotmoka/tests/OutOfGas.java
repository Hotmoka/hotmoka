/*
Copyright 2025 Fausto Spoto

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

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.OutOfGasException;

/**
 * A test for the execution of code that drains all gas.
 */
class OutOfGas extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("outofgas.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
	}

	@Test @DisplayName("OutOfGas.loop() drains all gas and throws exception")
	void callToLoopDrainsAllGas() {
		throwsTransactionExceptionWithCause(OutOfGasException.class, () ->
			addStaticVoidMethodCallTransaction(privateKey(0), account(0), _1_000_000, BigInteger.ONE, jar(),
				MethodSignatures.ofVoid(StorageTypes.classNamed("io.hotmoka.examples.outofgas.OutOfGas"), "loop")));
	}
}