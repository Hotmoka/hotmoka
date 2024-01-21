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

package io.hotmoka.beans.internal.marshalling;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.marshalling.AbstractObjectUnmarshaller;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * An unmarshaller for field signatures.
 */
class FieldSignatureUnmarshaller extends AbstractObjectUnmarshaller<FieldSignature> {

	private final Map<Integer, FieldSignature> memory = new HashMap<>();

	FieldSignatureUnmarshaller() {
		super(FieldSignature.class);
	}

	@Override
	public FieldSignature read(UnmarshallingContext context) throws IOException {
		int selector = context.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			try {
				var field = FieldSignatures.of((ClassType) StorageTypes.from(context), context.readStringUnshared(), StorageTypes.from(context));
				memory.put(memory.size(), field);
				return field;
			}
			catch (ClassCastException e) {
				throw new IOException("Failed field unmarshalling", e);
			}
		}
		else if (selector == 254)
			return memory.get(context.readInt());
		else
			return memory.get(selector);
	}
}