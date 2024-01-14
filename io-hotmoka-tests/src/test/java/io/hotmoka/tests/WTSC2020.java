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

import static java.math.BigInteger.ONE;
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
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.node.NonWhiteListedCallException;

/**
 * A test for the simple pyramid contract, used at the WTSC2020 workshop.
 */
class WTSC2020 extends HotmokaTest {
	private static final BigIntegerValue MINIMUM_INVESTMENT = StorageValues.bigIntegerOf(_50_000);
	private static final ClassType SIMPLE_PYRAMID = StorageTypes.classNamed("io.hotmoka.examples.wtsc2020.SimplePyramid");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = new ConstructorSignature(SIMPLE_PYRAMID, StorageTypes.BIG_INTEGER);
	private static final MethodSignature INVEST = new VoidMethodSignature(SIMPLE_PYRAMID, "invest", StorageTypes.BIG_INTEGER);
	private static final MethodSignature MOST_FREQUENT_INVESTOR = new NonVoidMethodSignature(SIMPLE_PYRAMID, "mostFrequentInvestor", StorageTypes.PAYABLE_CONTRACT);
	private static final MethodSignature MOST_FREQUENT_INVESTOR_CLASS = new NonVoidMethodSignature(SIMPLE_PYRAMID, "mostFrequentInvestorClass", StorageTypes.STRING);
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _20_000_000 = BigInteger.valueOf(20_000_000);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("wtsc2020.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_20_000_000, _20_000_000, _20_000_000, _20_000_000);
	}

	@Test @DisplayName("two investors do not get their investment back yet")
	void twoInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks its balance
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), CodeSignature.BALANCE, account(0));

		// no money back yet
		assertEquals(BigInteger.valueOf(19_950_000), balance0.getValue());
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		addInstanceMethodCallTransaction(privateKey(2), account(2), _20_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks its balance
		BigIntegerValue balance0 = (BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), CodeSignature.BALANCE, account(0));

		// the money is back!
		assertEquals(balance0.getValue(), BigInteger.valueOf(20_006_666));
	}

	@Test @DisplayName("three investors then check most frequent investor class")
	void mostFrequentInvestorClass() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		addInstanceMethodCallTransaction(privateKey(2), account(2), _20_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(1) invests again and becomes the most frequent investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks which is the most frequent investor class
		StringValue result = (StringValue) runInstanceMethodCallTransaction(account(0), _50_000, jar(), MOST_FREQUENT_INVESTOR_CLASS, pyramid);

		assertEquals(StorageTypes.EOA.getName(), result.getValue());
	}

	@Test @DisplayName("three investors then check most frequent investor and fails")
	void mostFrequentInvestor() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		addInstanceMethodCallTransaction(privateKey(2), account(2), _20_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(1) invests again and becomes the most frequent investor
		addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks who is the most frequent investor
		throwsTransactionExceptionWithCauseAndMessageContaining(NonWhiteListedCallException.class, "cannot prove that equals() and hashCode() on this object are deterministic and terminating", () ->
			runInstanceMethodCallTransaction(account(0), _50_000, jar(), MOST_FREQUENT_INVESTOR, pyramid)
		);
	}
}