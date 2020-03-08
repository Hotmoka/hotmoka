/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.nodes.CodeExecutionException;

/**
 * A test for the simplified crowd funding contract.
 */
class CrowdFundingSimplified extends TakamakaTest {

	private static final ClassType CAMPAIGN = new ClassType("io.takamaka.tests.crowdfunding.CrowdFundingSimplified$Campaign");

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ClassType CROWD_FUNDING_SIMPLIFIED = new ClassType("io.takamaka.tests.crowdfunding.CrowdFundingSimplified");

	private static final ConstructorSignature CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED = new ConstructorSignature("io.takamaka.tests.crowdfunding.CrowdFundingSimplified");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000_000L);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private MemoryBlockchain blockchain;

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
		blockchain = MemoryBlockchain.of(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"),
			ALL_FUNDS, BigInteger.ZERO, BigInteger.valueOf(10_000_000L), BigInteger.valueOf(10_000_000L));
		gamete = blockchain.account(0);
		beneficiary = blockchain.account(1);
		funder1 = blockchain.account(2);
		funder2 = blockchain.account(3);

		TransactionReference crowdfunding = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			Files.readAllBytes(Paths.get("../io-takamaka-examples/target/io-takamaka-examples-1.0-crowdfunding.jar")), blockchain.takamakaCode()));

		classpath = new Classpath(crowdfunding, true);

		crowdFunding = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED));
	}

	@Test @DisplayName("new CrowdFundingSimplified().newCampaign(beneficiary, 50) != null")
	void createCampaign() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		assertNotNull(campaign);
	}

	@Test @DisplayName("contributions are not enough then checkGoalReached yields false")
	void contributionsAreNotEnough() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder1, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder2, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(1L)), campaign));

		BooleanValue reached = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign));

		assertFalse(reached.value);
	}

	@Test @DisplayName("contributions are enough then checkGoalReached yields false")
	void contributionsAreEnough() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder1, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder2, _10_000, BigInteger.ONE, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(2L)), campaign));

		BooleanValue reached = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign));

		assertTrue(reached.value);
	}
}