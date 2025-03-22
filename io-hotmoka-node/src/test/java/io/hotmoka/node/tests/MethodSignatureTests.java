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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.NodeUnmarshallingContexts;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.api.signatures.MethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.testing.AbstractLoggedTests;

public class MethodSignatureTests extends AbstractLoggedTests {

	private static final ClassType MY_CLASS = StorageTypes.classNamed("io.hotmoka.MyClass");

	@Test
	@DisplayName("void method signatures are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForVoidMethodSignature() throws Exception {
		var method1 = MethodSignatures.ofVoid(MY_CLASS, "m", StorageTypes.classNamed("io.hotmoka.OtherClass"), StorageTypes.CHAR, StorageTypes.DOUBLE, StorageTypes.classNamed("io.hotmoka.Something"));
		String encoded = new MethodSignatures.Encoder().encode(method1);
		var method2 = new MethodSignatures.Decoder().decode(encoded);
		assertEquals(method1, method2);
	}

	@Test
	@DisplayName("void method signatures are correctly marshalled and unmarshalled")
	public void marshalUnmarshalWorksForVoidMethodSignature() throws Exception {
		var method1 = MethodSignatures.ofVoid(MY_CLASS, "m", StorageTypes.classNamed("io.hotmoka.OtherClass"), StorageTypes.CHAR, StorageTypes.DOUBLE, StorageTypes.classNamed("io.hotmoka.Something"));
		byte[] bytes;

		try (var baos = new ByteArrayOutputStream(); var context = NodeMarshallingContexts.of(baos)) {
			method1.into(context);
			context.flush();
			bytes = baos.toByteArray();
		}

		MethodSignature method2;
		try (var context = NodeUnmarshallingContexts.of(new ByteArrayInputStream(bytes))) {
			method2 = MethodSignatures.from(context);
		}

		assertEquals(method1, method2);
	}

	@Test
	@DisplayName("non-void method signatures are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForNonVoidMethodSignature() throws Exception {
		var method1 = MethodSignatures.ofNonVoid(MY_CLASS, "m", StorageTypes.FLOAT, StorageTypes.classNamed("io.hotmoka.OtherClass"), StorageTypes.CHAR, StorageTypes.DOUBLE, StorageTypes.classNamed("io.hotmoka.Something"));
		String encoded = new MethodSignatures.Encoder().encode(method1);
		var method2 = new MethodSignatures.Decoder().decode(encoded);
		assertEquals(method1, method2);
	}

	@Test
	@DisplayName("non-void method signatures without formals are correctly encoded into Json and decoded from Json")
	public void encodeDecodeWorksForNonVoidMethodSignatureWithoutFormals() throws Exception {
		var method1 = MethodSignatures.ofNonVoid(MY_CLASS, "m", StorageTypes.FLOAT);
		String encoded = new MethodSignatures.Encoder().encode(method1);
		var method2 = new MethodSignatures.Decoder().decode(encoded);
		assertEquals(method1, method2);
	}
}