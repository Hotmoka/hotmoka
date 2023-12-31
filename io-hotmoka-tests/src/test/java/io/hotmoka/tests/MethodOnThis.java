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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageTypes;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for calls to methods on "this".
 */
class MethodOnThis extends HotmokaTest {
	private static final ClassType BRIDGE = StorageTypes.of("io.hotmoka.examples.methodonthis.Bridge");
	private static final ClassType BRIDGE2 = StorageTypes.of("io.hotmoka.examples.methodonthis.Bridge2");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("methodonthis.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_100_000);
	}

	@Test @DisplayName("new Bridge().foo(100) then Bridge has balance 0 and its Sub field has balance 100")
	void testBalances() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference bridge = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), new ConstructorSignature(BRIDGE));
		addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			new VoidMethodSignature(BRIDGE, "foo", BasicTypes.INT), bridge, new IntValue(100));
		
		BigIntegerValue balanceOfBridge = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), new NonVoidMethodSignature(BRIDGE, "getBalance", StorageTypes.BIG_INTEGER), bridge);
		BigIntegerValue initialBalanceOfBridge = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), new NonVoidMethodSignature(BRIDGE, "getInitialBalance", StorageTypes.BIG_INTEGER), bridge);
		BigIntegerValue balanceOfSub = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), new NonVoidMethodSignature(BRIDGE, "getBalanceOfSub", StorageTypes.BIG_INTEGER), bridge);

		assertEquals(BigInteger.ZERO, balanceOfBridge.value);
		assertEquals(BigInteger.valueOf(100L), initialBalanceOfBridge.value);
		assertEquals(BigInteger.valueOf(100L), balanceOfSub.value);
	}

	@Test @DisplayName("new Bridge2().foo(100) then Bridge2 has balance 0 and its Sub2 field has balance 100")
	void testBalances2() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference bridge = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), new ConstructorSignature(BRIDGE2));
		addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			new VoidMethodSignature(BRIDGE2, "foo", BasicTypes.INT), bridge, new IntValue(100));
		
		BigIntegerValue balanceOfBridge = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), new NonVoidMethodSignature(BRIDGE2, "getBalance", StorageTypes.BIG_INTEGER), bridge);
		BigIntegerValue initialBalanceOfBridge = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), new NonVoidMethodSignature(BRIDGE2, "getInitialBalance", StorageTypes.BIG_INTEGER), bridge);
		BigIntegerValue balanceOfSub = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), new NonVoidMethodSignature(BRIDGE2, "getBalanceOfSub", StorageTypes.BIG_INTEGER), bridge);

		assertEquals(BigInteger.ZERO, balanceOfBridge.value);
		assertEquals(BigInteger.valueOf(100L), initialBalanceOfBridge.value);
		assertEquals(BigInteger.valueOf(100L), balanceOfSub.value);
	}
}