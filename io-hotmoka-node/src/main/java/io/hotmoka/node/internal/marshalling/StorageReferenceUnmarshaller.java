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

package io.hotmoka.node.internal.marshalling;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.marshalling.AbstractObjectUnmarshaller;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.values.StorageReference;

/**
 * An unmarshaller for storage references.
 */
class StorageReferenceUnmarshaller extends AbstractObjectUnmarshaller<StorageReference> {

	private final Map<Integer, StorageReference> memory = new HashMap<>();

	StorageReferenceUnmarshaller() {
		super(StorageReference.class);
	}

	@Override
	public StorageReference read(UnmarshallingContext context) throws IOException {
		int selector = context.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			var reference = StorageValues.reference(TransactionReferences.from(context), context.readBigInteger());

			int size = memory.size();
			if (size < Integer.MAX_VALUE)
				memory.put(size, reference);

			return reference;
		}
		else if (selector == 254)
			return memory.get(context.readInt());
		else
			return memory.get(selector);
	}
}