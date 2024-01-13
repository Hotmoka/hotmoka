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

import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.node.DeserializationError;

/**
 * A test for the creation of classes with the same name but from different jars.
 */
class ClassSwap extends HotmokaTest {
	private static final ConstructorSignature CONSTRUCTOR_C = new ConstructorSignature("C");
	private static final MethodSignature GET = new NonVoidMethodSignature("C", "get", StorageTypes.INT);

	/**
	 * The only account of the blockchain.
	 */
	private StorageReference account;

	/**
	 * The private key of {@linkplain #account}.
	 */
	private PrivateKey key;

	/**
	 * The classpath for the class C whose method get() yields 13.
	 */
	private TransactionReference classpathC13;

	/**
	 * The classpath for the class C whose method get() yields 17.
	 */
	private TransactionReference classpathC17;

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000);
		account = account(0);
		key = privateKey(0);

		classpathC13 = addJarStoreTransaction
			(key, account, _500_000, BigInteger.ONE, takamakaCode(), Files.readAllBytes(Paths.get("jars/c13.jar")), takamakaCode());

		classpathC17 = addJarStoreTransaction
			(key, account, _500_000, BigInteger.ONE, takamakaCode(), Files.readAllBytes(Paths.get("jars/c17.jar")), takamakaCode());
	}

	@Test @DisplayName("c13 new/get works in its classpath")
	void testC13() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c13 = addConstructorCallTransaction(key, account, _50_000, BigInteger.ONE, classpathC13, CONSTRUCTOR_C);
		IntValue get = (IntValue) addInstanceMethodCallTransaction(key, account, _50_000, BigInteger.ONE, classpathC13, GET, c13);

		assertSame(13, get.getValue());
	}

	@Test @DisplayName("c17 new/get works in its classpath")
	void testC17() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c17 = addConstructorCallTransaction(key, account, _50_000, BigInteger.ONE, classpathC17, CONSTRUCTOR_C);
		IntValue get = (IntValue) addInstanceMethodCallTransaction(key, account, _50_000, BigInteger.ONE, classpathC17, GET, c17);

		assertSame(17, get.getValue());
	}

	@Test @DisplayName("c13 new/get fails if classpath changed")
	void testC13SwapC17() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference c13 = addConstructorCallTransaction(key, account, _50_000, BigInteger.ONE, classpathC13, CONSTRUCTOR_C);

		// the following call should fail since c13 was created from another jar
		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			addInstanceMethodCallTransaction(key, account, _50_000, BigInteger.ONE, classpathC17, GET, c13)
		);
	}
}