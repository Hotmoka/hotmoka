/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static takamaka.blockchain.types.BasicTypes.INT;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BigIntegerValue;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for the simplified crowd funding contract.
 */
class CrowdFundingSimplified {

	private static final ClassType CAMPAIGN = new ClassType("takamaka.tests.crowdfunding.CrowdFundingSimplified$Campaign");

	private static final BigInteger _1_000 = BigInteger.valueOf(1000);

	private static final ClassType CROWD_FUNDING_SIMPLIFIED = new ClassType("takamaka.tests.crowdfunding.CrowdFundingSimplified");

	private static final ConstructorSignature CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED = new ConstructorSignature("takamaka.tests.crowdfunding.CrowdFundingSimplified");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private Blockchain blockchain;

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
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference crowdfunding = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/crowdfunding.jar")), takamakaBase));

		classpath = new Classpath(crowdfunding, true);

		beneficiary = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount")));

		funder1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount", INT), new IntValue(1000)));

		funder2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount", INT), new IntValue(1000)));

		crowdFunding = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED));
	}

	@Test @DisplayName("new CrowdFundingSimplified().newCampaign(beneficiary, 50) != null")
	void createCampaign() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", new ClassType("takamaka.lang.PayableContract"), ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		assertNotNull(campaign);
	}

	@Test @DisplayName("contributions are not enough then checkGoalReached yields false")
	void contributionsAreNotEnough() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", new ClassType("takamaka.lang.PayableContract"), ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder1, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder2, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(1L)), campaign));

		BooleanValue reached = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", CAMPAIGN),
			crowdFunding, campaign));

		assertFalse(reached.value);
	}

	@Test @DisplayName("contributions are enough then checkGoalReached yields false")
	void contributionsAreEnough() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", new ClassType("takamaka.lang.PayableContract"), ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder1, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder2, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(2L)), campaign));

		BooleanValue reached = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath,
			new MethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", CAMPAIGN),
			crowdFunding, campaign));

		assertTrue(reached.value);
	}
}