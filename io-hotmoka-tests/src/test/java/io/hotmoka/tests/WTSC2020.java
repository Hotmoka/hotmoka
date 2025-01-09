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
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the simple pyramid contract, used at the WTSC2020 workshop.
 */
class WTSC2020 extends HotmokaTest {
	private static final BigIntegerValue MINIMUM_INVESTMENT = StorageValues.bigIntegerOf(_50_000);
	private static final ClassType SIMPLE_PYRAMID = StorageTypes.classNamed("io.hotmoka.examples.wtsc2020.SimplePyramid");
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = ConstructorSignatures.of(SIMPLE_PYRAMID, StorageTypes.BIG_INTEGER);
	private static final VoidMethodSignature INVEST = MethodSignatures.ofVoid(SIMPLE_PYRAMID, "invest", StorageTypes.BIG_INTEGER);
	private static final NonVoidMethodSignature MOST_FREQUENT_INVESTOR = MethodSignatures.ofNonVoid(SIMPLE_PYRAMID, "mostFrequentInvestor", StorageTypes.PAYABLE_CONTRACT);
	private static final NonVoidMethodSignature MOST_FREQUENT_INVESTOR_CLASS = MethodSignatures.ofNonVoid(SIMPLE_PYRAMID, "mostFrequentInvestorClass", StorageTypes.STRING);
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
	void twoInvestors() throws Exception {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _50_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks its balance
		var balance0 = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), MethodSignatures.BALANCE, account(0));

		// no money back yet
		assertEquals(BigInteger.valueOf(19_950_000), balance0.getValue());
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws Exception {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _50_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		addInstanceVoidMethodCallTransaction(privateKey(2), account(2), _20_000, ZERO, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks its balance
		var balance0 = (BigIntegerValue) runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), MethodSignatures.BALANCE, account(0));

		// the money is back!
		assertEquals(balance0.getValue(), BigInteger.valueOf(20_006_666));
	}

	@Test @DisplayName("three investors then check most frequent investor class")
	void mostFrequentInvestorClass() throws Exception {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		addInstanceVoidMethodCallTransaction(privateKey(2), account(2), _20_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(1) invests again and becomes the most frequent investor
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks which is the most frequent investor class
		var result = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), MOST_FREQUENT_INVESTOR_CLASS, pyramid).asReturnedString(MOST_FREQUENT_INVESTOR_CLASS, __ -> new NodeException());

		assertEquals(StorageTypes.EOA.getName(), result);
	}

	@Test @DisplayName("three investors then check most frequent investor")
	void mostFrequentInvestor() throws Exception {
		// account(0) creates a SimplePyramid object in blockchain and becomes the first investor
		StorageReference pyramid = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT);

		// account(1) becomes the second investor
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(2) becomes the third investor
		addInstanceVoidMethodCallTransaction(privateKey(2), account(2), _20_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(1) invests again and becomes the most frequent investor
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), INVEST, pyramid, MINIMUM_INVESTMENT);

		// account(0) checks who is the most frequent investor
		StorageReference mostFrequent = runInstanceNonVoidMethodCallTransaction(account(0), _50_000, jar(), MOST_FREQUENT_INVESTOR, pyramid).asReturnedReference(MOST_FREQUENT_INVESTOR, __ -> new NodeException());

		// account(1) is the most frequent investor
		assertEquals(account(1), mostFrequent);
	}
}