/**
 * 
 */
package takamaka.tests;

import static io.takamaka.code.blockchain.types.BasicTypes.BOOLEAN;
import static io.takamaka.code.blockchain.types.BasicTypes.INT;
import static io.takamaka.code.blockchain.types.BasicTypes.LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.request.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.request.StaticMethodCallTransactionRequest;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.BooleanValue;
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.LongValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StorageValue;
import io.takamaka.code.memory.MemoryBlockchain;

/**
 * A test for the storage map Takamaka class.
 */
class Collections extends TakamakaTest {

	private static final ClassType MAP_TESTS = new ClassType("io.takamaka.tests.collections.MapTests");
	private static final ClassType INT_MAP_TESTS = new ClassType("io.takamaka.tests.collections.IntMapTests");
	private static final ClassType ARRAY_TESTS = new ClassType("io.takamaka.tests.collections.ArrayTests");
	private static final ClassType MAP_HOLDER = new ClassType("io.takamaka.tests.collections.MapHolder");
	private static final ClassType STATE = new ClassType("io.takamaka.tests.collections.MapHolder$State");
	private static final ClassType COMPARABLE = new ClassType("java.lang.Comparable");

	private static final BigInteger _5_000_000 = BigInteger.valueOf(5_000_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(10_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private AbstractSequentialBlockchain blockchain;

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

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference collections = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _5_000_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/collections.jar")), takamakaBase));

		classpath = new Classpath(collections, true);
	}

	@Test @DisplayName("MapTests.testIteration1() == 4950")
	void geometricSum() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_TESTS, "testIteration1", INT)));
		assertEquals(4950, sum.value);
	}

	@Test @DisplayName("MapTests.testUpdate1() == 5050")
	void geometricSumAfterUpdate() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_TESTS, "testUpdate1", INT)));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("MapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateWithStream() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_TESTS, "testUpdate2", INT)));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("MapTests.testNullValues() == 100L()")
	void nullValuesInMap() throws TransactionException, CodeExecutionException {
		LongValue count = (LongValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_TESTS, "testNullValues", LONG)));
		assertEquals(100L, count.value);
	}

	@Test @DisplayName("IntMapTests.testIteration1() == 4950")
	void geometricSumIntKeys() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(INT_MAP_TESTS, "testIteration1", INT)));
		assertEquals(4950, sum.value);
	}

	@Test @DisplayName("IntMapTests.testUpdate1() == 5050")
	void geometricSumAfterUpdateIntKeys() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(INT_MAP_TESTS, "testUpdate1", INT)));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("IntMapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateIntKeysWithStream() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(INT_MAP_TESTS, "testUpdate2", INT)));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("IntMapTests.testNullValues() == 100L()")
	void nullValuesInMapIntKeys() throws TransactionException, CodeExecutionException {
		LongValue count = (LongValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(INT_MAP_TESTS, "testNullValues", LONG)));
		assertEquals(100L, count.value);
	}

	@Test @DisplayName("ArrayTests.testRandomInitialization() == 1225")
	void randomArray() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(ARRAY_TESTS, "testRandomInitialization", INT)));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("ArrayTests.countNullsAfterRandomInitialization() == 50L")
	void randomArrayCountNulls() throws TransactionException, CodeExecutionException {
		LongValue count = (LongValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(ARRAY_TESTS, "countNullsAfterRandomInitialization", LONG)));
		assertEquals(50L, count.value);
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault1() == 1325")
	void randomArrayThenUpdate1() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(ARRAY_TESTS, "testUpdateWithDefault1", INT)));
		assertEquals(1325, sum.value);
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault2() == 1225")
	void randomArrayThenUpdate2() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(ARRAY_TESTS, "testUpdateWithDefault2", INT)));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("ArrayTests.testGetOrDefault() == 1225")
	void randomArrayTheGetOrDefault() throws TransactionException, CodeExecutionException {
		IntValue sum = (IntValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(ARRAY_TESTS, "testGetOrDefault", INT)));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("new MapHolder()")
	void mapHolder() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING")
	void mapHolderGet0() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get0", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isRunning", BOOLEAN, ClassType.OBJECT), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING")
	void mapHolderGet1() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get1", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isSleeping", BOOLEAN, ClassType.OBJECT), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING")
	void mapHolderGet10() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get10", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isWaiting", BOOLEAN, ClassType.OBJECT), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with State")
	void mapHolderGet0State() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get0", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isRunning2", BOOLEAN, STATE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with State")
	void mapHolderGet1State() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get1", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isSleeping2", BOOLEAN, STATE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with State")
	void mapHolderGet10State() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get10", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isWaiting2", BOOLEAN, STATE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with Comparable")
	void mapHolderGet0Comparable() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get0", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isRunning3", BOOLEAN, COMPARABLE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with Comparable")
	void mapHolderGet1Comparable() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get1", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isSleeping3", BOOLEAN, COMPARABLE), mapHolder, state));

		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with Comparable")
	void mapHolderGet10Comparable() throws TransactionException, CodeExecutionException {
		StorageReference mapHolder = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _5_000_000, classpath, new ConstructorSignature(MAP_HOLDER)));

		StorageValue state = blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "get10", STATE), mapHolder));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(gamete, _5_000_000, classpath, new NonVoidMethodSignature(MAP_HOLDER, "isWaiting3", BOOLEAN, COMPARABLE), mapHolder, state));

		assertTrue(result.value);
	}
}