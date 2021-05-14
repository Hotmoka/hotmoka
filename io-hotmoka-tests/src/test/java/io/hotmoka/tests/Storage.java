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

import static io.hotmoka.beans.types.BasicTypes.INT;
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
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;

/**
 * A test for the simple storage class.
 */
class Storage extends TakamakaTest {
	private static final ClassType SIMPLE_STORAGE = new ClassType("io.hotmoka.examples.storage.SimpleStorage");
	private static final VoidMethodSignature SET = new VoidMethodSignature(SIMPLE_STORAGE, "set", INT);
	private static final NonVoidMethodSignature GET = new NonVoidMethodSignature(SIMPLE_STORAGE, "get", INT);
	private static final ConstructorSignature CONSTRUCTOR_SIMPLE_STORAGE = new ConstructorSignature("io.hotmoka.examples.storage.SimpleStorage");
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
	void neverInitializedStorageYieldsInt() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		StorageValue value = runInstanceMethodCallTransaction(eoa, _50_000, jar(), GET, storage);
		assertTrue(value instanceof IntValue);
	}

	@Test @DisplayName("new SimpleStorage().get() == 0")
	void neverInitializedStorageYields0() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		IntValue value = (IntValue) runInstanceMethodCallTransaction(eoa, _50_000, jar(), GET, storage);
		assertEquals(value.value, 0);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then get() == 13")
	void set13ThenGet13() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		addInstanceMethodCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), SET, storage, new IntValue(13));
		IntValue value = (IntValue) runInstanceMethodCallTransaction(eoa, _50_000, jar(), GET, storage);
		assertEquals(value.value, 13);
	}

	@Test @DisplayName("new SimpleStorage().set(13) then set(17) then get() == 17")
	void set13set17ThenGet17() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference storage = addConstructorCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), CONSTRUCTOR_SIMPLE_STORAGE);
		addInstanceMethodCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), SET, storage, new IntValue(13));
		addInstanceMethodCallTransaction(key, eoa, _50_000, BigInteger.ONE, jar(), SET, storage, new IntValue(17));
		IntValue value = (IntValue) runInstanceMethodCallTransaction(eoa, _50_000, jar(), GET, storage);
		assertEquals(value.value, 17);
	}
}