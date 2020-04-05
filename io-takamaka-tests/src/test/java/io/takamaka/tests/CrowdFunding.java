/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for the crowd funding contract.
 */
class CrowdFunding extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ClassType CROWD_FUNDING = new ClassType("io.takamaka.tests.crowdfunding.CrowdFunding");

	private static final ConstructorSignature CONSTRUCTOR_CROWD_FUNDING = new ConstructorSignature("io.takamaka.tests.crowdfunding.CrowdFunding");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000_000L);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference gamete;

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

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		mkBlockchain(ALL_FUNDS, BigInteger.ZERO, BigInteger.valueOf(10_000_000L), BigInteger.valueOf(10_000_000L));
		gamete = account(0);
		beneficiary = account(1);
		funder1 = account(2);
		funder2 = account(3);

		TransactionReference crowdfunding = addJarStoreTransaction
			(gamete, _20_000, BigInteger.ONE, takamakaCode(), bytesOf("crowdfunding.jar"), takamakaCode());

		classpath = new Classpath(crowdfunding, true);
		crowdFunding = addConstructorCallTransaction(gamete, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_CROWD_FUNDING);
	}

	@Test @DisplayName("new CrowdFunding().newCampaign(beneficiary, 50) == 0")
	void createCampaign() throws TransactionException, CodeExecutionException {
		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		assertEquals(0, id.value);
	}

	@Test @DisplayName("new CrowdFunding().newCampaign(beneficiary, 50) twice == 1")
	void createTwoCampaigns() throws TransactionException, CodeExecutionException {
		postInstanceMethodCallTransaction
			(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		assertEquals(1, id.value);
	}

	@Test @DisplayName("contributions are not enough then checkGoalReached yields false")
	void contributionsAreNotEnough() throws TransactionException, CodeExecutionException {
		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		postInstanceMethodCallTransaction
			(funder1, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING, "contribute", ClassType.BIG_INTEGER, INT),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), id);

		postInstanceMethodCallTransaction
			(funder2, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING, "contribute", ClassType.BIG_INTEGER, INT),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(1L)), id);

		BooleanValue reached = (BooleanValue) addInstanceMethodCallTransaction
			(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING, "checkGoalReached", BOOLEAN, INT),
			crowdFunding, id);

		assertFalse(reached.value);
	}

	@Test @DisplayName("contributions are enough then checkGoalReached yields false")
	void contributionsAreEnough() throws TransactionException, CodeExecutionException {
		IntValue id = (IntValue) addInstanceMethodCallTransaction
			(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING, "newCampaign", INT, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L)));

		postInstanceMethodCallTransaction
			(funder1, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING, "contribute", ClassType.BIG_INTEGER, INT),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), id);

		postInstanceMethodCallTransaction
			(funder2, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING, "contribute", ClassType.BIG_INTEGER, INT),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(2L)), id);

		BooleanValue reached = (BooleanValue) addInstanceMethodCallTransaction
			(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING, "checkGoalReached", BOOLEAN, INT),
			crowdFunding, id);

		assertTrue(reached.value);
	}
}