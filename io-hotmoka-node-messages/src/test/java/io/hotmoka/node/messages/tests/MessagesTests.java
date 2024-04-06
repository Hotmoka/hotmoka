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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.NodeInfos;
import io.hotmoka.node.messages.GetNodeInfoMessages;
import io.hotmoka.node.messages.GetNodeInfoResultMessages;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class MessagesTests extends AbstractLoggedTests {

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
}