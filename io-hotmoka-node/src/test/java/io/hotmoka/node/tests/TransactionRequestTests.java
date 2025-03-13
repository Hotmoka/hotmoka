/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.node.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.Signer;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.testing.AbstractLoggedTests;

public class TransactionRequestTests extends AbstractLoggedTests {
	private final static TransactionReference classpath = TransactionReferences.of("cafebabe01234567cafebabe01234567cafebabe01234567cafebabe01234567", IllegalArgumentException::new);
	private final static TransactionReference reference = TransactionReferences.of("01234567cafebabe01234567cafebabe01234567cafebabe01234567cafebabe", IllegalArgumentException::new);
	private final static TransactionReference reference2 = TransactionReferences.of("a1234567cafebabe01234567cafebabe01234567cafebabe01234567cafebabe", IllegalArgumentException::new);
	private final static TransactionReference reference3 = TransactionReferences.of("b1234567cafebabe01234567cafebabe01234567cafebabe01234567cafebabe", IllegalArgumentException::new);
	private final static StorageReference caller = StorageValues.reference(reference, BigInteger.TWO, IllegalArgumentException::new);
	private final static byte[] jar = "Imagine this to be a very beautiful jar file".getBytes();
	private final static Signer<SignedTransactionRequest<?>> signer;
	private final static BigInteger nonce = BigInteger.valueOf(1317);
	private final static BigInteger gasLimit = BigInteger.valueOf(1987);
	private final static BigInteger gasPrice = BigInteger.valueOf(200);
	private final static String chainId = "marabunta";
	private final static ClassType MY_CLASS = StorageTypes.classNamed("io.hotmoka.MyClass", IllegalArgumentException::new);
	private final static NonVoidMethodSignature MOO = MethodSignatures.ofNonVoid(MY_CLASS, "moo", StorageTypes.BOOLEAN, StorageTypes.INT, StorageTypes.classNamed("io.hotmoka.OtherClass", IllegalArgumentException::new));

	static {
		try {
			signer = SignatureAlgorithms.ed25519().getSigner(SignatureAlgorithms.ed25519().getKeyPair().getPrivate(), SignedTransactionRequest<?>::toByteArrayWithoutSignature);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@DisplayName("gamete creation transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGameteCreationTransactionRequest() throws Exception {
		String publicKey = Base64.toBase64String(SignatureAlgorithms.ed25519().getKeyPair().getPublic().getEncoded());
		var request1 = TransactionRequests.gameteCreation(classpath, BigInteger.TWO, publicKey, IllegalArgumentException::new);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}

	@Test
	@DisplayName("initialization transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForInitializationTransactionRequest() throws Exception {
		var manifest = StorageValues.reference(reference, BigInteger.ONE, IllegalArgumentException::new);
		var request1 = TransactionRequests.initialization(classpath, manifest, IllegalArgumentException::new);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}

	@Test
	@DisplayName("jar store initial transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForJarStoreInitialTransactionRequest() throws Exception {
		var request1 = TransactionRequests.jarStoreInitial(jar, new TransactionReference[] { reference, reference2, reference3 }, IllegalArgumentException::new);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}

	@Test
	@DisplayName("jar store transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForJarStoreTransactionRequest() throws Exception {
		var request1 = TransactionRequests.jarStore(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, jar, reference, reference2, reference3);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}

	@Test
	@DisplayName("constructor call transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForConstructorCallTransactionRequest() throws Exception {
		var constructor = ConstructorSignatures.of(MY_CLASS, StorageTypes.INT, StorageTypes.classNamed("io.hotmoka.OtherClass", IllegalArgumentException::new));
		var value1 = StorageValues.intOf(13);
		var value2 = StorageValues.reference(reference2, BigInteger.ZERO, RuntimeException::new);
		var request1 = TransactionRequests.constructorCall(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, constructor, value1, value2);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}

	@Test
	@DisplayName("static method call transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForStaticMethodCallTransactionRequest() throws Exception {
		var value1 = StorageValues.intOf(13);
		var value2 = StorageValues.reference(reference2, BigInteger.ZERO, RuntimeException::new);
		var request1 = TransactionRequests.staticMethodCall(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, MOO, value1, value2);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}

	@Test
	@DisplayName("instance method call transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForInstanceMethodCallTransactionRequest() throws Exception {
		var value1 = StorageValues.intOf(13);
		var value2 = StorageValues.reference(reference2, BigInteger.ZERO, IllegalArgumentException::new);
		var receiver = StorageValues.reference(reference2, BigInteger.valueOf(17), IllegalArgumentException::new);
		var request1 = TransactionRequests.instanceMethodCall(signer, caller, nonce, chainId, gasLimit, gasPrice, classpath, MOO, receiver, value1, value2);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}

	@Test
	@DisplayName("instance system method call transaction requests are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForInstanceSystemMethodCallTransactionRequest() throws Exception {
		var value1 = StorageValues.intOf(13);
		var value2 = StorageValues.reference(reference2, BigInteger.ZERO, IllegalArgumentException::new);
		var receiver = StorageValues.reference(reference2, BigInteger.valueOf(17), IllegalArgumentException::new);
		var request1 = TransactionRequests.instanceSystemMethodCall(caller, nonce, gasLimit, classpath, MOO, receiver, new StorageValue[] { value1, value2 }, IllegalArgumentException::new);
		String encoded = new TransactionRequests.Encoder().encode(request1);
		var request2 = new TransactionRequests.Decoder().decode(encoded);
		assertEquals(request1, request2);
	}
}