package io.hotmoka.beans.marshalling.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.marshalling.MarshallingContext;
import io.hotmoka.marshalling.ObjectMarshaller;

/**
 * Knowledge about how a transaction reference can be marshalled.
 */
public class TransactionReferenceMarshaller extends ObjectMarshaller<TransactionReference> {
	
	private final Map<TransactionReference, Integer> memory = new HashMap<>();

	public TransactionReferenceMarshaller() {
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