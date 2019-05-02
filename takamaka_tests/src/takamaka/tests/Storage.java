/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for the remote purchase contract.
 */
class Storage {

	private static final BigInteger _1_000 = BigInteger.valueOf(1000);

	private static final ClassType SIMPLE_STORAGE = new ClassType("takamaka.tests.storage.SimpleStorage");

	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_STORAGE = new ConstructorSignature("takamaka.tests.storage.SimpleStorage");

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

		TransactionReference storage = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/storage.jar")), takamakaBase));

		classpath = new Classpath(storage, true);
	}

	@Test @DisplayName("new SimpleStorage().get() is an int")
	void neverInitializedStorageYieldsInt() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		StorageValue value = blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(SIMPLE_STORAGE, "get"), storage));
		assertTrue(value instanceof IntValue);
	}

	@Test @DisplayName("new SimpleStorage().get() == 0")
	void neverInitializedStorageYields0() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(SIMPLE_STORAGE, "get"), storage));
		assertEquals(value.value, 0);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then get() == 13")
	void set13ThenGet13() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(13)));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(SIMPLE_STORAGE, "get"), storage));
		assertEquals(value.value, 13);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then set(17) then get() == 17")
	void set13set17ThenGet17() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _1_000, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(13)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(17)));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _1_000, classpath, new MethodSignature(SIMPLE_STORAGE, "get"), storage));
		assertEquals(value.value, 17);
	}
}