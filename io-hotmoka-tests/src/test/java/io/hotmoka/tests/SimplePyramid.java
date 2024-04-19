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

import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the simple pyramid contract.
 */
class SimplePyramid extends HotmokaTest {
	private static final BigIntegerValue MINIMUM_INVESTMENT = StorageValues.bigIntegerOf(10_000);
	private static final ClassType SIMPLE_PYRAMID = StorageTypes.classNamed("io.hotmoka.examples.ponzi.SimplePyramid");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = ConstructorSignatures.of(SIMPLE_PYRAMID, StorageTypes.BIG_INTEGER);
	private static final MethodSignature INVEST = MethodSignatures.ofVoid(SIMPLE_PYRAMID, "invest", StorageTypes.BIG_INTEGER);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("ponzi.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(BigInteger.valueOf(200_000L), BigInteger.valueOf(200_000L), BigInteger.valueOf(200_000L));
	}

	@Test @DisplayName("two investors do not get investment back yet")
	void twoInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(1), account(1), _100_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), MethodSignatures.BALANCE, account(0));
		assertTrue(balance0.getValue().compareTo(BigInteger.valueOf(190_000)) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(1), account(1), _100_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(2), account(2), _20_000, BigInteger.ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), MethodSignatures.BALANCE, account(0));
		assertTrue(balance0.getValue().compareTo(BigInteger.valueOf(201_000)) >= 0);
	}
}