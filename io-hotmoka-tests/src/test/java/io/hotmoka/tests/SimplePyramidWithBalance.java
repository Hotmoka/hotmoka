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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for the simple pyramid with balance contract.
 */
class SimplePyramidWithBalance extends TakamakaTest {
	private static final BigIntegerValue MINIMUM_INVESTMENT = new BigIntegerValue(BigInteger.valueOf(10_000L));
	private static final ClassType SIMPLE_PYRAMID = new ClassType("io.hotmoka.examples.ponzi.SimplePyramidWithBalance");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = new ConstructorSignature(SIMPLE_PYRAMID, ClassType.BIG_INTEGER);
	private static final MethodSignature INVEST = new VoidMethodSignature(SIMPLE_PYRAMID, "invest", ClassType.BIG_INTEGER);
	private static final MethodSignature WITHDRAW = new VoidMethodSignature(SIMPLE_PYRAMID, "withdraw");
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
	void twoInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(1), account(1), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), WITHDRAW, pyramid);
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _100_000, jar(), CodeSignature.BALANCE, account(0));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(190_000)) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(1), account(1), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(2), account(2), _100_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);
		addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), WITHDRAW, pyramid);
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _100_000, jar(), CodeSignature.BALANCE, account(0));
		assertTrue(balance0.value.compareTo(BigInteger.valueOf(201_000)) > 0);
	}
}