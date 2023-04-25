package io.hotmoka.beans.marshalling.internal;

import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.marshalling.AbstractObjectMarshaller;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * Knowledge about how a transaction reference can be marshalled.
 */
public class TransactionReferenceMarshaller extends AbstractObjectMarshaller<TransactionReference> {
	
	private final Map<TransactionReference, Integer> memory = new HashMap<>();

	public TransactionReferenceMarshaller() {
		super(TransactionReference.class);
	}

	@Override
	public void write(TransactionReference transaction, MarshallingContext context) {
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