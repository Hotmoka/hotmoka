/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tests;

import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageTypes.LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A test for the storage map Takamaka class.
 */
class Collections extends HotmokaTest {
	private static final ClassType MAP_TESTS = StorageTypes.classNamed("io.hotmoka.examples.collections.MapTests", IllegalArgumentException::new);
	private static final ClassType INT_MAP_TESTS = StorageTypes.classNamed("io.hotmoka.examples.collections.IntMapTests", IllegalArgumentException::new);
	private static final ClassType ARRAY_TESTS = StorageTypes.classNamed("io.hotmoka.examples.collections.ArrayTests", IllegalArgumentException::new);
	private static final ClassType SET_TESTS = StorageTypes.classNamed("io.hotmoka.examples.collections.SetTests", IllegalArgumentException::new);
	private static final ClassType MAP_HOLDER = StorageTypes.classNamed("io.hotmoka.examples.collections.MapHolder", IllegalArgumentException::new);
	private static final ClassType STATE = StorageTypes.classNamed("io.hotmoka.examples.collections.MapHolder$State", IllegalArgumentException::new);
	private static final ClassType COMPARABLE = StorageTypes.classNamed("java.lang.Comparable", IllegalArgumentException::new);

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
	void geometricSum() throws Exception {
		var testIteration1 = MethodSignatures.ofNonVoid(MAP_TESTS, "testIteration1", INT);
		assertEquals(4950, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testIteration1).asReturnedInt(testIteration1, NodeException::new));
	}

	@Test @DisplayName("MapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateWithStream() throws Exception {
		var testUpdate2 = MethodSignatures.ofNonVoid(MAP_TESTS, "testUpdate2", INT);
		assertEquals(5050, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testUpdate2).asReturnedInt(testUpdate2, NodeException::new));
	}

	@Test @DisplayName("MapTests.testNullValues() == 100L")
	void nullValuesInMap() throws Exception {
		var method = MethodSignatures.ofNonVoid(MAP_TESTS, "testNullValues", LONG);
		assertEquals(100L, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), method).asReturnedLong(method, NodeException::new));
	}

	@Test @DisplayName("IntMapTests.testIteration1() == 4950")
	void geometricSumIntKeys() throws Exception {
		var testIteration1 = MethodSignatures.ofNonVoid(INT_MAP_TESTS, "testIteration1", INT);
		assertEquals(4950, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testIteration1).asReturnedInt(testIteration1, NodeException::new));
	}

	@Test @DisplayName("IntMapTests.testUpdate2() == 5050")
	void geometricSumAfterUpdateIntKeysWithStream() throws Exception {
		var method = MethodSignatures.ofNonVoid(INT_MAP_TESTS, "testUpdate2", INT);
		assertEquals(5050, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), method).asReturnedInt(method, NodeException::new));
	}

	@Test @DisplayName("IntMapTests.testNullValues() == 100L()")
	void nullValuesInMapIntKeys() throws Exception {
		var testNullValues = MethodSignatures.ofNonVoid(INT_MAP_TESTS, "testNullValues", LONG);
		assertEquals(100L, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testNullValues).asReturnedLong(testNullValues, NodeException::new));
	}

	@Test @DisplayName("ArrayTests.testRandomInitialization() == 1225")
	void randomArray() throws Exception {
		var testRandomInitialization = MethodSignatures.ofNonVoid(ARRAY_TESTS, "testRandomInitialization", INT);
		assertEquals(1225, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testRandomInitialization).asReturnedInt(testRandomInitialization, NodeException::new));
	}

	@Test @DisplayName("ArrayTests.countNullsAfterRandomInitialization() == 50L")
	void randomArrayCountNulls() throws Exception {
		NonVoidMethodSignature countNullsAfterRandomInitialization = MethodSignatures.ofNonVoid(ARRAY_TESTS, "countNullsAfterRandomInitialization", LONG);
		assertEquals(50L, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), countNullsAfterRandomInitialization).asReturnedLong(countNullsAfterRandomInitialization, NodeException::new));
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault1() == 1325")
	void randomArrayThenUpdate1() throws Exception {
		var testUpdateWithDefault1 = MethodSignatures.ofNonVoid(ARRAY_TESTS, "testUpdateWithDefault1", INT);
		assertEquals(1325, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testUpdateWithDefault1).asReturnedInt(testUpdateWithDefault1, NodeException::new));
	}

	@Test @DisplayName("ArrayTests.testByteArrayThenIncrease() == 1375")
	void randomArrayThenIncrease() throws Exception {
		var testByteArrayThenIncrease = MethodSignatures.ofNonVoid(ARRAY_TESTS, "testByteArrayThenIncrease", INT);
		assertEquals(1375, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testByteArrayThenIncrease).asReturnedInt(testByteArrayThenIncrease, NodeException::new));
	}

	@Test @DisplayName("ArrayTests.testUpdateWithDefault2() == 1225")
	void randomArrayThenUpdate2() throws Exception {
		var testUpdateWithDefault2 = MethodSignatures.ofNonVoid(ARRAY_TESTS, "testUpdateWithDefault2", INT);
		assertEquals(1225, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testUpdateWithDefault2).asReturnedInt(testUpdateWithDefault2, NodeException::new));
	}

	@Test @DisplayName("ArrayTests.testGetOrDefault() == 1225")
	void randomArrayTheGetOrDefault() throws Exception {
		var testGetOrDefault = MethodSignatures.ofNonVoid(ARRAY_TESTS, "testGetOrDefault", INT);
		assertEquals(1225, runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testGetOrDefault).asReturnedInt(testGetOrDefault, NodeException::new));
	}

	@Test @DisplayName("SetTests.testRandomInitialization() == true")
	void randomRandomSetInitialization() throws Exception {
		var testRandomInitialization = MethodSignatures.ofNonVoid(SET_TESTS, "testRandomInitialization", BOOLEAN);
		assertTrue(runStaticNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), testRandomInitialization).asReturnedBoolean(testRandomInitialization, NodeException::new));
	}

	@Test @DisplayName("new MapHolder()")
	void mapHolder() throws Exception {
		addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING")
	void mapHolderGet0() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get0", STATE), mapHolder);
		var isRunning = MethodSignatures.ofNonVoid(MAP_HOLDER, "isRunning", BOOLEAN, StorageTypes.OBJECT);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isRunning, mapHolder, state).asReturnedBoolean(isRunning, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING")
	void mapHolderGet1() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get1", STATE), mapHolder);
		var isSleeping = MethodSignatures.ofNonVoid(MAP_HOLDER, "isSleeping", BOOLEAN, StorageTypes.OBJECT);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isSleeping, mapHolder, state).asReturnedBoolean(isSleeping, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING")
	void mapHolderGet10() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get10", STATE), mapHolder);
		var isWaiting = MethodSignatures.ofNonVoid(MAP_HOLDER, "isWaiting", BOOLEAN, StorageTypes.OBJECT);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isWaiting, mapHolder, state).asReturnedBoolean(isWaiting, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with State")
	void mapHolderGet0State() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get0", STATE), mapHolder);
		var isRunning2 = MethodSignatures.ofNonVoid(MAP_HOLDER, "isRunning2", BOOLEAN, STATE);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isRunning2, mapHolder, state).asReturnedBoolean(isRunning2, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with State")
	void mapHolderGet1State() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get1", STATE), mapHolder);
		var isSleeping2 = MethodSignatures.ofNonVoid(MAP_HOLDER, "isSleeping2", BOOLEAN, STATE);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isSleeping2, mapHolder, state).asReturnedBoolean(isSleeping2, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with State")
	void mapHolderGet10State() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get10", STATE), mapHolder);
		var isWaiting2 = MethodSignatures.ofNonVoid(MAP_HOLDER, "isWaiting2", BOOLEAN, STATE);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isWaiting2, mapHolder, state).asReturnedBoolean(isWaiting2, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get0() == RUNNING with Comparable")
	void mapHolderGet0Comparable() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get0", STATE), mapHolder);
		NonVoidMethodSignature isRunning3 = MethodSignatures.ofNonVoid(MAP_HOLDER, "isRunning3", BOOLEAN, COMPARABLE);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isRunning3, mapHolder, state).asReturnedBoolean(isRunning3, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get1() == SLEEPING with Comparable")
	void mapHolderGet1Comparable() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get1", STATE), mapHolder);
		var isSleeping3 = MethodSignatures.ofNonVoid(MAP_HOLDER, "isSleeping3", BOOLEAN, COMPARABLE);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isSleeping3, mapHolder, state).asReturnedBoolean(isSleeping3, NodeException::new));
	}

	@Test @DisplayName("new MapHolder().get10() == WAITING with Comparable")
	void mapHolderGet10Comparable() throws Exception {
		StorageReference mapHolder = addConstructorCallTransaction(key, eoa, _10_000_000, BigInteger.ONE, jar(), ConstructorSignatures.of(MAP_HOLDER));
		StorageValue state = runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), MethodSignatures.ofNonVoid(MAP_HOLDER, "get10", STATE), mapHolder);
		var isWaiting3 = MethodSignatures.ofNonVoid(MAP_HOLDER, "isWaiting3", BOOLEAN, COMPARABLE);
		assertTrue(runInstanceNonVoidMethodCallTransaction(eoa, _10_000_000, jar(), isWaiting3, mapHolder, state).asReturnedBoolean(isWaiting3, NodeException::new));
	}
}