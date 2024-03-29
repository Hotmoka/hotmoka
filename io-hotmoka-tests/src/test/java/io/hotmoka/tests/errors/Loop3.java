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

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.node.NonWhiteListedCallException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.tests.HotmokaTest;

class Loop3 extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("loop3.jar"), takamakaCode());
	}

	@Test @DisplayName("install jar then call to Loop.loop() fails")
	void callLoop() throws TransactionException, IOException, TransactionRejectedException, InvalidKeyException, SignatureException {
		TransactionReference loop = addJarStoreTransaction(privateKey(0), account(0), _500_000, BigInteger.ONE, takamakaCode(), bytesOf("loop3.jar"), takamakaCode());

		HotmokaTest.throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () -> 
			addStaticMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, loop, MethodSignatures.ofVoid("io.hotmoka.examples.errors.loop3.Loop", "loop")));
	}
}