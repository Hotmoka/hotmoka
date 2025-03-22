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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.Updates;
import io.hotmoka.node.api.signatures.FieldSignature;
import io.hotmoka.node.api.updates.Update;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.testing.AbstractLoggedTests;

public class UpdateTests extends AbstractLoggedTests {
	private final static StorageReference object = StorageValues.reference
		(TransactionReferences.of("cafebabedeadbeafcafebabedeadbeafcafebabedeadbeafcafebabedeadbeaf"), BigInteger.valueOf(13));

	private final static FieldSignature field = FieldSignatures.of
		(StorageTypes.classNamed("io.hotmoka.MyClass"), "f1", StorageTypes.classNamed("io.hotmoka.OtherClass"));

	@Test
	@DisplayName("class tags are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForClassTag() throws Exception {
		var jar = TransactionReferences.of("01234567deadbeafcafebabedeadbeafcafebabedeadbeafcafebabedeadbeaf");
		var classTag1 = Updates.classTag(object, StorageTypes.classNamed("MyGreatClass"), jar);
		String encoded = new Updates.Encoder().encode(classTag1);
		var classTag2 = new Updates.Decoder().decode(encoded);
		assertEquals(classTag1, classTag2);
	}

	@Test
	@DisplayName("class tags are correctly marshalled and unmarshalled")
	public void marshalUnmarshalWorksForClassTag() throws Exception {
		var jar = TransactionReferences.of("01234567deadbeafcafebabedeadbeafcafebabedeadbeafcafebabedeadbeaf");
		var classTag1 = Updates.classTag(object, StorageTypes.classNamed("MyGreatClass"), jar);
		byte[] bytes;

		try (var baos = new ByteArrayOutputStream(); var context = NodeMarshallingContexts.of(baos)) {
			classTag1.into(context);
			context.flush();
			bytes = baos.toByteArray();
		}

		Update classTag2;
		try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))) {
			classTag2 = Updates.from(context);
		}

		assertEquals(classTag1, classTag2);
	}

	@Test
	@DisplayName("updates of big integer are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfBigInteger() throws Exception {
		var update1 = Updates.ofBigInteger(object, field, BigInteger.TEN);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of boolean are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfBoolean() throws Exception {
		var update1 = Updates.ofBoolean(object, field, true);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of byte are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfByte() throws Exception {
		var update1 = Updates.ofByte(object, field, (byte) 13);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of char are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfChar() throws Exception {
		var update1 = Updates.ofChar(object, field, 'A');
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of double are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfDouble() throws Exception {
		var update1 = Updates.ofDouble(object, field, 3.1415);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of float are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfFloat() throws Exception {
		var update1 = Updates.ofFloat(object, field, 3.1415f);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of int are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfInt() throws Exception {
		var update1 = Updates.ofInt(object, field, 1973);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of long are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfLong() throws Exception {
		var update1 = Updates.ofLong(object, field, 1234567890L);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of short are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfShort() throws Exception {
		var update1 = Updates.ofShort(object, field, (short) 1973);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of storage are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfStorage() throws Exception {
		var value = StorageValues.reference(TransactionReferences.of("01234567deadbeafcafebabedeadbeafcafebabedeadbeafcafebabedeadbeaf"), BigInteger.TWO);
		var update1 = Updates.ofStorage(object, field, value);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates of string are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateOfString() throws Exception {
		var update1 = Updates.ofString(object, field, "hello");
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}

	@Test
	@DisplayName("updates to null are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForUpdateToNull() throws Exception {
		var update1 = Updates.toNull(object, field, false);
		String encoded = new Updates.Encoder().encode(update1);
		var update2 = new Updates.Decoder().decode(encoded);
		assertEquals(update1, update2);
	}
}