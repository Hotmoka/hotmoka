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
import static io.hotmoka.node.StorageTypes.STORAGE_MAP;
import static io.hotmoka.node.StorageTypes.STORAGE_MAP_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A test for the storage map Takamaka class.
 */
class StorageMap extends HotmokaTest {
	private static final ConstructorSignature STORAGE_TREE_MAP_INIT = ConstructorSignatures.of(StorageTypes.classNamed("io.takamaka.code.util.StorageTreeMap"));
	private static final NonVoidMethodSignature MK_EMPTY_EXPORTED_STORAGE_MAP = MethodSignatures.ofNonVoid(StorageTypes.classNamed("io.hotmoka.examples.storagemap.ExportedStorageMapMaker"), "mkEmptyExportedStorageMap", STORAGE_MAP);
	private static final NonVoidMethodSignature STORAGE_MAP_ISEMPTY = MethodSignatures.ofNonVoid(STORAGE_MAP_VIEW, "isEmpty", BOOLEAN);
	private static final NonVoidMethodSignature STORAGE_MAP_MIN = MethodSignatures.ofNonVoid(STORAGE_MAP_VIEW, "min", StorageTypes.OBJECT);
	private static final NonVoidMethodSignature STORAGE_MAP_SIZE = MethodSignatures.ofNonVoid(STORAGE_MAP_VIEW, "size", INT);
	private static final NonVoidMethodSignature STORAGE_MAP_GET = MethodSignatures.ofNonVoid(STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT);
	private static final VoidMethodSignature STORAGE_MAP_PUT = MethodSignatures.ofVoid(STORAGE_MAP, "put", StorageTypes.OBJECT, StorageTypes.OBJECT);
	private static final VoidMethodSignature STORAGE_MAP_REMOVE = MethodSignatures.ofVoid(STORAGE_MAP, "remove", StorageTypes.OBJECT);
	private static final StorageValue ONE = StorageValues.bigIntegerOf(1);
	private static final StorageValue TWO = StorageValues.bigIntegerOf(2);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference account0;

	/**
	 * The classpath of the classes being tested.
	 */
	private TransactionReference classpath;

	/**
	 * The private key of {@link #account0}.
	 */
	private PrivateKey key;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("storagemap.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_10_000_000);
		classpath = jar();
		account0 = account(0);
		key = privateKey(0);
	}

	@Test @DisplayName("new StorageTreeMap()")
	void constructionSucceeds() throws Exception {
		addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
	}

