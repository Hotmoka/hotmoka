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

package io.hotmoka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.hotmoka.tests.HotmokaTest;

class IllegalCallToNonWhiteListedMethod11 extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("System.currentTimeMillis()")
	void testNonWhiteListedCall() {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, takamakaCode(), new NonVoidMethodSignature(System.class.getName(), "currentTimeMillis", BasicTypes.LONG))
		);
	}
}