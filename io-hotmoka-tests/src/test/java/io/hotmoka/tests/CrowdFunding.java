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

import static io.hotmoka.beans.StorageTypes.BOOLEAN;
import static io.hotmoka.beans.StorageTypes.INT;
import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;

/**
 * A test for the crowd funding contract.
 */
class CrowdFunding extends HotmokaTest {
	private static final ClassType CROWD_FUNDING = StorageTypes.classNamed("io.hotmoka.examples.crowdfunding.CrowdFunding");
	private static final ConstructorSignature CONSTRUCTOR_CROWD_FUNDING = ConstructorSignatures.of(CROWD_FUNDING);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference account0;

	/**
	 * TYhe beneficiary of the crowd funding.
	 */
	private StorageReference beneficiary;

	/**
	 * A first funder.
	 */
	private StorageReference funder1;

	/**
	 * A second funder.
	 */
	private StorageReference funder2;

	/**
	 * The crowd funding contract.
	 */
	private StorageReference crowdFunding;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("crowdfunding.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000, BigInteger.ZERO, _10_000_000, _10_000_000);
		account0 = account(0);
		beneficiary = account(1);
		funder1 = account(2);
		funder2 = account(3);
		crowdFunding = addConstructorCallTransaction(privateKey(0), account0, _100_000, ONE, jar(), CONSTRUCTOR_CROWD_FUNDING);
	}

	@Test @DisplayName("new CrowdFunding().newCampaign(beneficiary, 50) == 0")
	void createCampaign() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _100_000, ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		assertEquals(0, id.getValue());
	}

	@Test @DisplayName("new CrowdFunding().newCampaign(beneficiary, 50) twice == 1")
	void createTwoCampaigns() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addInstanceMethodCallTransaction
			(privateKey(0), account0, _100_000, ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _100_000, ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		assertEquals(1, id.getValue());
	}

	@Test @DisplayName("contributions are not enough then checkGoalReached yields false")
	void contributionsAreNotEnough() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _100_000, ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		addInstanceMethodCallTransaction
			(privateKey(2), funder1, _100_000, ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING, "contribute", StorageTypes.BIG_INTEGER, INT),
			crowdFunding, StorageValues.bigIntegerOf(48L), id);

		addInstanceMethodCallTransaction
			(privateKey(3), funder2, _100_000, ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING, "contribute", StorageTypes.BIG_INTEGER, INT),
			crowdFunding, StorageValues.bigIntegerOf(1L), id);

		BooleanValue reached = (BooleanValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _100_000, ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING, "checkGoalReached", BOOLEAN, INT),
			crowdFunding, id);

		assertFalse(reached.getValue());
	}

	@Test @DisplayName("contributions are enough then checkGoalReached yields false")
	void contributionsAreEnough() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _100_000, ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		addInstanceMethodCallTransaction
			(privateKey(2), funder1, _100_000, ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING, "contribute", StorageTypes.BIG_INTEGER, INT),
			crowdFunding, StorageValues.bigIntegerOf(48L), id);

		addInstanceMethodCallTransaction
			(privateKey(3), funder2, _100_000, ONE, jar(),
			new VoidMethodSignature(CROWD_FUNDING, "contribute", StorageTypes.BIG_INTEGER, INT),
			crowdFunding, StorageValues.bigIntegerOf(2L), id);

		BooleanValue reached = (BooleanValue) addInstanceMethodCallTransaction
			(privateKey(0), account0, _100_000, ONE, jar(),
			new NonVoidMethodSignature(CROWD_FUNDING, "checkGoalReached", BOOLEAN, INT),
			crowdFunding, id);

		assertTrue(reached.getValue());
	}
}