	@Test @DisplayName("new StorageTreeMap().size() == 0")
	void sizeIsInitially0() throws Exception {
		StorageReference map = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
		int size = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_SIZE, map).asReturnedInt(STORAGE_MAP_SIZE, UnexpectedValueException::new);
		assertEquals(0, size);
	}

	@Test @DisplayName("new StorageTreeMap().isEmpty() == true")
	void mapIsInitiallyEmpty() throws Exception {
		StorageReference map = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
		boolean isEmpty = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_ISEMPTY, map).asReturnedBoolean(STORAGE_MAP_ISEMPTY, UnexpectedValueException::new);
		assertTrue(isEmpty);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k,v) then get(k) yields v")
	void putThenGet() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
		StorageReference eoa = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		addInstanceVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa, ONE);
		BigInteger get = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_GET, map, eoa).asReturnedBigInteger(STORAGE_MAP_GET, UnexpectedValueException::new);

		assertEquals(BigInteger.ONE, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k1,v) then get(k2) yields null")
	void putThenGetWithOtherKey() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);
		KeyPair keys1 = signature().getKeyPair();
		String publicKey1 = Base64.toBase64String(signature().encodingOf(keys1.getPublic()));
		StorageReference eoa1 = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey1));
		KeyPair keys2 = signature().getKeyPair();
		String publicKey2 = Base64.toBase64String(signature().encodingOf(keys2.getPublic()));
		StorageReference eoa2 = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey2));
		addInstanceVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa1, ONE);
		StorageValue get = runInstanceNonVoidMethodCallTransaction
			(account0, _500_000, classpath, MethodSignatures.ofNonVoid(STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT), map, eoa2);

		assertEquals(StorageValues.NULL, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k1,v) then get(k2, _default) yields default")
	void putThenGetWithOtherKeyAndDefaultValue() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);
		KeyPair keys1 = signature().getKeyPair();
		String publicKey1 = Base64.toBase64String(signature().encodingOf(keys1.getPublic()));
		StorageReference eoa1 = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey1));
		KeyPair keys2 = signature().getKeyPair();
		String publicKey2 = Base64.toBase64String(signature().encodingOf(keys2.getPublic()));
		StorageReference eoa2 = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey2));
		addInstanceVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa1, ONE);
		StorageValue get = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, MethodSignatures.ofNonVoid(STORAGE_MAP_VIEW, "getOrDefault", StorageTypes.OBJECT, StorageTypes.OBJECT, StorageTypes.OBJECT), map, eoa2, TWO);

		assertEquals(TWO, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys then size is 10")
	void put100RandomThenSize() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);

		var accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
			accounts[i] = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		}

		var random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceVoidMethodCallTransaction
				(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, accounts[i], StorageValues.bigIntegerOf(random.nextLong()));

		int size = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_SIZE, map).asReturnedInt(STORAGE_MAP_SIZE, UnexpectedValueException::new);

		assertEquals(10, size);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 times the same key then size is 1")
	void put100TimesSameKeyThenSize() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
		StorageReference eoa = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));

		var random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceVoidMethodCallTransaction
				(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa, StorageValues.bigIntegerOf(random.nextLong()));

		int size = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_SIZE, map).asReturnedInt(STORAGE_MAP_SIZE, UnexpectedValueException::new);

		assertEquals(1, size);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 times equal string keys then size is 1")
	void put100TimesEqualStringThenSize() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);

		var random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceVoidMethodCallTransaction
				(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, StorageValues.stringOf("hello"), StorageValues.bigIntegerOf(random.nextLong()));

		int size = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_SIZE, map).asReturnedInt(STORAGE_MAP_SIZE, UnexpectedValueException::new);

		assertEquals(1, size);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 random BigInteger keys then min key is correct")
	void min() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);

		var random = new Random();
		BigInteger min = null;
		for (int i = 0; i < 10; i++) {
			BigInteger bi = BigInteger.valueOf(random.nextLong()); 
			if (min == null || bi.compareTo(min) < 0)
				min = bi;

			addInstanceVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, StorageValues.bigIntegerOf(bi), StorageValues.stringOf("hello"));
		}

		BigInteger result = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_MIN, map).asReturnedBigInteger(STORAGE_MAP_MIN, UnexpectedValueException::new);

		assertEquals(min, result);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys then remove the last then size is 9")
	void put100RandomThenRemoveLastThenSize() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);

		var accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
			accounts[i] = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		}

		var random = new Random();
		int i = 0;
		do {
			addInstanceVoidMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, accounts[i], StorageValues.bigIntegerOf(random.nextLong()));
		}
		while (++i < 10);

		addInstanceVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_REMOVE, map, accounts[random.nextInt(10)]);

		int size = runInstanceNonVoidMethodCallTransaction(account0, _500_000, classpath, STORAGE_MAP_SIZE, map).asReturnedInt(STORAGE_MAP_SIZE, UnexpectedValueException::new);

		assertEquals(9, size);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys and checks contains after each put")
	void put100RandomEachTimeCheckContains() throws Exception {
		var map = addStaticNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP)
				.asReturnedReference(MK_EMPTY_EXPORTED_STORAGE_MAP, UnexpectedValueException::new);

		var accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
			accounts[i] = addConstructorCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		}

		var random = new Random();
		var containsKey = MethodSignatures.ofNonVoid(STORAGE_MAP_VIEW, "containsKey", BOOLEAN, StorageTypes.OBJECT);
		for (int i = 0; i < 10; i++) {
			addInstanceVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, accounts[i], StorageValues.bigIntegerOf(random.nextLong()));
			assertTrue(addInstanceNonVoidMethodCallTransaction(key, account0, _500_000, BigInteger.ONE, classpath, containsKey, map, accounts[i])
				.asReturnedBoolean(containsKey, UnexpectedValueException::new));
		}
	}
}