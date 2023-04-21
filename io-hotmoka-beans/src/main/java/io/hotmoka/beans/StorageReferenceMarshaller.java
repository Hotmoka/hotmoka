package io.hotmoka.beans;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.values.StorageReference;

/**
 * Knowledge about how a storage reference can be marshalled.
 */
class StorageReferenceMarshaller extends ObjectMarshaller<StorageReference> {
	
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
			reference.transaction.into(context);
			context.writeBigInteger(reference.progressive);
		}
	}
}