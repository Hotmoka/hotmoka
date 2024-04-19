/*
Copyright 2021 Fausto Spoto

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

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.marshalling.AbstractObjectMarshaller;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Knowledge about how a storage reference can be marshalled.
 */
class StorageReferenceMarshaller extends AbstractObjectMarshaller<StorageReference> {
	
	private final Map<StorageReference, Integer> memory = new HashMap<>();

	StorageReferenceMarshaller() {
		super(StorageReference.class);
	}

	@Override
	public void write(StorageReference reference, MarshallingContext context) throws IOException {
		Integer index = memory.get(reference);
		if (index != null) {
			if (index < 254)
				context.writeByte(index);
			else {
				context.writeByte(254);
				context.writeInt(index);
			}
		}
		else {
			int next = memory.size();
			if (next == Integer.MAX_VALUE) // irrealistic
				throw new IllegalStateException("too many storage references in the same context");

			memory.put(reference, next);

			context.writeByte(255);
			reference.getTransaction().into(context);
			context.writeBigInteger(reference.getProgressive());
		}
	}
}