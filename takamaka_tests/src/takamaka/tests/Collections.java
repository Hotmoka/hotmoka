/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.request.StaticMethodCallTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for the storage map Takamaka class.
 */
class Collections {

	private static final ClassType MAP_TESTS = new ClassType("takamaka.tests.collections.MapTests");

	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

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
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference collections = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _200_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/collections.jar")), takamakaBase));

		classpath = new Classpath(collections, true);
	}

	@Test @DisplayName("MapTests.testIteration1()")
	void geometricSum() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_TESTS, "testIteration1")));
		assertEquals(4950, sum.value);
	}

	@Test @DisplayName("MapTests.testUpdate1()")
	void geometricSumAfterUpdate() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_TESTS, "testUpdate1")));
		assertEquals(5050, sum.value);
	}
}