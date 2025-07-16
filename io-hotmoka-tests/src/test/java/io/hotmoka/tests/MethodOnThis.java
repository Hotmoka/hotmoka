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

import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for calls to methods on "this".
 */
class MethodOnThis extends HotmokaTest {
	private static final ClassType BRIDGE = StorageTypes.classNamed("io.hotmoka.examples.methodonthis.Bridge");
	private static final ClassType BRIDGE2 = StorageTypes.classNamed("io.hotmoka.examples.methodonthis.Bridge2");

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("methodonthis.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_100_000);
	}

	@Test @DisplayName("new Bridge().foo(100) then Bridge has balance 0 and its Sub field has balance 100")
	void testBalances() throws Exception {
		StorageReference bridge = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ConstructorSignatures.of(BRIDGE));
		addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			MethodSignatures.ofVoid(BRIDGE, "foo", StorageTypes.INT), bridge, StorageValues.intOf(100));
		
		var getBalance = MethodSignatures.ofNonVoid(BRIDGE, "getBalance", StorageTypes.BIG_INTEGER);
		BigInteger balanceOfBridge = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), getBalance, bridge).asReturnedBigInteger(getBalance, UnexpectedValueException::new);
		var getInitialBalance = MethodSignatures.ofNonVoid(BRIDGE, "getInitialBalance", StorageTypes.BIG_INTEGER);
		BigInteger initialBalanceOfBridge = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), getInitialBalance, bridge).asReturnedBigInteger(getInitialBalance, UnexpectedValueException::new);
		var getBalanceOfSub = MethodSignatures.ofNonVoid(BRIDGE, "getBalanceOfSub", StorageTypes.BIG_INTEGER);
		BigInteger balanceOfSub = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), getBalanceOfSub, bridge).asReturnedBigInteger(getBalanceOfSub, UnexpectedValueException::new);

		assertEquals(BigInteger.ZERO, balanceOfBridge);
		assertEquals(BigInteger.valueOf(100L), initialBalanceOfBridge);
		assertEquals(BigInteger.valueOf(100L), balanceOfSub);
	}

	@Test @DisplayName("new Bridge2().foo(100) then Bridge2 has balance 0 and its Sub2 field has balance 100")
	void testBalances2() throws Exception {
		StorageReference bridge = addConstructorCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(), ConstructorSignatures.of(BRIDGE2));
		addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			MethodSignatures.ofVoid(BRIDGE2, "foo", StorageTypes.INT), bridge, StorageValues.intOf(100));
		
		var getBalance = MethodSignatures.ofNonVoid(BRIDGE2, "getBalance", StorageTypes.BIG_INTEGER);
		BigInteger balanceOfBridge = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), getBalance, bridge).asReturnedBigInteger(getBalance, UnexpectedValueException::new);
		var getInitialBalance = MethodSignatures.ofNonVoid(BRIDGE2, "getInitialBalance", StorageTypes.BIG_INTEGER);
		BigInteger initialBalanceOfBridge = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), getInitialBalance, bridge).asReturnedBigInteger(getInitialBalance, UnexpectedValueException::new);
		var getBalanceOfSub = MethodSignatures.ofNonVoid(BRIDGE2, "getBalanceOfSub", StorageTypes.BIG_INTEGER);
		BigInteger balanceOfSub = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), getBalanceOfSub, bridge).asReturnedBigInteger(getBalanceOfSub, UnexpectedValueException::new);

		assertEquals(BigInteger.ZERO, balanceOfBridge);
		assertEquals(BigInteger.valueOf(100L), initialBalanceOfBridge);
		assertEquals(BigInteger.valueOf(100L), balanceOfSub);
	}
}