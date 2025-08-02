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

import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.node.StorageTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for a class that uses lambda expressions referring to entries.
 */
class Lambdas extends HotmokaTest {
	private static final ClassType LAMBDAS = StorageTypes.classNamed("io.hotmoka.examples.lambdas.Lambdas");
	private static final ConstructorSignature CONSTRUCTOR_LAMBDAS = ConstructorSignatures.of(LAMBDAS, StorageTypes.BIG_INTEGER, StorageTypes.STRING);

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference eoa;

	/**
	 * The private key of {@linkplain #eoa}.
	 */
	private PrivateKey key;

	/**
	 * The base64 encoded public key of the Lambdas being created.
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
	void createLambdas() throws Exception {
		addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
	}

	@Test @DisplayName("new Lambdas().invest(10)")
	void createLambdasThenInvest10() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceVoidMethodCallTransaction(key, eoa, _500_000, panarea(1), jar(), MethodSignatures.ofVoid(LAMBDAS, "invest", StorageTypes.BIG_INTEGER), lambdas, StorageValues.bigIntegerOf(1));
	}

	@Test @DisplayName("new Lambdas().testLambdaWithThis()")
	void testLambdaWithThis() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceVoidMethodCallTransaction(key, eoa, _500_000, panarea(1), jar(), MethodSignatures.ofVoid(LAMBDAS, "testLambdaWithThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThis()")
	void testLambdaWithoutThis() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testLambdaWithoutThis"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThisGetStatic()")
	void testLambdaWithoutThisGetStatic() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testLambdaWithoutThisGetStatic"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntry()")
	void testMethodReferenceToEntry() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		var callee = MethodSignatures.ofNonVoid(LAMBDAS, "testMethodReferenceToEntry", INT);
		var result = addInstanceNonVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), callee, lambdas).asReturnedInt(callee, UnexpectedValueException::new);

		assertEquals(11, result);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntryOfOtherClass()")
	void testMethodReferenceToEntryOfOtherClass() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testMethodReferenceToEntryOfOtherClass"), lambdas);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntrySameContract()")
	void testMethodReferenceToEntrySameContract() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceNonVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofNonVoid(LAMBDAS, "testMethodReferenceToEntrySameContract", INT), lambdas);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntry()")
	void testConstructorReferenceToEntry() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		var callee = MethodSignatures.ofNonVoid(LAMBDAS, "testConstructorReferenceToEntry", INT);
		var result = addInstanceNonVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), callee, lambdas).asReturnedInt(callee, UnexpectedValueException::new);

		assertEquals(11, result);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntryPopResult()")
	void testConstructorReferenceToEntryPopResult() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, panarea(1), jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		addInstanceVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), MethodSignatures.ofVoid(LAMBDAS, "testConstructorReferenceToEntryPopResult"), lambdas);
	}

	@Test @DisplayName("new Lambdas().whiteListChecks(13,1,1973)==7")
	void testWhiteListChecks() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		var callee = MethodSignatures.ofNonVoid(LAMBDAS, "whiteListChecks", INT, StorageTypes.OBJECT, StorageTypes.OBJECT, StorageTypes.OBJECT);
		var result = addInstanceNonVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(),
			callee, lambdas, StorageValues.bigIntegerOf(13L), StorageValues.bigIntegerOf(1L), StorageValues.bigIntegerOf(1973L))
				.asReturnedInt(callee, UnexpectedValueException::new);

		assertEquals(7, result);
	}

	@Test @DisplayName("new Lambdas().concatenation(\"hello\", \"hi\", self, 1973L, 13)==\"\"")
	void testConcatenation() throws Exception {
		StorageReference lambdas = addConstructorCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_LAMBDAS, StorageValues.bigIntegerOf(_100_000), StorageValues.stringOf(publicKey));
		var callee = MethodSignatures.ofNonVoid(LAMBDAS, "concatenation", StorageTypes.STRING, StorageTypes.STRING, StorageTypes.OBJECT, LAMBDAS, StorageTypes.LONG, INT);
		var result = addInstanceNonVoidMethodCallTransaction(key, eoa, _500_000, BigInteger.ONE, jar(),
			callee, lambdas, StorageValues.stringOf("hello"), StorageValues.stringOf("hi"), lambdas, StorageValues.longOf(1973L), StorageValues.intOf(13))
				.asReturnedString(callee, UnexpectedValueException::new);

		assertEquals("hellohian externally owned account197313", result);
	}
}