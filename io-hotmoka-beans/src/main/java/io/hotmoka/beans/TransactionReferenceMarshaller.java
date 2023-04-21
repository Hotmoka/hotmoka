package io.hotmoka.beans;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.references.TransactionReference;

/**
 * Knowledge about how a transaction reference can be marshalled.
 */
class TransactionReferenceMarshaller extends ObjectMarshaller<TransactionReference> {
	
	private final Map<TransactionReference, Integer> memory = new HashMap<>();

	TransactionReferenceMarshaller() {
		super(TransactionReference.class);
	}

	@Override
	public void write(TransactionReference transaction, MarshallingContext context) throws IOException {
		Integer index = memory.get(transaction);
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
				throw new IllegalStateException("too many transaction references in the same context");

			memory.put(transaction, next);

			context.writeByte(255);
			context.write(transaction.getHashAsBytes());
		}
	}
}