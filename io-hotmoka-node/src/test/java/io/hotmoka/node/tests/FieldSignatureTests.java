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

import io.hotmoka.node.FieldSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.testing.AbstractLoggedTests;

public class FieldSignatureTests extends AbstractLoggedTests {

	@Test
	@DisplayName("field signatures of class type are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForFieldSignatureOfClassType() throws Exception {
		var field1 = FieldSignatures.of(StorageTypes.classNamed("io.hotmoka.MyClass"), "f1", StorageTypes.classNamed("io.hotmoka.OtherClass"));
		String encoded = new FieldSignatures.Encoder().encode(field1);
		var field2 = new FieldSignatures.Decoder().decode(encoded);
		assertEquals(field1, field2);
	}

	@Test
	@DisplayName("field signatures of basic type are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForFieldSignatureOfBasicType() throws Exception {
		var field1 = FieldSignatures.of(StorageTypes.classNamed("io.hotmoka.MyClass"), "f1", StorageTypes.INT);
		String encoded = new FieldSignatures.Encoder().encode(field1);
		var field2 = new FieldSignatures.Decoder().decode(encoded);
		assertEquals(field1, field2);
	}
}