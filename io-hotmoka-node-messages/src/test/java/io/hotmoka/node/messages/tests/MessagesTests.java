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
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.NodeInfos;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.TransactionResponses;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.requests.ConstructorCallTransactionRequest;
import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.api.requests.JarStoreTransactionRequest;
import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.AddConstructorCallTransactionMessages;
import io.hotmoka.node.messages.AddConstructorCallTransactionResultMessages;
import io.hotmoka.node.messages.AddGameteCreationTransactionMessages;
import io.hotmoka.node.messages.AddGameteCreationTransactionResultMessages;
import io.hotmoka.node.messages.AddInitializationTransactionMessages;
import io.hotmoka.node.messages.AddInitializationTransactionResultMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreInitialTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreInitialTransactionResultMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionMessages;
import io.hotmoka.node.messages.AddJarStoreTransactionResultMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.AddStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.EventMessages;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.GetConfigMessages;
import io.hotmoka.node.messages.GetConfigResultMessages;
import io.hotmoka.node.messages.GetIndexMessages;
import io.hotmoka.node.messages.GetIndexResultMessages;
import io.hotmoka.node.messages.GetInfoMessages;
import io.hotmoka.node.messages.GetInfoResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
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
import io.hotmoka.node.messages.PostStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.PostStaticMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunInstanceMethodCallTransactionResultMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionMessages;
import io.hotmoka.node.messages.RunStaticMethodCallTransactionResultMessages;
import io.hotmoka.testing.AbstractLoggedTests;

public class MessagesTests extends AbstractLoggedTests {
	private final static TransactionReference TRANSACTION_REFERENCE = TransactionReferences.of("12345678901234567890abcdeabcdeff12345678901234567890abcdeabcdeff");
	private final static StorageReference OBJECT = StorageValues.reference(TRANSACTION_REFERENCE, BigInteger.ONE);
	private final static NonVoidMethodSignature TARGET = MethodSignatures.ofNonVoid(StorageTypes.classNamed("my.class"), "target", StorageTypes.STRING, StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);

