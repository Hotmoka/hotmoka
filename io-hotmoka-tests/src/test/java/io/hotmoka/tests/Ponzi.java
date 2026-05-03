/*
Copyright 2026 Fausto Spoto

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

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;

class Ponzi extends HotmokaTest {
	private static final ClassType PONZI = StorageTypes.classNamed("io.hotmoka.examples.ponzi.Ponzi");
	private static final ConstructorSignature PONZI_CONSTRUCTOR = ConstructorSignatures.of(PONZI);
	private static final VoidMethodSignature INVEST_METHOD = MethodSignatures.ofVoid(PONZI, "invest", StorageTypes.LONG);
	private static BigInteger GAS_PRICE = BigInteger.ONE;
	private static BigInteger GAS_LIMIT = _10_000_000;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("ponzi.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, _1_000_000_000, _1_000_000_000);
	}

	@Test
	@DisplayName("Create Ponzi, add two investors, half of last investment goes to the creator")
	void ThreeInvestorsHalfOfLastInvestmentGoesToCreator() throws Exception {
		// account(0) creates the contract and becomes the first investor
		var contract = addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, jar(), PONZI_CONSTRUCTOR);

		// we take note of the balance of account(0) after the creation of the contract
		BigInteger account0InitialBalance = getBalanceOf(account(0));

		// account(1) invests in the contract
		long firstInvestment = 10_000_000;
		addInstanceVoidMethodCallTransaction(privateKey(1), account(1), GAS_LIMIT, GAS_PRICE, jar(), INVEST_METHOD, contract, StorageValues.longOf(firstInvestment));

		// the first investment went to account(0)
		assertEquals(account0InitialBalance.add(BigInteger.valueOf(firstInvestment)), getBalanceOf(account(0)));

		// account(2) invests in the contract
		var secondInvestment = 2_000_000;
		addInstanceVoidMethodCallTransaction(privateKey(2), account(2), GAS_LIMIT, GAS_PRICE, jar(), INVEST_METHOD, contract, StorageValues.longOf(secondInvestment));

		// half of the second investment went to account(0)
		assertEquals(account0InitialBalance.add(BigInteger.valueOf(firstInvestment + secondInvestment / 2)), getBalanceOf(account(0)));
	}

	@Test
	@DisplayName("Create Ponzi, add an investor with a two small investment, get an exception")
	void InvestmentTooSmallThrowsException() throws Exception {
		// account(0) creates the contract and becomes the first investor
		var contract = addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, jar(), PONZI_CONSTRUCTOR);

		// account(1) invests too little in the contract
		long firstInvestment = 100;
		throwsTransactionExceptionWithCauseAndMessageContaining("io.takamaka.code.lang.RequirementViolationException", "you must invest at least 1000", () ->
			addInstanceVoidMethodCallTransaction(privateKey(1), account(1), GAS_LIMIT, GAS_PRICE, jar(), INVEST_METHOD, contract, StorageValues.longOf(firstInvestment)));
	}

	private BigInteger getBalanceOf(StorageReference account) throws Exception {
		return runInstanceNonVoidMethodCallTransaction(account, GAS_LIMIT, jar(), MethodSignatures.BALANCE, account)
					.asBigInteger(__ -> new ClassCastException());
	}
}