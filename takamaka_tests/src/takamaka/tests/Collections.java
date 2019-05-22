/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import takamaka.blockchain.request.StaticMethodCallTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.BooleanValue;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StorageValue;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for the storage map Takamaka class.
 */
class Collections {

	private static final ClassType MAP_TESTS = new ClassType("takamaka.tests.collections.MapTests");
	private static final ClassType INT_MAP_TESTS = new ClassType("takamaka.tests.collections.IntMapTests");
	private static final ClassType ARRAY_TESTS = new ClassType("takamaka.tests.collections.ArrayTests");
	private static final ClassType MAP_HOLDER = new ClassType("takamaka.tests.collections.MapHolder");
	private static final ClassType STATE = new ClassType("takamaka.tests.collections.MapHolder$State");
	private static final ClassType COMPARABLE = new ClassType("java.lang.Comparable");

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

	@Test @DisplayName("MapTests.testIteration1() == 4950")
	void geometricSum() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_TESTS, "testIteration1")));
		assertEquals(4950, sum.value);
	}

	@Test @DisplayName("MapTests.testUpdate1() == 5050")
	void geometricSumAfterUpdate() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_TESTS, "testUpdate1")));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("MapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateWithStream() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_TESTS, "testUpdate2")));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("MapTests.testNullValues() == 100L()")
	void nullValuesInMap() throws TransactionException, CodeExecutionException {
		LongValue count = (LongValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_TESTS, "testNullValues")));
		assertEquals(100L, count.value);
	}

	@Test @DisplayName("IntMapTests.testIteration1() == 4950")
	void geometricSumIntKeys() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(INT_MAP_TESTS, "testIteration1")));
		assertEquals(4950, sum.value);
	}

	@Test @DisplayName("IntMapTests.testUpdate1() == 5050")
	void geometricSumAfterUpdateIntKeys() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(INT_MAP_TESTS, "testUpdate1")));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("IntMapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateIntKeysWithStream() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(INT_MAP_TESTS, "testUpdate2")));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("IntMapTests.testNullValues() == 100L()")
	void nullValuesInMapIntKeys() throws TransactionException, CodeExecutionException {
		LongValue count = (LongValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(INT_MAP_TESTS, "testNullValues")));
		assertEquals(100L, count.value);
	}

	@Test @DisplayName("ArrayTests.testRandomInitialization() == 1225")
	void randomArray() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(ARRAY_TESTS, "testRandomInitialization")));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("ArrayTests.countNullsAfterRandomInitialization() == 50L")
	void randomArrayCountNulls() throws TransactionException, CodeExecutionException {
		LongValue count = (LongValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(ARRAY_TESTS, "countNullsAfterRandomInitialization")));
		assertEquals(50L, count.value);
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault1() == 1325")
	void randomArrayThenUpdate1() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(ARRAY_TESTS, "testUpdateWithDefault1")));
		assertEquals(1325, sum.value);
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault2() == 1225")
	void randomArrayThenUpdate2() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(ARRAY_TESTS, "testUpdateWithDefault2")));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("ArrayTests.testGetOrDefault() == 1225")
	void randomArrayTheGetOrDefault() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(ARRAY_TESTS, "testGetOrDefault")));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("new MapHolder()")
	void mapHolder() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING")
	void mapHolderGet0() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get0"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isRunning", ClassType.OBJECT), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING")
	void mapHolderGet1() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get1"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isSleeping", ClassType.OBJECT), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING")
	void mapHolderGet10() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get10"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isWaiting", ClassType.OBJECT), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with State")
	void mapHolderGet0State() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get0"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isRunning2", STATE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with State")
	void mapHolderGet1State() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get1"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isSleeping2", STATE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with State")
	void mapHolderGet10State() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get10"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isWaiting2", STATE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with Comparable")
	void mapHolderGet0Comparable() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get0"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isRunning3", COMPARABLE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with Comparable")
	void mapHolderGet1Comparable() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get1"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isSleeping3", COMPARABLE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with Comparable")
	void mapHolderGet10Comparable() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _200_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "get10"), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _200_000, classpath, new MethodSignature(MAP_HOLDER, "isWaiting3", COMPARABLE), mapHolder, state));

		assertTrue(result.value);
	}
}