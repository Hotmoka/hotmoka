/**
 * 
 */
package io.hotmoka.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static io.hotmoka.beans.types.BasicTypes.LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A test for the storage map Takamaka class.
 */
class Collections extends TakamakaTest {
	private static final ClassType MAP_TESTS = new ClassType("io.hotmoka.examples.collections.MapTests");
	private static final ClassType INT_MAP_TESTS = new ClassType("io.hotmoka.examples.collections.IntMapTests");
	private static final ClassType ARRAY_TESTS = new ClassType("io.hotmoka.examples.collections.ArrayTests");
	private static final ClassType MAP_HOLDER = new ClassType("io.hotmoka.examples.collections.MapHolder");
	private static final ClassType STATE = new ClassType("io.hotmoka.examples.collections.MapHolder$State");
	private static final ClassType COMPARABLE = new ClassType("java.lang.Comparable");

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference eoa;

	/**
	 * The private key of {@linkplain #eoa}.
	 */
	private PrivateKey key;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("collections.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
		eoa = account(0);
		key = privateKey(0);
	}

	@Test @DisplayName("MapTests.testIteration1() == 4950")
	void geometricSum() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_TESTS, "testIteration1", INT));
		assertEquals(4950, sum.value);
	}

	@Test @DisplayName("MapTests.testUpdate1() == 5050")
	void geometricSumAfterUpdate() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_TESTS, "testUpdate1", INT));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("MapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateWithStream() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_TESTS, "testUpdate2", INT));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("MapTests.testNullValues() == 100L")
	void nullValuesInMap() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		LongValue count = (LongValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_TESTS, "testNullValues", LONG));
		assertEquals(100L, count.value);
	}

	@Test @DisplayName("IntMapTests.testIteration1() == 4950")
	void geometricSumIntKeys() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(INT_MAP_TESTS, "testIteration1", INT));
		assertEquals(4950, sum.value);
	}

	@Test @DisplayName("IntMapTests.testUpdate1() == 5050")
	void geometricSumAfterUpdateIntKeys() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(INT_MAP_TESTS, "testUpdate1", INT));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("IntMapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateIntKeysWithStream() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(INT_MAP_TESTS, "testUpdate2", INT));
		assertEquals(5050, sum.value);
	}

	@Test @DisplayName("IntMapTests.testNullValues() == 100L()")
	void nullValuesInMapIntKeys() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		LongValue count = (LongValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(INT_MAP_TESTS, "testNullValues", LONG));
		assertEquals(100L, count.value);
	}

	@Test @DisplayName("ArrayTests.testRandomInitialization() == 1225")
	void randomArray() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(ARRAY_TESTS, "testRandomInitialization", INT));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("ArrayTests.countNullsAfterRandomInitialization() == 50L")
	void randomArrayCountNulls() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		LongValue count = (LongValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(ARRAY_TESTS, "countNullsAfterRandomInitialization", LONG));
		assertEquals(50L, count.value);
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault1() == 1325")
	void randomArrayThenUpdate1() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(ARRAY_TESTS, "testUpdateWithDefault1", INT));
		assertEquals(1325, sum.value);
	}

	@Test @DisplayName("ArrayTests.testByteArrayThenIncrease() == 1375")
	void randomArrayThenIncrease() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(ARRAY_TESTS, "testByteArrayThenIncrease", INT));
		assertEquals(1375, sum.value);
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault2() == 1225")
	void randomArrayThenUpdate2() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(ARRAY_TESTS, "testUpdateWithDefault2", INT));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("ArrayTests.testGetOrDefault() == 1225")
	void randomArrayTheGetOrDefault() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		IntValue sum = (IntValue) runStaticMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(ARRAY_TESTS, "testGetOrDefault", INT));
		assertEquals(1225, sum.value);
	}

	@Test @DisplayName("new MapHolder()")
	void mapHolder() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING")
	void mapHolderGet0() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get0", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isRunning", BOOLEAN, ClassType.OBJECT), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING")
	void mapHolderGet1() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get1", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isSleeping", BOOLEAN, ClassType.OBJECT), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING")
	void mapHolderGet10() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get10", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isWaiting", BOOLEAN, ClassType.OBJECT), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with State")
	void mapHolderGet0State() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get0", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isRunning2", BOOLEAN, STATE), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with State")
	void mapHolderGet1State() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get1", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isSleeping2", BOOLEAN, STATE), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with State")
	void mapHolderGet10State() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get10", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isWaiting2", BOOLEAN, STATE), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with Comparable")
	void mapHolderGet0Comparable() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get0", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isRunning3", BOOLEAN, COMPARABLE), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with Comparable")
	void mapHolderGet1Comparable() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get1", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isSleeping3", BOOLEAN, COMPARABLE), mapHolder, state);
		assertTrue(result.value);
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with Comparable")
	void mapHolderGet10Comparable() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), new ConstructorSignature(MAP_HOLDER));
		StorageValue state = runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "get10", STATE), mapHolder);
		BooleanValue result = (BooleanValue) runInstanceMethodCallTransaction(eoa, _10_000_000, jar(), new NonVoidMethodSignature(MAP_HOLDER, "isWaiting3", BOOLEAN, COMPARABLE), mapHolder, state);
		assertTrue(result.value);
	}
}