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

import static io.hotmoka.node.StorageTypes.INT;
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
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A test for the simple storage class.
 */
class Storage extends HotmokaTest {
	private static final ClassType SIMPLE_STORAGE = StorageTypes.classNamed("io.hotmoka.examples.storage.SimpleStorage", IllegalArgumentException::new);
	private static final VoidMethodSignature SET = MethodSignatures.ofVoid(SIMPLE_STORAGE, "set", INT);
	private static final NonVoidMethodSignature GET = MethodSignatures.ofNonVoid(SIMPLE_STORAGE, "get", INT);
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_STORAGE = ConstructorSignatures.of("io.hotmoka.examples.storage.SimpleStorage");
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

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
		setJar("storage.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(ALL_FUNDS);
		eoa = account(0);
		key = privateKey(0);
	}

	@Test @DisplayName("new SimpleStorage().get() is an int")
	void neverInitializedStorageYieldsInt() throws Exception {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		StorageValue value = runInstanceNonVoidMethodCallTransaction(eoa, _50_000, jar(), GET, storage);
		assertTrue(value instanceof IntValue);
	}

	@Test @DisplayName("new SimpleStorage().get() == 0")
	void neverInitializedStorageYields0() throws Exception {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		var value = runInstanceNonVoidMethodCallTransaction(eoa, _50_000, jar(), GET, storage).asReturnedInt(GET, NodeException::new);
		assertEquals(0, value);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then get() == 13")
	void set13ThenGet13() throws Exception {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		addInstanceVoidMethodCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), SET, storage, StorageValues.intOf(13));
		var value = runInstanceNonVoidMethodCallTransaction(eoa, _50_000, jar(), GET, storage).asReturnedInt(GET, NodeException::new);
		assertEquals(13, value);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then set(17) then get() == 17")
	void set13set17ThenGet17() throws Exception {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		addInstanceVoidMethodCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), SET, storage, StorageValues.intOf(13));
		addInstanceVoidMethodCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), SET, storage, StorageValues.intOf(17));
		var value = runInstanceNonVoidMethodCallTransaction(eoa, _50_000, jar(), GET, storage).asReturnedInt(GET, NodeException::new);
		assertEquals(17, value);
	}
}