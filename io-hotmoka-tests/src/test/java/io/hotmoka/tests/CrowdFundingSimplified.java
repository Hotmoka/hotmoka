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

import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BooleanValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the simplified crowd funding contract.
 */
class CrowdFundingSimplified extends HotmokaTest {
	private static final ClassType CAMPAIGN = StorageTypes.classNamed("io.hotmoka.examples.crowdfunding.CrowdFundingSimplified$Campaign", IllegalArgumentException::new);
	private static final ClassType CROWD_FUNDING_SIMPLIFIED = StorageTypes.classNamed("io.hotmoka.examples.crowdfunding.CrowdFundingSimplified", IllegalArgumentException::new);
	private static final ConstructorSignature CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED = ConstructorSignatures.of(CROWD_FUNDING_SIMPLIFIED);

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
		crowdFunding = addConstructorCallTransaction(privateKey(0), account0, _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED);
	}

	@Test @DisplayName("new CrowdFundingSimplified().newCampaign(beneficiary, 50) != null")
	void createCampaign() throws Exception {
		StorageReference campaign = (StorageReference) addInstanceNonVoidMethodCallTransaction
			(privateKey(0), account0, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofNonVoid(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		assertNotNull(campaign);
	}

	@Test @DisplayName("contributions are not enough then checkGoalReached yields false")
	void contributionsAreNotEnough() throws Exception {
		var campaign = (StorageReference) addInstanceNonVoidMethodCallTransaction
			(privateKey(0), account0, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofNonVoid(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		addInstanceVoidMethodCallTransaction
			(privateKey(2), funder1, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofVoid(CROWD_FUNDING_SIMPLIFIED, "contribute", StorageTypes.BIG_INTEGER, CAMPAIGN),
			crowdFunding, StorageValues.bigIntegerOf(48L), campaign);

		addInstanceVoidMethodCallTransaction
			(privateKey(3), funder2, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofVoid(CROWD_FUNDING_SIMPLIFIED, "contribute", StorageTypes.BIG_INTEGER, CAMPAIGN),
			crowdFunding, StorageValues.bigIntegerOf(1L), campaign);

		var reached = (BooleanValue) addInstanceNonVoidMethodCallTransaction
			(privateKey(0), account0, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofNonVoid(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign);

		assertFalse(reached.getValue());
	}

	@Test @DisplayName("contributions are enough then checkGoalReached yields false")
	void contributionsAreEnough() throws Exception {
		var campaign = (StorageReference) addInstanceNonVoidMethodCallTransaction
			(privateKey(0), account0, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofNonVoid(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, StorageTypes.PAYABLE_CONTRACT, StorageTypes.BIG_INTEGER),
			crowdFunding, beneficiary, StorageValues.bigIntegerOf(50L));

		addInstanceVoidMethodCallTransaction
			(privateKey(2), funder1, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofVoid(CROWD_FUNDING_SIMPLIFIED, "contribute", StorageTypes.BIG_INTEGER, CAMPAIGN),
			crowdFunding, StorageValues.bigIntegerOf(48L), campaign);

		addInstanceVoidMethodCallTransaction
			(privateKey(3), funder2, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofVoid(CROWD_FUNDING_SIMPLIFIED, "contribute", StorageTypes.BIG_INTEGER, CAMPAIGN),
			crowdFunding, StorageValues.bigIntegerOf(2L), campaign);

		var reached = (BooleanValue) addInstanceNonVoidMethodCallTransaction
			(privateKey(0), account0, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.ofNonVoid(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign);

		assertTrue(reached.getValue());
	}
}