	@Test
	@DisplayName("getInfo messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetInfo() throws Exception {
		var expected = GetInfoMessages.of("id");
		String encoded = new GetInfoMessages.Encoder().encode(expected);
		var actual = new GetInfoMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getInfoResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetInfoResult() throws Exception {
		var expected = GetInfoResultMessages.of(NodeInfos.of("special node", "1.2.3", "id314"), "id");
		String encoded = new GetInfoResultMessages.Encoder().encode(expected);
		var actual = new GetInfoResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getConsensusConfig messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetConsensusConfig() throws Exception {
		var expected = GetConfigMessages.of("id");
		String encoded = new GetConfigMessages.Encoder().encode(expected);
		var actual = new GetConfigMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getConsensusConfigResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetConsensusConfigResult() throws Exception {
		var config = ConsensusConfigBuilders.defaults()
			.setChainId("my chain")
			.setInitialSupply(BigInteger.valueOf(100L))
			.build();
		var expected = GetConfigResultMessages.of(config, "id");
		String encoded = new GetConfigResultMessages.Encoder().encode(expected);
		var actual = new GetConfigResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getTakamakaCode messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetTakamakaCode() throws Exception {
		var expected = GetTakamakaCodeMessages.of("id");
		String encoded = new GetTakamakaCodeMessages.Encoder().encode(expected);
		var actual = new GetTakamakaCodeMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getTakamakaCodeResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetTakamakaCodeResult() throws Exception {
		var expected = GetTakamakaCodeResultMessages.of(TransactionReferences.of("12345678901234567890abcdeabcdeff12345678901234567890abcdeabcdeff"), "id");
		String encoded = new GetTakamakaCodeResultMessages.Encoder().encode(expected);
		var actual = new GetTakamakaCodeResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getManifest messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetManifest() throws Exception {
		var expected = GetManifestMessages.of("id");
		String encoded = new GetManifestMessages.Encoder().encode(expected);
		var actual = new GetManifestMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getManifestResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetManifestResult() throws Exception {
		var expected = GetManifestResultMessages.of(OBJECT, "id");
		String encoded = new GetManifestResultMessages.Encoder().encode(expected);
		var actual = new GetManifestResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getClassTag messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetClassTag() throws Exception {
		var expected = GetClassTagMessages.of(OBJECT, "id");
		String encoded = new GetClassTagMessages.Encoder().encode(expected);
		var actual = new GetClassTagMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getClassTagResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetClassTagResult() throws Exception {
		var expected = GetClassTagResultMessages.of(Updates.classTag(OBJECT, StorageTypes.classNamed("my.class"), OBJECT.getTransaction()), "id");
		String encoded = new GetClassTagResultMessages.Encoder().encode(expected);
		var actual = new GetClassTagResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getState messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetState() throws Exception {
		var expected = GetStateMessages.of(OBJECT, "id");
		String encoded = new GetStateMessages.Encoder().encode(expected);
		var actual = new GetStateMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getStateResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetStateResult() throws Exception {
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
	@DisplayName("getIndex messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetIndex() throws Exception {
		var expected = GetIndexMessages.of(OBJECT, "id");
		String encoded = new GetIndexMessages.Encoder().encode(expected);
		var actual = new GetIndexMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getIndexResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetIndexResult() throws Exception {
		var random = new Random();
		byte[] bytes1 = new byte[TransactionReference.REQUEST_HASH_LENGTH];
		random.nextBytes(bytes1);
		TransactionReference reference1 = TransactionReferences.of(bytes1);
		byte[] bytes2 = new byte[TransactionReference.REQUEST_HASH_LENGTH];
		random.nextBytes(bytes2);
		TransactionReference reference2 = TransactionReferences.of(bytes2);
		byte[] bytes3 = new byte[TransactionReference.REQUEST_HASH_LENGTH];
		random.nextBytes(bytes3);
		TransactionReference reference3 = TransactionReferences.of(bytes3);
	
		var expected = GetIndexResultMessages.of(Stream.of(reference1, reference2, reference3), "id");
		String encoded = new GetIndexResultMessages.Encoder().encode(expected);
		var actual = new GetIndexResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getRequest messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetRequest() throws Exception {
		var expected = GetRequestMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new GetRequestMessages.Encoder().encode(expected);
		var actual = new GetRequestMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getRequestResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetRequestResult() throws Exception {
		var request = TransactionRequests.gameteCreation(TRANSACTION_REFERENCE, BigInteger.valueOf(10_000_000L), Base64.toBase64String(SignatureAlgorithms.ed25519().getKeyPair().getPublic().getEncoded()));
		var expected = GetRequestResultMessages.of(request, "id");
		String encoded = new GetRequestResultMessages.Encoder().encode(expected);
		var actual = new GetRequestResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getResponse messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetResponse() throws Exception {
		var expected = GetResponseMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new GetResponseMessages.Encoder().encode(expected);
		var actual = new GetResponseMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getResponseResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetResponseResult() throws Exception {
		ClassType clazz = StorageTypes.classNamed("io.my.Class");
		var update1 = Updates.ofInt(OBJECT, FieldSignatures.of(clazz, "field1", StorageTypes.INT), 42);
		var update2 = Updates.ofBigInteger(OBJECT, FieldSignatures.of(clazz, "field2", StorageTypes.BIG_INTEGER), BigInteger.valueOf(13L));
		var update3 = Updates.ofString(OBJECT, FieldSignatures.of(clazz, "field3", StorageTypes.STRING), "hello");
		var response = TransactionResponses.constructorCallException(Stream.of(update1, update2, update3), Stream.of(OBJECT),
			BigInteger.valueOf(42L), BigInteger.valueOf(13L), BigInteger.valueOf(17L), "io.my.Exception", "code exploded", null);

		var expected = GetResponseResultMessages.of(response, "id");
		String encoded = new GetResponseResultMessages.Encoder().encode(expected);
		var actual = new GetResponseResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getPolledResponse messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetPolledResponse() throws Exception {
		var expected = GetPolledResponseMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new GetPolledResponseMessages.Encoder().encode(expected);
		var actual = new GetPolledResponseMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("getPolledResponseResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForGetPolledResponseResult() throws Exception {
		ClassType clazz = StorageTypes.classNamed("io.my.Class");
		var update1 = Updates.ofInt(OBJECT, FieldSignatures.of(clazz, "field1", StorageTypes.INT), 42);
		var update2 = Updates.ofBigInteger(OBJECT, FieldSignatures.of(clazz, "field2", StorageTypes.BIG_INTEGER), BigInteger.valueOf(13L));
		var update3 = Updates.ofString(OBJECT, FieldSignatures.of(clazz, "field3", StorageTypes.STRING), "hello");
		var response = TransactionResponses.constructorCallException(Stream.of(update1, update2, update3), Stream.of(OBJECT),
			BigInteger.valueOf(42L), BigInteger.valueOf(13L), BigInteger.valueOf(17L), "io.my.Exception", "code exploded", null);

		var expected = GetPolledResponseResultMessages.of(response, "id");
		String encoded = new GetPolledResponseResultMessages.Encoder().encode(expected);
		var actual = new GetPolledResponseResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runInstanceMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunInstanceMethodCallTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), InstanceMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.instanceMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain",
			BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE, TARGET, OBJECT,
			StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = RunInstanceMethodCallTransactionMessages.of(request, "id");
		String encoded = new RunInstanceMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new RunInstanceMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runInstanceMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunInstanceMethodCallNonVoidTransactionResult() throws Exception {
		var expected = RunInstanceMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new RunInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runInstanceMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunInstanceMethodCallVoidTransactionResult() throws Exception {
		var expected = RunInstanceMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new RunInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runStaticMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunStaticMethodCallTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), StaticMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.staticMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L),
			TRANSACTION_REFERENCE, TARGET, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = RunStaticMethodCallTransactionMessages.of(request, "id");
		String encoded = new RunStaticMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new RunStaticMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runStaticMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunStaticMethodCallNonVoidTransactionResult() throws Exception {
		var expected = RunStaticMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new RunStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("runStaticMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForRunStaticMethodCallVoidTransactionResult() throws Exception {
		var expected = RunStaticMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new RunStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new RunStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInstanceMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInstanceMethodCallTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), InstanceMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.instanceMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L),
			BigInteger.valueOf(17L), TRANSACTION_REFERENCE, TARGET, OBJECT,
			StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = AddInstanceMethodCallTransactionMessages.of(request, "id");
		String encoded = new AddInstanceMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new AddInstanceMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInstanceMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInstanceMethodCallNonVoidTransactionResult() throws Exception {
		var expected = AddInstanceMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new AddInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInstanceMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInstanceMethodCallVoidTransactionResult() throws Exception {
		var expected = AddInstanceMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new AddInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addStaticMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddStaticMethodCallTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), StaticMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.staticMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L),
				TRANSACTION_REFERENCE, TARGET, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = AddStaticMethodCallTransactionMessages.of(request, "id");
		String encoded = new AddStaticMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new AddStaticMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addStaticMethodCallTransactionResult messages for non-void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddStaticMethodCallNonVoidTransactionResult() throws Exception {
		var expected = AddStaticMethodCallTransactionResultMessages.of(Optional.of(StorageValues.stringOf("hello")), "id");
		String encoded = new AddStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addStaticMethodCallTransactionResult messages for void methods are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddStaticMethodCallVoidTransactionResult() throws Exception {
		var expected = AddStaticMethodCallTransactionResultMessages.of(Optional.empty(), "id");
		String encoded = new AddStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addConstructorCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddConstructorCallTransaction() throws Exception {
		var constructor = ConstructorSignatures.of(StorageTypes.classNamed("my.class"), StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), ConstructorCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.constructorCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE,
			constructor, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = AddConstructorCallTransactionMessages.of(request, "id");
		String encoded = new AddConstructorCallTransactionMessages.Encoder().encode(expected);
		var actual = new AddConstructorCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addConstructorCallTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddConstructorCallTransactionResult() throws Exception {
		var expected = AddConstructorCallTransactionResultMessages.of(OBJECT, "id");
		String encoded = new AddConstructorCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddConstructorCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addJarStoreTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddJarStoreTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), JarStoreTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.jarStore(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L),
			TRANSACTION_REFERENCE,
			"These are the bytes of a very large jar that must be installed in the Hotmoka node".getBytes());
		var expected = AddJarStoreTransactionMessages.of(request, "id");
		String encoded = new AddJarStoreTransactionMessages.Encoder().encode(expected);
		var actual = new AddJarStoreTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addJarStoreTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddJarStoreTransactionResult() throws Exception {
		var expected = AddJarStoreTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new AddJarStoreTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddJarStoreTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addGameteCreationTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddGameteCreationTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var request = TransactionRequests.gameteCreation(TRANSACTION_REFERENCE, BigInteger.valueOf(42424242L), Base64.toBase64String(keys.getPublic().getEncoded()));
		var expected = AddGameteCreationTransactionMessages.of(request, "id");
		String encoded = new AddGameteCreationTransactionMessages.Encoder().encode(expected);
		var actual = new AddGameteCreationTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addGameteCreationTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddGameteCreationTransactionResult() throws Exception {
		var expected = AddGameteCreationTransactionResultMessages.of(OBJECT, "id");
		String encoded = new AddGameteCreationTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddGameteCreationTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addJarStoreInitialTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddJarStoreInitialTransaction() throws Exception {
		var request = TransactionRequests.jarStoreInitial("These are the bytes of a very large jar that must be installed in the Hotmoka node".getBytes(), new TransactionReference[0]);
		var expected = AddJarStoreInitialTransactionMessages.of(request, "id");
		String encoded = new AddJarStoreInitialTransactionMessages.Encoder().encode(expected);
		var actual = new AddJarStoreInitialTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addJarStoreInitialTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddJarStoreInitialTransactionResult() throws Exception {
		var expected = AddJarStoreInitialTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new AddJarStoreInitialTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddJarStoreInitialTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInitializationTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInitializationTransaction() throws Exception {
		var request = TransactionRequests.initialization(TRANSACTION_REFERENCE, OBJECT);
		var expected = AddInitializationTransactionMessages.of(request, "id");
		String encoded = new AddInitializationTransactionMessages.Encoder().encode(expected);
		var actual = new AddInitializationTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("addInitializationTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForAddInitializationTransactionResult() throws Exception {
		var expected = AddInitializationTransactionResultMessages.of("id");
		String encoded = new AddInitializationTransactionResultMessages.Encoder().encode(expected);
		var actual = new AddInitializationTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postConstructorCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostConstructorCallTransaction() throws Exception {
		var constructor = ConstructorSignatures.of(StorageTypes.classNamed("my.class"), StorageTypes.BOOLEAN, StorageTypes.FLOAT, StorageTypes.INT);
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), ConstructorCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.constructorCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L), TRANSACTION_REFERENCE,
			constructor, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = PostConstructorCallTransactionMessages.of(request, "id");
		String encoded = new PostConstructorCallTransactionMessages.Encoder().encode(expected);
		var actual = new PostConstructorCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postConstructorCallTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostConstructorCallTransactionResult() throws Exception {
		var expected = PostConstructorCallTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new PostConstructorCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new PostConstructorCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postInstanceMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostInstanceMethodCallTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), InstanceMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.instanceMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L),
			BigInteger.valueOf(17L), TRANSACTION_REFERENCE, TARGET, OBJECT,
			StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = PostInstanceMethodCallTransactionMessages.of(request, "id");
		String encoded = new PostInstanceMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new PostInstanceMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postInstanceMethodCallTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostInstanceMethodCallTransactionResult() throws Exception {
		var expected = PostInstanceMethodCallTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new PostInstanceMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new PostInstanceMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postStaticMethodCallTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostStaticMethodCallTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), StaticMethodCallTransactionRequest::toByteArrayWithoutSignature);
		var request = TransactionRequests.staticMethodCall(signer, OBJECT, BigInteger.valueOf(13L), "my_chain", BigInteger.valueOf(1000L), BigInteger.valueOf(17L),
			TRANSACTION_REFERENCE, TARGET, StorageValues.FALSE, StorageValues.floatOf(3.14f), StorageValues.intOf(2024));
		var expected = PostStaticMethodCallTransactionMessages.of(request, "id");
		String encoded = new PostStaticMethodCallTransactionMessages.Encoder().encode(expected);
		var actual = new PostStaticMethodCallTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postStaticMethodCallTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostStaticMethodCallTransactionResult() throws Exception {
		var expected = PostStaticMethodCallTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new PostStaticMethodCallTransactionResultMessages.Encoder().encode(expected);
		var actual = new PostStaticMethodCallTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postJarStoreTransaction messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostJarStoreTransaction() throws Exception {
		var ed25519 = SignatureAlgorithms.ed25519();
		var keys = ed25519.getKeyPair();
		var signer = ed25519.getSigner(keys.getPrivate(), JarStoreTransactionRequest::toByteArrayWithoutSignature);
		/*(Signer<? super JarStoreTransactionRequest> signer,
				StorageReference caller,
				BigInteger nonce,
				String chainId,
				BigInteger gasLimit,ù
				BigInteger gasPrice,
				TransactionReference classpath,
				byte[] jar,
				TransactionReference[] dependencies,
				ExceptionSupplier<? extends E> onIllegalArgs) throws E, InvalidKeyException, */
		var request = TransactionRequests.jarStore(signer, OBJECT, BigInteger.valueOf(13L), "my_chain",
			BigInteger.valueOf(1000L), BigInteger.valueOf(17L),
			TRANSACTION_REFERENCE,
			"These are the bytes of a very large jar that must be installed in the Hotmoka node".getBytes());
		var expected = PostJarStoreTransactionMessages.of(request, "id");
		String encoded = new PostJarStoreTransactionMessages.Encoder().encode(expected);
		var actual = new PostJarStoreTransactionMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("postJarStoreTransactionResult messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForPostJarStoreTransactionResult() throws Exception {
		var expected = PostJarStoreTransactionResultMessages.of(TRANSACTION_REFERENCE, "id");
		String encoded = new PostJarStoreTransactionResultMessages.Encoder().encode(expected);
		var actual = new PostJarStoreTransactionResultMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}

	@Test
	@DisplayName("event messages are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForEvent() throws Exception {
		var expected = EventMessages.of(OBJECT, StorageValues.reference(TRANSACTION_REFERENCE, BigInteger.TWO));
		String encoded = new EventMessages.Encoder().encode(expected);
		var actual = new EventMessages.Decoder().decode(encoded);
		assertEquals(expected, actual);
	}
}