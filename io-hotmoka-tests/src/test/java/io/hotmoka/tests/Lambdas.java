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

import static io.hotmoka.beans.StorageTypes.INT;
import static io.hotmoka.helpers.Coin.panarea;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.api.signatures.ConstructorSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.IntValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StringValue;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;

/**
 * A test for a class that uses lambda expressions referring to entries.
 */
class Lambdas extends HotmokaTest {
	private static final ClassType LAMBDAS = StorageTypes.classNamed("io.hotmoka.examples.lambdas.Lambdas");
	private static final ConstructorSignature CONSTRUCTOR_LAMBDAS = ConstructorSignatures.of("io.hotmoka.examples.lambdas.Lambdas", StorageTypes.BIG_INTEGER, StorageTypes.STRING);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference eoa;

	/**
	 * The private key of {@linkplain #eoa}.
	 */
	private PrivateKey key;

	/**
	 * The base64 encoded public ket of the Lambdas being created.
	 */
	private String publicKey;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("lambdas.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
		eoa = account(0);
		key = privateKey(0);
		KeyPair keys = signature().getKeyPair();
		publicKey = Base64.getEncoder().encodeToString(signature().encodingOf(keys.getPublic()));
	}

	@Test @DisplayName("new Lambdas()")
	void createLambdas() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
	}

	@Test @DisplayName("new Lambdas().invest(10)")
	void createLambdasThenInvest10() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, panarea(1), jar(), MethodSignatures.ofVoid(LAMBDAS, "invest", StorageTypes.BIG_INTEGER), lambdas, StorageValues.bigIntegerOf(1));
	}

	@Test @DisplayName("new Lambdas().testLambdaWithThis()")
	void testLambdaWithThis() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, panarea(1), jar(), MethodSignatures.ofVoid(LAMBDAS, "testLambdaWithThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThis()")
	void testLambdaWithoutThis() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testLambdaWithoutThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThisGetStatic()")
	void testLambdaWithoutThisGetStatic() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testLambdaWithoutThisGetStatic"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntry()")
	void testMethodReferenceToEntry() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.of(LAMBDAS, "testMethodReferenceToEntry", INT), lambdas);

		assertEquals(11, result.getValue());
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntryOfOtherClass()")
	void testMethodReferenceToEntryOfOtherClass() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testMethodReferenceToEntryOfOtherClass"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntrySameContract()")
	void testMethodReferenceToEntrySameContract() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.of(LAMBDAS, "testMethodReferenceToEntrySameContract", INT), lambdas);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntry()")
	void testConstructorReferenceToEntry() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.of(LAMBDAS, "testConstructorReferenceToEntry", INT), lambdas);

		assertEquals(11, result.getValue());
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntryPopResult()")
	void testConstructorReferenceToEntryPopResult() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testConstructorReferenceToEntryPopResult"), lambdas);
	}

	@Test @DisplayName("new Lambdas().whiteListChecks(13,1,1973)==7")
	void testWhiteListChecks() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(),
			MethodSignatures.of(LAMBDAS, "whiteListChecks", INT, StorageTypes.OBJECT, StorageTypes.OBJECT, StorageTypes.OBJECT),
			lambdas, StorageValues.bigIntegerOf(13L), StorageValues.bigIntegerOf(1L), StorageValues.bigIntegerOf(1973L));

		assertEquals(7, result.getValue());
	}

	@Test @DisplayName("new Lambdas().concatenation(\"hello\", \"hi\", self, 1973L, 13)==\"\"")
	void testConcatenation() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		StringValue result = (StringValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(),
			MethodSignatures.of(LAMBDAS, "concatenation", StorageTypes.STRING, StorageTypes.STRING, StorageTypes.OBJECT, LAMBDAS, StorageTypes.LONG, INT),
			lambdas,
			StorageValues.stringOf("hello"), StorageValues.stringOf("hi"), lambdas, StorageValues.longOf(1973L), StorageValues.intOf(13));

		assertEquals("hellohian externally owned account197313", result.getValue());
	}
}