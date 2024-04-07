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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.NodeInfos;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.Updates;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.messages.GetClassTagMessages;
import io.hotmoka.node.messages.GetClassTagResultMessages;
import io.hotmoka.node.messages.GetManifestMessages;
import io.hotmoka.node.messages.GetManifestResultMessages;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
import io.hotmoka.node.messages.GetTakamakaCodeMessages;
import io.hotmoka.node.messages.GetTakamakaCodeResultMessages;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class MessagesTests extends AbstractLoggedTests {

	private final static StorageReference OBJECT = StorageValues.reference(TransactionReferences.of("12345678901234567890abcdeabcdeff12345678901234567890abcdeabcdeff"), BigInteger.ONE);

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
}