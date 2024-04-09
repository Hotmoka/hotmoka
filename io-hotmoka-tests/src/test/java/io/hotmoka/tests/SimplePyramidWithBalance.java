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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test for the simple pyramid with balance contract.
 */
class SimplePyramidWithBalance extends HotmokaTest {
	private static final BigIntegerValue MINIMUM_INVESTMENT = StorageValues.bigIntegerOf(10_000);
	private static final ClassType SIMPLE_PYRAMID = StorageTypes.classNamed("io.hotmoka.examples.ponzi.SimplePyramidWithBalance");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = ConstructorSignatures.of(SIMPLE_PYRAMID, StorageTypes.BIG_INTEGER);
	private static final MethodSignature INVEST = MethodSignatures.ofVoid(SIMPLE_PYRAMID, "invest", StorageTypes.BIG_INTEGER);
	private static final MethodSignature WITHDRAW = MethodSignatures.ofVoid(SIMPLE_PYRAMID, "withdraw");
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
	void twoInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(1), account(1), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), WITHDRAW, pyramid);
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _100_000, jar(), MethodSignatures.BALANCE, account(0));
		assertTrue(balance0.getValue().compareTo(BigInteger.valueOf(190_000)) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(1), account(1), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(2), account(2), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), WITHDRAW, pyramid);
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _100_000, jar(), MethodSignatures.BALANCE, account(0));
		assertTrue(balance0.getValue().compareTo(BigInteger.valueOf(201_000)) > 0);
	}
}