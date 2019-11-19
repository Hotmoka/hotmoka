/**
 * 
 */
package takamaka.tests;

import static io.takamaka.code.blockchain.types.BasicTypes.BOOLEAN;
import static io.takamaka.code.blockchain.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.code.blockchain.AbstractSequentialBlockchain;
import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.ConstructorSignature;
import io.takamaka.code.blockchain.NonVoidMethodSignature;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.VoidMethodSignature;
import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.request.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.BigIntegerValue;
import io.takamaka.code.blockchain.values.BooleanValue;
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.memory.MemoryBlockchain;

/**
 * A test for the simplified crowd funding contract.
 */
class CrowdFundingSimplified extends TakamakaTest {

	private static final ClassType CAMPAIGN = new ClassType("io.takamaka.tests.crowdfunding.CrowdFundingSimplified$Campaign");

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ClassType CROWD_FUNDING_SIMPLIFIED = new ClassType("io.takamaka.tests.crowdfunding.CrowdFundingSimplified");

	private static final ConstructorSignature CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED = new ConstructorSignature("io.takamaka.tests.crowdfunding.CrowdFundingSimplified");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private AbstractSequentialBlockchain blockchain;

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

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference crowdfunding = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/crowdfunding.jar")), takamakaBase));

		classpath = new Classpath(crowdfunding, true);

		beneficiary = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _10_000, classpath, new ConstructorSignature(ClassType.EOA)));

		funder1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _10_000, classpath, new ConstructorSignature(ClassType.EOA, INT), new IntValue(10000)));

		funder2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _10_000, classpath, new ConstructorSignature(ClassType.EOA, INT), new IntValue(10000)));

		crowdFunding = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _10_000, classpath, CONSTRUCTOR_CROWD_FUNDING_SIMPLIFIED));
	}

	@Test @DisplayName("new CrowdFundingSimplified().newCampaign(beneficiary, 50) != null")
	void createCampaign() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		assertNotNull(campaign);
	}

	@Test @DisplayName("contributions are not enough then checkGoalReached yields false")
	void contributionsAreNotEnough() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder1, _10_000, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder2, _10_000, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(1L)), campaign));

		BooleanValue reached = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign));

		assertFalse(reached.value);
	}

	@Test @DisplayName("contributions are enough then checkGoalReached yields false")
	void contributionsAreEnough() throws TransactionException, CodeExecutionException {
		StorageReference campaign = (StorageReference) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "newCampaign", CAMPAIGN, ClassType.PAYABLE_CONTRACT, ClassType.BIG_INTEGER),
			crowdFunding, beneficiary, new BigIntegerValue(BigInteger.valueOf(50L))));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder1, _10_000, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(48L)), campaign));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(funder2, _10_000, classpath,
			new VoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "contribute", ClassType.BIG_INTEGER, CAMPAIGN),
			crowdFunding, new BigIntegerValue(BigInteger.valueOf(2L)), campaign));

		BooleanValue reached = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpath,
			new NonVoidMethodSignature(CROWD_FUNDING_SIMPLIFIED, "checkGoalReached", BOOLEAN, CAMPAIGN),
			crowdFunding, campaign));

		assertTrue(reached.value);
	}
}