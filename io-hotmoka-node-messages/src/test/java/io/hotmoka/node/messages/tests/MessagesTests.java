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

package io.hotmoka.node.messages.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.NodeInfos;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.TransactionResponses;
import io.hotmoka.beans.Updates;
import io.hotmoka.beans.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.api.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.messages.AddConstructorCallTransactionMessages;
import io.hotmoka.node.messages.AddConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.GetConsensusConfigMessages;
import io.hotmoka.node.messages.GetConsensusConfigResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
import io.hotmoka.node.messages.GetPolledResponseMessages;
import io.hotmoka.node.messages.GetPolledResponseResultMessages;
import io.hotmoka.node.messages.GetRequestMessages;
import io.hotmoka.node.messages.GetRequestResultMessages;
import io.hotmoka.node.messages.GetResponseMessages;
import io.hotmoka.node.messages.GetResponseResultMessages;
import io.hotmoka.node.messages.GetStateMessages;
import io.hotmoka.node.messages.GetStateResultMessages;
import io.hotmoka.node.messages.GetTakamakaCodeMessages;
import io.hotmoka.node.messages.GetTakamakaCodeResultMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionMessages;
import io.hotmoka.node.messages.PostConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.PostInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.PostInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.PostJarStoreTransactionMessages;
import io.hotmoka.node.messages.PostJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionResultMessages;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class MessagesTests extends AbstractLoggedTests {
	private final static TransactionReference TRANSACTION_REFERENCE = TransactionReferences.of("12345678901234567890abcdeabcdeff12345678901234567890abcdeabcdeff");
	private final static StorageReference OBJECT = StorageValues.reference(TRANSACTION_REFERENCE, BigInteger.ONE);

	@Test
	@DisplayName("getNodeInfo messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetNodeInfo() throws EncodeException, DecodeException {
		var expected = GetNodeInfoMessages.of("id");
		String encoded = new GetNodeInfoMessages.Encoder().encode(expected);
		var actual = new GetNodeInfoMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getConsensusConfig messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetConsensusConfig() throws EncodeException, DecodeException {
		var expected = GetConsensusConfigMessages.of("id");
		String encoded = new GetConsensusConfigMessages.Encoder().encode(expected);
		var actual = new GetConsensusConfigMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getConsensusConfigResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetConsensusConfigResult() throws EncodeException, DecodeException {
		var expected = GetConsensusConfigResultMessages.of("config", "id");
		String encoded = new GetConsensusConfigResultMessages.Encoder().encode(expected);
		var actual = new GetConsensusConfigResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getNodeInfoResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetNodeInfoResult() throws EncodeException, DecodeException {
		var expected = GetNodeInfoResultMessages.of(NodeInfos.of("special node", "1.2.3", "id314"), "id");
		String encoded = new GetNodeInfoResultMessages.Encoder().encode(expected);
		var actual = new GetNodeInfoResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getTakamakaCode messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetTakamakaCode() throws EncodeException, DecodeException {
		var expected = GetTakamakaCodeMessages.of("id");
		String encoded = new GetTakamakaCodeMessages.Encoder().encode(expected);
		var actual = new GetTakamakaCodeMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getTakamakaCodeResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetTakamakaCodeResult() throws EncodeException, DecodeException {
		var expected = GetTakamakaCodeResultMessages.of(TransactionReferences.of("12345678901234567890abcdeabcdeff12345678901234567890abcdeabcdeff"), "id");
		String encoded = new GetTakamakaCodeResultMessages.Encoder().encode(expected);
		var actual = new GetTakamakaCodeResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getManifest messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetManifest() throws EncodeException, DecodeException {
		var expected = GetManifestMessages.of("id");
		String encoded = new GetManifestMessages.Encoder().encode(expected);
		var actual = new GetManifestMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getManifestResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetManifestResult() throws EncodeException, DecodeException {
		var expected = GetManifestResultMessages.of(OBJECT, "id");
		String encoded = new GetManifestResultMessages.Encoder().encode(expected);
		var actual = new GetManifestResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getClassTag messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetClassTag() throws EncodeException, DecodeException {
		var expected = GetClassTagMessages.of(OBJECT, "id");
		String encoded = new GetClassTagMessages.Encoder().encode(expected);
		var actual = new GetClassTagMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getClassTagResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetClassTagResult() throws EncodeException, DecodeException {
		var expected = GetClassTagResultMessages.of(Updates.classTag(OBJECT, StorageTypes.classNamed("my.class"), OBJECT.getTransaction()), "id");
		String encoded = new GetClassTagResultMessages.Encoder().encode(expected);
		var actual = new GetClassTagResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getState messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetState() throws EncodeException, DecodeException {
		var expected = GetStateMessages.of(OBJECT, "id");
		String encoded = new GetStateMessages.Encoder().encode(expected);
		var actual = new GetStateMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getStateResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetStateResult() throws EncodeException, DecodeException {
		ClassType clazz = StorageTypes.classNamed("io.my.Class");
		Update classTag = Updates.classTag(OBJECT, StorageTypes.classNamed("my.class"), OBJECT.getTransaction());
		Update update1 = Updates.ofInt(OBJECT, FieldSignatures.of(clazz, "field1", StorageTypes.INT), 42);
		Update update2 = Updates.ofBigInteger(OBJECT, FieldSignatures.of(clazz, "field2", StorageTypes.BIG_INTEGER), BigInteger.valueOf(13L));
		Update update3 = Updates.ofString(OBJECT, FieldSignatures.of(clazz, "field3", StorageTypes.STRING), "hello");

		var expected = GetStateResultMessages.of(Stream.of(classTag, update1, update2, update3), "id");
		String encoded = new GetStateResultMessages.Encoder().encode(expected);
		var actual = new GetStateResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getRequest messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetRequest() throws EncodeException, DecodeException {
		var expected = GetRequestMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new GetRequestMessages.Encoder().encode(expected);
		var actual = new GetRequestMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getRequestResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetRequestResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var request = TransactionRequests.gameteCreation(TRANSACTION_REFERENCE, BigInteger.valueOf(10_000_000L), BigInteger.ZERO, Base64.toBase64String(SignatureAlgorithms.ed25519().getKeyPair().getPublic().getEncoded()));
		var expected = GetRequestResultMessages.of(request, "id");
		String encoded = new GetRequestResultMessages.Encoder().encode(expected);
		var actual = new GetRequestResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getResponse messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetResponse() throws EncodeException, DecodeException {
		var expected = GetResponseMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new GetResponseMessages.Encoder().encode(expected);
		var actual = new GetResponseMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getResponseResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetResponseResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		ClassType clazz = StorageTypes.classNamed("io.my.Class");
		Update update1 = Updates.ofInt(OBJECT, FieldSignatures.of(clazz, "field1", StorageTypes.INT), 42);
		Update update2 = Updates.ofBigInteger(OBJECT, FieldSignatures.of(clazz, "field2", StorageTypes.BIG_INTEGER), BigInteger.valueOf(13L));
		Update update3 = Updates.ofString(OBJECT, FieldSignatures.of(clazz, "field3", StorageTypes.STRING), "hello");
		var response = TransactionResponses.constructorCallException("io.my.Exception", "code exploded", null, Stream.of(update1, update2, update3), Stream.of(OBJECT),
			BigInteger.valueOf(42L), BigInteger.valueOf(13L), BigInteger.valueOf(17L));

		var expected = GetResponseResultMessages.of(response, "id");
		String encoded = new GetResponseResultMessages.Encoder().encode(expected);
		var actual = new GetResponseResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getPolledResponse messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetPolledResponse() throws EncodeException, DecodeException {
		var expected = GetPolledResponseMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new GetPolledResponseMessages.Encoder().encode(expected);
		var actual = new GetPolledResponseMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getPolledResponseResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetPolledResponseResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		ClassType clazz = StorageTypes.classNamed("io.my.Class");
		Update update1 = Updates.ofInt(OBJECT, FieldSignatures.of(clazz, "field1", StorageTypes.INT), 42);
		Update update2 = Updates.ofBigInteger(OBJECT, FieldSignatures.of(clazz, "field2", StorageTypes.BIG_INTEGER), BigInteger.valueOf(13L));
		Update update3 = Updates.ofString(OBJECT, FieldSignatures.of(clazz, "field3", StorageTypes.STRING), "hello");
		var response = TransactionResponses.constructorCallException("io.my.Exception", "code exploded", null, Stream.of(update1, update2, update3), Stream.of(OBJECT),
			BigInteger.valueOf(42L), BigInteger.valueOf(13L), BigInteger.valueOf(17L));

		var expected = GetPolledResponseResultMessages.of(response, "id");
		String encoded = new GetPolledResponseResultMessages.Encoder().encode(expected);
		var actual = new GetPolledResponseResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runInstanceMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunInstanceMethodCallTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var method = MethodSignatures.of("my.class", "target", StorageTypes.STRING, StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), InstanceMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.instanceMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, method, OBJECT, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = RunInstanceMethodCallTransactionMessages.of(request, "id");
		String encoded = new RunInstanceMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new RunInstanceMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runInstanceMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunInstanceMethodCallNonVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = RunInstanceMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new RunInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runInstanceMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunInstanceMethodCallVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = RunInstanceMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new RunInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runStaticMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunStaticMethodCallTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var method = MethodSignatures.of("my.class", "target", StorageTypes.STRING, StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), StaticMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.staticMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, method, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = RunStaticMethodCallTransactionMessages.of(request, "id");
		String encoded = new RunStaticMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new RunStaticMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runStaticMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunStaticMethodCallNonVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = RunStaticMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new RunStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runStaticMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunStaticMethodCallVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = RunStaticMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new RunStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInstanceMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInstanceMethodCallTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var method = MethodSignatures.of("my.class", "target", StorageTypes.STRING, StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), InstanceMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.instanceMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, method, OBJECT, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = AddInstanceMethodCallTransactionMessages.of(request, "id");
		String encoded = new AddInstanceMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new AddInstanceMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInstanceMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInstanceMethodCallNonVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = AddInstanceMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new AddInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInstanceMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInstanceMethodCallVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = AddInstanceMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new AddInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addStaticMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddStaticMethodCallTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var method = MethodSignatures.of("my.class", "target", StorageTypes.STRING, StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), StaticMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.staticMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, method, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = AddStaticMethodCallTransactionMessages.of(request, "id");
		String encoded = new AddStaticMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new AddStaticMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addStaticMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddStaticMethodCallNonVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = AddStaticMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new AddStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addStaticMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddStaticMethodCallVoidTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = AddStaticMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new AddStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addConstructorCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddConstructorCallTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var constructor = ConstructorSignatures.of("my.class", StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), ConstructorCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.constructorCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, constructor, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = AddConstructorCallTransactionMessages.of(request, "id");
		String encoded = new AddConstructorCallTransactionMessages.Encoder().encode(expected);
		var actual = new AddConstructorCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addConstructorCallTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddConstructorCallTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = AddConstructorCallTransactionResultMessages.of(OBJECT, "id");
		String encoded = new AddConstructorCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddConstructorCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addJarStoreTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddJarStoreTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), JarStoreTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.jarStore(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE,
			"These are the bytes of a very large jar that must be installed in the Hotmoka node".getBytes());
		var expected = AddJarStoreTransactionMessages.of(request, "id");
		String encoded = new AddJarStoreTransactionMessages.Encoder().encode(expected);
		var actual = new AddJarStoreTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addJarStoreTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddJarStoreTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = AddJarStoreTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new AddJarStoreTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddJarStoreTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postConstructorCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostConstructorCallTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var constructor = ConstructorSignatures.of("my.class", StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), ConstructorCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.constructorCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, constructor, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = PostConstructorCallTransactionMessages.of(request, "id");
		String encoded = new PostConstructorCallTransactionMessages.Encoder().encode(expected);
		var actual = new PostConstructorCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postConstructorCallTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostConstructorCallTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = PostConstructorCallTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new PostConstructorCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new PostConstructorCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postInstanceMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostInstanceMethodCallTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var method = MethodSignatures.of("my.class", "target", StorageTypes.STRING, StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), InstanceMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.instanceMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, method, OBJECT, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = PostInstanceMethodCallTransactionMessages.of(request, "id");
		String encoded = new PostInstanceMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new PostInstanceMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postInstanceMethodCallTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostInstanceMethodCallTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = PostInstanceMethodCallTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new PostInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new PostInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postJarStoreTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostJarStoreTransaction() throws EncodeException, DecodeException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), JarStoreTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.jarStore(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE,
			"These are the bytes of a very large jar that must be installed in the Hotmoka node".getBytes());
		var expected = PostJarStoreTransactionMessages.of(request, "id");
		String encoded = new PostJarStoreTransactionMessages.Encoder().encode(expected);
		var actual = new PostJarStoreTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postJarStoreTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostJarStoreTransactionResult() throws EncodeException, DecodeException, NoSuchAlgorithmException {
		var expected = PostJarStoreTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new PostJarStoreTransactionResultMessages.Encoder().encode(expected);
		var actual = new PostJarStoreTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}
}