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

import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Base64;

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
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;

/**
 * A test for a class that uses lambda expressions referring to entries.
 */
class Lambdas extends TakamakaTest {
	private static final ClassType LAMBDAS = new ClassType("io.hotmoka.examples.lambdas.Lambdas");
	private static final ConstructorSignature CONSTRUCTOR_LAMBDAS = new ConstructorSignature("io.hotmoka.examples.lambdas.Lambdas", ClassType.BIG_INTEGER, ClassType.STRING);

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
		addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
	}

	@Test @DisplayName("new Lambdas().invest(10)")
	void createLambdasThenInvest10() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, panarea(1), jar(), new VoidMethodSignature(LAMBDAS, "invest", ClassType.BIG_INTEGER), lambdas, new BigIntegerValue(BigInteger.ONE));
	}

	@Test @DisplayName("new Lambdas().testLambdaWithThis()")
	void testLambdaWithThis() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, panarea(1), jar(), new VoidMethodSignature(LAMBDAS, "testLambdaWithThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThis()")
	void testLambdaWithoutThis() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testLambdaWithoutThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThisGetStatic()")
	void testLambdaWithoutThisGetStatic() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testLambdaWithoutThisGetStatic"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntry()")
	void testMethodReferenceToEntry() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), new NonVoidMethodSignature(LAMBDAS, "testMethodReferenceToEntry", INT), lambdas);

		assertEquals(11, result.value);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntryOfOtherClass()")
	void testMethodReferenceToEntryOfOtherClass() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testMethodReferenceToEntryOfOtherClass"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntrySameContract()")
	void testMethodReferenceToEntrySameContract() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), new NonVoidMethodSignature(LAMBDAS, "testMethodReferenceToEntrySameContract", INT), lambdas);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntry()")
	void testConstructorReferenceToEntry() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), new NonVoidMethodSignature(LAMBDAS, "testConstructorReferenceToEntry", INT), lambdas);

		assertEquals(11, result.value);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntryPopResult()")
	void testConstructorReferenceToEntryPopResult() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), new VoidMethodSignature(LAMBDAS, "testConstructorReferenceToEntryPopResult"), lambdas);
	}

	@Test @DisplayName("new Lambdas().whiteListChecks(13,1,1973)==7")
	void testWhiteListChecks() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		IntValue result = (IntValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(LAMBDAS, "whiteListChecks", INT, ClassType.OBJECT, ClassType.OBJECT, ClassType.OBJECT),
			lambdas, new BigIntegerValue(BigInteger.valueOf(13L)), new BigIntegerValue(BigInteger.valueOf(1L)), new BigIntegerValue(BigInteger.valueOf(1973L)));

		assertEquals(7, result.value);
	}

	@Test @DisplayName("new Lambdas().concatenation(\"hello\", \"hi\", self, 1973L, 13)==\"\"")
	void testConcatenation() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000), new StringValue(publicKey));
		StringValue result = (StringValue) addInstanceMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(),
			new NonVoidMethodSignature(LAMBDAS, "concatenation", ClassType.STRING, ClassType.STRING, ClassType.OBJECT, LAMBDAS, BasicTypes.LONG, INT),
			lambdas,
			new StringValue("hello"), new StringValue("hi"), lambdas, new LongValue(1973L), new IntValue(13));

		assertEquals("hellohian externally owned account197313", result.value);
	}
}