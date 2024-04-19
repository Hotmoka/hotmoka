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

import static io.hotmoka.beans.StorageTypes.BOOLEAN;
import static io.hotmoka.beans.StorageTypes.INT;
import static io.hotmoka.beans.StorageTypes.STORAGE_MAP;
import static io.hotmoka.beans.StorageTypes.STORAGE_MAP_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.signatures.MethodSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.BooleanValue;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.crypto.Base64;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test for the storage map Takamaka class.
 */
class StorageMap extends HotmokaTest {
	private static final BigInteger _50_000 = BigInteger.valueOf(50_000);
	private static final ConstructorSignature STORAGE_TREE_MAP_INIT = ConstructorSignatures.of("io.takamaka.code.util.StorageTreeMap");
	private static final MethodSignature MK_EMPTY_EXPORTED_STORAGE_MAP = MethodSignatures.of("io.hotmoka.examples.storagemap.ExportedStorageMapMaker", "mkEmptyExportedStorageMap", STORAGE_MAP);
	private static final MethodSignature STORAGE_MAP_ISEMPTY = MethodSignatures.of(STORAGE_MAP_VIEW, "isEmpty", BOOLEAN);
	private static final MethodSignature STORAGE_MAP_MIN = MethodSignatures.of(STORAGE_MAP_VIEW, "min", StorageTypes.OBJECT);
	private static final MethodSignature STORAGE_MAP_SIZE = MethodSignatures.of(STORAGE_MAP_VIEW, "size", INT);
	private static final MethodSignature STORAGE_MAP_GET = MethodSignatures.of(STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT);
	private static final MethodSignature STORAGE_MAP_PUT = MethodSignatures.ofVoid(STORAGE_MAP, "put", StorageTypes.OBJECT, StorageTypes.OBJECT);
	private static final MethodSignature STORAGE_MAP_REMOVE = MethodSignatures.ofVoid(STORAGE_MAP, "remove", StorageTypes.OBJECT);
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
		setAccounts(_1_000_000);
		classpath = jar();
		account0 = account(0);
		key = privateKey(0);
	}

	@Test @DisplayName("new StorageTreeMap()")
	void constructionSucceeds() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
	}

	@Test @DisplayName("new StorageTreeMap().size() == 0")
	void sizeIsInitially0() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(StorageValues.intOf(0), size);
	}

	@Test @DisplayName("new StorageTreeMap().isEmpty() == true")
	void mapIsInitiallyEmpty() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_TREE_MAP_INIT);
		BooleanValue size = (BooleanValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_ISEMPTY, map);

		assertEquals(StorageValues.TRUE, size);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k,v) then get(k) yields v")
	void putThenGet() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
		StorageReference eoa = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa, ONE);
		BigIntegerValue get = (BigIntegerValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_GET, map, eoa);

		assertEquals(ONE, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k1,v) then get(k2) yields null")
	void putThenGetWithOtherKey() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys1 = signature().getKeyPair();
		String publicKey1 = Base64.toBase64String(signature().encodingOf(keys1.getPublic()));
		StorageReference eoa1 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey1));
		KeyPair keys2 = signature().getKeyPair();
		String publicKey2 = Base64.toBase64String(signature().encodingOf(keys2.getPublic()));
		StorageReference eoa2 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey2));
		addInstanceMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa1, ONE);
		StorageValue get = runInstanceMethodCallTransaction
			(account0, _50_000, classpath, MethodSignatures.of(STORAGE_MAP_VIEW, "get", StorageTypes.OBJECT, StorageTypes.OBJECT), map, eoa2);

		assertEquals(StorageValues.NULL, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap().put(k1,v) then get(k2, _default) yields default")
	void putThenGetWithOtherKeyAndDefaultValue() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys1 = signature().getKeyPair();
		String publicKey1 = Base64.toBase64String(signature().encodingOf(keys1.getPublic()));
		StorageReference eoa1 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey1));
		KeyPair keys2 = signature().getKeyPair();
		String publicKey2 = Base64.toBase64String(signature().encodingOf(keys2.getPublic()));
		StorageReference eoa2 = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey2));
		addInstanceMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa1, ONE);
		StorageValue get = runInstanceMethodCallTransaction(account0, _50_000, classpath, MethodSignatures.of(STORAGE_MAP_VIEW, "getOrDefault", StorageTypes.OBJECT, StorageTypes.OBJECT, StorageTypes.OBJECT), map, eoa2, TWO);

		assertEquals(TWO, get);
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys then size is 10")
	void put100RandomThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		StorageReference[] accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
			accounts[i] = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		}

		var random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, accounts[i], StorageValues.bigIntegerOf(random.nextLong()));

		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(10, size.getValue());
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 times the same key then size is 1")
	void put100TimesSameKeyThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
		StorageReference eoa = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));

		var random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceMethodCallTransaction
				(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, eoa, StorageValues.bigIntegerOf(random.nextLong()));

		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(1, size.getValue());
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 times equal string keys then size is 1")
	void put100TimesEqualStringThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		var random = new Random();
		for (int i = 0; i < 10; i++)
			addInstanceMethodCallTransaction
				(key, account0, _50_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, StorageValues.stringOf("hello"), StorageValues.bigIntegerOf(random.nextLong()));

		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(1, size.getValue());
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 random BigInteger keys then min key is correct")
	void min() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		var random = new Random();
		BigInteger min = null;
		for (int i = 0; i < 10; i++) {
			BigInteger bi = BigInteger.valueOf(random.nextLong()); 
			if (min == null || bi.compareTo(min) < 0)
				min = bi;

			addInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, StorageValues.bigIntegerOf(bi), StorageValues.stringOf("hello"));
		}

		BigIntegerValue result = (BigIntegerValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_MIN, map);

		assertEquals(min, result.getValue());
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys then remove the last then size is 9")
	void put100RandomThenRemoveLastThenSize() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		var map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		var accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
			accounts[i] = addConstructorCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		}

		var random = new Random();
		int i = 0;
		do {
			addInstanceMethodCallTransaction
				(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, accounts[i], StorageValues.bigIntegerOf(random.nextLong()));
		}
		while (++i < 10);

		addInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_REMOVE, map, accounts[random.nextInt(10)]);
		IntValue size = (IntValue) runInstanceMethodCallTransaction(account0, _50_000, classpath, STORAGE_MAP_SIZE, map);

		assertEquals(9, size.getValue());
	}

	@Test @DisplayName("mkEmptyExportedStorageMap() put 10 storage keys and checks contains after each put")
	void put100RandomEachTimeCheckContains() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		var map = (StorageReference) addStaticMethodCallTransaction(key, account0, _50_000, BigInteger.ONE, classpath, MK_EMPTY_EXPORTED_STORAGE_MAP);

		var accounts = new StorageReference[10];
		for (int i = 0; i < 10; i++) {
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
			accounts[i] = addConstructorCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));
		}

		var results = new BooleanValue[10];

		var random = new Random();
		for (int i = 0; i < 10; i++) {
			addInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, STORAGE_MAP_PUT, map, accounts[i], StorageValues.bigIntegerOf(random.nextLong()));
			results[i] = (BooleanValue) addInstanceMethodCallTransaction(key, account0, _100_000, BigInteger.ONE, classpath, MethodSignatures.of(STORAGE_MAP_VIEW, "containsKey", BOOLEAN, StorageTypes.OBJECT), map, accounts[i]);
		}

		for (BooleanValue result: results)
			assertTrue(result.getValue());
	}
}