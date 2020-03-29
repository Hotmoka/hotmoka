/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
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
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.memory.MemoryBlockchain;

/**
 * A test for the simple storage class.
 */
class Storage extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ClassType SIMPLE_STORAGE = new ClassType("io.takamaka.tests.storage.SimpleStorage");

	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_STORAGE = new ConstructorSignature("io.takamaka.tests.storage.SimpleStorage");

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private MemoryBlockchain blockchain;

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
		blockchain = mkMemoryBlockchain(ALL_FUNDS);
		gamete = blockchain.account(0);

		TransactionReference storage = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("storage.jar"), blockchain.takamakaCode()));

		classpath = new Classpath(storage, true);
	}

	@Test @DisplayName("new SimpleStorage().get() is an int")
	void neverInitializedStorageYieldsInt() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		StorageValue value = blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertTrue(value instanceof IntValue);
	}

	@Test @DisplayName("new SimpleStorage().get() == 0")
	void neverInitializedStorageYields0() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertEquals(value.value, 0);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then get() == 13")
	void set13ThenGet13() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(13)));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertEquals(value.value, 13);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then set(17) then get() == 17")
	void set13set17ThenGet17() throws TransactionException, CodeExecutionException {
		StorageReference storage = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_SIMPLE_STORAGE));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(13)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(SIMPLE_STORAGE, "set", INT), storage, new IntValue(17)));
		IntValue value = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, BigInteger.ONE, classpath, new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT), storage));
		assertEquals(value.value, 17);
	}
}