package io.hotmoka.beans;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.signatures.FieldSignature;

/**
 * Knowledge about how a field signature can be marshalled.
 */
class FieldSignatureMarshaller extends ObjectMarshaller<FieldSignature> {
	
	private final Map<FieldSignature, Integer> memory = new HashMap<>();

	FieldSignatureMarshaller() {
		super(FieldSignature.class);
	}

	@Override
	public void write(FieldSignature field, MarshallingContext context) throws IOException {
		Integer index = memory.get(field);
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
				throw new IllegalStateException("too many field signatures in the same context");

			memory.put(field, next);

			context.writeByte(255);
			field.definingClass.into(context);
			context.writeUTF(field.name);
			field.type.into(context);
		}
	}
}