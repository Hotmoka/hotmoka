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
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.NodeInfos;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.Updates;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
import io.hotmoka.node.messages.GetRequestMessages;
import io.hotmoka.node.messages.GetRequestResultMessages;
import io.hotmoka.node.messages.GetStateMessages;
import io.hotmoka.node.messages.GetStateResultMessages;
import io.hotmoka.node.messages.GetTakamakaCodeMessages;
import io.hotmoka.node.messages.GetTakamakaCodeResultMessages;
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
}