/**
 * 
 */
package takamaka.tests;

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
import takamaka.blockchain.values.StorageReference;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for a class that uses lambda expressions referring to entries.
 */
class Lambdas {

	private static final BigInteger _1_000 = BigInteger.valueOf(1000);

	private static final ClassType LAMBDAS = new ClassType("takamaka.tests.lambdas.Lambdas");

	private static final ConstructorSignature CONSTRUCTOR_LAMBDAS = new ConstructorSignature("takamaka.tests.lambdas.Lambdas", ClassType.BIG_INTEGER);

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
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference lambdas = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/lambdas.jar")), takamakaBase));

		classpath = new Classpath(lambdas, true);
	}

	@Test @DisplayName("new Lambdas()")
	void createLambdas() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_20_000)));
	}

	@Test @DisplayName("new Lambdas().invest(10)")
	void createLambdasThenInvest10() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_20_000)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _20_000, classpath, new MethodSignature(LAMBDAS, "invest", ClassType.BIG_INTEGER),
			lambdas, new BigIntegerValue(BigInteger.ONE)));
	}
}