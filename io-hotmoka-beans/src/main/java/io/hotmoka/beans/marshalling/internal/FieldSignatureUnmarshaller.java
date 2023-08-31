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

package io.hotmoka.beans.marshalling.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.signatures.FieldSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.types.StorageType;
import io.hotmoka.marshalling.AbstractObjectUnmarshaller;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * An unmarshaller for field signatures.
 */
public class FieldSignatureUnmarshaller extends AbstractObjectUnmarshaller<FieldSignature> {

	private final Map<Integer, FieldSignature> memory = new HashMap<>();

	public FieldSignatureUnmarshaller() {
		super(FieldSignature.class);
	}

	@Override
	public FieldSignature read(UnmarshallingContext context) throws IOException {
		int selector = context.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			var field = new FieldSignature((ClassType) StorageType.from(context), context.readStringUnshared(), StorageType.from(context));
			memory.put(memory.size(), field);
			return field;
		}
		else if (selector == 254)
			return memory.get(context.readInt());
		else
			return memory.get(selector);
	}
}