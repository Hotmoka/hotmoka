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

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the deserialization of a cyclic data structure.
 */
class PayableFailure extends HotmokaTest {

	private final static ClassType C = StorageTypes.classNamed("io.hotmoka.examples.payablefailure.C");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("payablefailure.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
	}

	@Test @DisplayName("new C().foo(null) goes into exception")
	void callFoo() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), ConstructorSignatures.of(C));

		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "parameter cannot be null", () ->
			addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), MethodSignatures.ofVoid(C, "foo", C), c, StorageValues.NULL));

		BigInteger balance = ((BigIntegerValue) runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), MethodSignatures.BALANCE, account(0))).getValue();
		assertEquals(_1_000_000, balance);
	}

	@Test @DisplayName("new C().goo(1000, null) goes into exception and 1000 remains in the balance of the caller")
	void callGoo() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), ConstructorSignatures.of(C));

		// sends 1000 as payment for a transaction that fails
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "parameter cannot be null", () ->
			addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), MethodSignatures.ofVoid(C, "goo", StorageTypes.LONG, C), c, StorageValues.longOf(1000), StorageValues.NULL));

		BigInteger balance = ((BigIntegerValue) runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), MethodSignatures.BALANCE, account(0))).getValue();

		// 1000 is back in the balance of the caller
		assertEquals(_1_000_000, balance);
	}
}