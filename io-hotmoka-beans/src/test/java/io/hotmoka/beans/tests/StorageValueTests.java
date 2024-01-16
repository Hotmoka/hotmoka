/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.beans.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class StorageValueTests extends AbstractLoggedTests {

	@Test
	@DisplayName("big integer storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForBigIntegerStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.bigIntegerOf(BigInteger.valueOf(12345678));
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("boolean storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForBooleanStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.booleanOf(true);
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("byte storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForByteStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.byteOf((byte) 13);
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("char storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForCharStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.charOf('@');
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("double storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForDoubleStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.doubleOf(3.1415);
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("enum storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForEnumStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.enumElementOf("io.hotmoka.Season", "SPRING");
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("float storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForFloatStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.floatOf(3.1415f);
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("int storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForIntStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.intOf(2024);
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("long storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForLongStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.longOf(123456789L);
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("null storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForNullStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.NULL;
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("short storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForShortStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.shortOf((short) 1234);
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("storage references are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForStorageReference() throws EncodeException, DecodeException {
		var value1 = StorageValues.reference(TransactionReferences.of("cafebabe12345678cafebabe12345678cafebabe12345678cafebabe12345678"), BigInteger.valueOf(13));
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}

	@Test
	@DisplayName("string storage values are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForStringStorageValue() throws EncodeException, DecodeException {
		var value1 = StorageValues.stringOf("hello");
		String encoded = new StorageValues.Encoder().encode(value1);
		var value2 = new StorageValues.Decoder().decode(encoded);
		assertEquals(value1, value2);
	}
}