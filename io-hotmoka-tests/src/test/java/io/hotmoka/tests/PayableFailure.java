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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.StorageReference;

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
	void callFoo() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), new ConstructorSignature(C));

		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "parameter cannot be null", () ->
			addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), new VoidMethodSignature(C, "foo", C), c, NullValue.INSTANCE));

		BigInteger balance = ((BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), CodeSignature.BALANCE, account(0))).value;
		assertEquals(_1_000_000, balance);
	}

	@Test @DisplayName("new C().goo(1000, null) goes into exception and 1000 remains in the balance of the caller")
	void callGoo() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), new ConstructorSignature(C));

		// sends 1000 as payment for a transaction that fails
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "parameter cannot be null", () ->
			addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, ZERO, jar(), new VoidMethodSignature(C, "goo", StorageTypes.LONG, C), c, new LongValue(1000), NullValue.INSTANCE));

		BigInteger balance = ((BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), CodeSignature.BALANCE, account(0))).value;

		// 1000 is back in the balance of the caller
		assertEquals(_1_000_000, balance);
	}
}