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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.StorageTypes;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class StorageTypeTests extends AbstractLoggedTests {

	@Test
	@DisplayName("basic types are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForBasicType() throws EncodeException, DecodeException {
		var type1 = StorageTypes.INT;
		String encoded = new StorageTypes.Encoder().encode(type1);
		var type2 = new StorageTypes.Decoder().decode(encoded);
		assertEquals(type1, type2);
	}

	@Test
	@DisplayName("class types are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForClassType() throws EncodeException, DecodeException {
		var type1 = StorageTypes.STRING;
		String encoded = new StorageTypes.Encoder().encode(type1);
		var type2 = new StorageTypes.Decoder().decode(encoded);
		assertEquals(type1, type2);
	}
}