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

import static io.hotmoka.node.MethodSignatures.BALANCE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the simple pyramid with balance contract.
 */
class SimplePyramidWithBalance extends HotmokaTest {
	private static final BigIntegerValue MINIMUM_INVESTMENT = StorageValues.bigIntegerOf(10_000);
	private static final ClassType SIMPLE_PYRAMID = StorageTypes.classNamed("io.hotmoka.examples.ponzi.SimplePyramidWithBalance", IllegalArgumentException::new);
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = ConstructorSignatures.of(SIMPLE_PYRAMID, StorageTypes.BIG_INTEGER);
	private static final VoidMethodSignature INVEST = MethodSignatures.ofVoid(SIMPLE_PYRAMID, "invest", StorageTypes.BIG_INTEGER);
	private static final VoidMethodSignature WITHDRAW = MethodSignatures.ofVoid(SIMPLE_PYRAMID, "withdraw");
	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("ponzi.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_200_000, _200_000, _200_000);
	}

	@Test @DisplayName("two investors do not get investment back yet")
	void twoInvestors() throws Exception {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), WITHDRAW, pyramid);
		var balance0 = runInstanceNonVoidMethodCallTransaction(account(0), _100_000, jar(), BALANCE, account(0)).asReturnedBigInteger(BALANCE, NodeException::new);
		assertTrue(balance0.compareTo(BigInteger.valueOf(190_000)) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws Exception {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceVoidMethodCallTransaction(privateKey(2), account(2), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), WITHDRAW, pyramid);
		var balance0 = runInstanceNonVoidMethodCallTransaction(account(0), _100_000, jar(), BALANCE, account(0)).asReturnedBigInteger(BALANCE, NodeException::new);
		assertTrue(balance0.compareTo(BigInteger.valueOf(201_000)) > 0);
	}
}