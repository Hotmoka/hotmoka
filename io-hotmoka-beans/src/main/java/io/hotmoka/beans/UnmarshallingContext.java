package io.hotmoka.beans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.values.StorageReference;

/**
 * A context used during bytes unmarshalling into objects.
 */
public class UnmarshallingContext {
	public final ObjectInputStream ois;
	private final Map<Integer, StorageReference> memoryStorageReference = new HashMap<>();

	public UnmarshallingContext(ObjectInputStream ois) {
		this.ois = ois;
	}

	/**
	 * Reads a storage reference from this context. It uses progressive counters to
	 * decompress repeated storage references for the same context.
	 * 
	 * @return the storage reference
	 */
	public StorageReference readStorageReference() throws ClassNotFoundException, IOException {
		int selector = ois.readByte();
		if (selector == 0) {
			StorageReference reference = new StorageReference(TransactionReference.from(this), Marshallable.unmarshallBigInteger(this));
			memoryStorageReference.put(memoryStorageReference.size(), reference);
			return reference;
		}
		else
			return memoryStorageReference.get(Marshallable.readCompactInt(this));
	}
}