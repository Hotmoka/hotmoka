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

import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.testing.AbstractLoggedTests;
import jakarta.websocket.DecodeException;
import jakarta.websocket.EncodeException;

public class MethodSignatureTests extends AbstractLoggedTests {

	@Test
	@DisplayName("void method signatures are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForVoidMethodSignature() throws EncodeException, DecodeException {
		var method1 = MethodSignatures.ofVoid("io.hotmoka.MyClass", "m", StorageTypes.named("io.hotmoka.OtherClass"), StorageTypes.CHAR, StorageTypes.DOUBLE, StorageTypes.named("io.hotmoka.Something"));
		String encoded = new MethodSignatures.Encoder().encode(method1);
		var method2 = new MethodSignatures.Decoder().decode(encoded);
		assertEquals(method1, method2);
	}

	@Test
	@DisplayName("non-void method signatures are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForNonVoidMethodSignature() throws EncodeException, DecodeException {
		var method1 = MethodSignatures.of("io.hotmoka.MyClass", "m", StorageTypes.FLOAT, StorageTypes.named("io.hotmoka.OtherClass"), StorageTypes.CHAR, StorageTypes.DOUBLE, StorageTypes.named("io.hotmoka.Something"));
		String encoded = new MethodSignatures.Encoder().encode(method1);
		var method2 = new MethodSignatures.Decoder().decode(encoded);
		assertEquals(method1, method2);
	}
}