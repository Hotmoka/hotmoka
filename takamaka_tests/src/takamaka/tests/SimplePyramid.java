/**
 * 
 */
package takamaka.tests;

import static io.takamaka.code.blockchain.types.BasicTypes.INT;
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
import io.takamaka.code.blockchain.MethodSignature;
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
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.memory.MemoryBlockchain;

/**
 * A test for the simple pyramid contract.
 */
class SimplePyramid extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final BigIntegerValue MINIMUM_INVESTMENT = new BigIntegerValue(BigInteger.valueOf(10_000L));

	private static final ClassType SIMPLE_PYRAMID = new ClassType("io.takamaka.tests.ponzi.SimplePyramid");

	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_PYRAMID = new ConstructorSignature(SIMPLE_PYRAMID, ClassType.BIG_INTEGER);

	private static final MethodSignature INVEST = new VoidMethodSignature(SIMPLE_PYRAMID, "invest", ClassType.BIG_INTEGER);

	private static final MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private AbstractSequentialBlockchain blockchain;

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference gamete;

	/**
	 * The four participants to the pyramid.
	 */
	private StorageReference[] players = new StorageReference[4];

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest
				(Files.readAllBytes(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference ponzi = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/ponzi.jar")), takamakaBase));

		classpath = new Classpath(ponzi, true);

		for (int i = 0; i < 4; i++)
			players[i] = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, _10_000, classpath, new ConstructorSignature(ClassType.TEOA, INT), new IntValue(20_000)));
	}

	@Test @DisplayName("two investors do not get investment back yet")
	void twoInvestors() throws TransactionException, CodeExecutionException {
		StorageReference pyramid = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(players[0], _10_000, classpath, CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[1], _10_000, classpath, INVEST, pyramid, MINIMUM_INVESTMENT));
		BigIntegerValue balance0 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[0], _10_000, classpath, GET_BALANCE, players[0]));
		assertTrue(balance0.value.compareTo(_10_000) <= 0);
	}

	@Test @DisplayName("with three investors the first gets its investment back")
	void threeInvestors() throws TransactionException, CodeExecutionException {
		StorageReference pyramid = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(players[0], _10_000, classpath, CONSTRUCTOR_SIMPLE_PYRAMID, MINIMUM_INVESTMENT));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[1], _10_000, classpath, INVEST, pyramid, MINIMUM_INVESTMENT));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[2], _20_000, classpath, INVEST, pyramid, MINIMUM_INVESTMENT));
		BigIntegerValue balance0 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(players[0], _10_000, classpath, GET_BALANCE, players[0]));
		assertTrue(balance0.value.compareTo(_20_000) > 0);
	}
}