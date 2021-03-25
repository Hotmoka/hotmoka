package io.hotmoka.beans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.beans.values.StorageReference;

/**
 * A context used during bytes unmarshalling into objects.
 */
public class UnmarshallingContext {
	public final ObjectInputStream ois;
	private final Map<Integer, StorageReference> memoryStorageReference = new HashMap<>();
	private final Map<Integer, TransactionReference> memoryTransactionReference = new HashMap<>();

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
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			StorageReference reference = new StorageReference(TransactionReference.from(this), Marshallable.unmarshallBigInteger(this));
			memoryStorageReference.put(memoryStorageReference.size(), reference);
			return reference;
		}
		else if (selector == 254)
			return memoryStorageReference.get(ois.readInt());
		else
			return memoryStorageReference.get(selector);
	}

	/**
	 * Reads a transaction reference from this context. It uses progressive counters to
	 * decompress repeated transaction references for the same context.
	 * 
	 * @return the transaction reference
	 */
	public TransactionReference readTransactionReference() throws ClassNotFoundException, IOException {
		int selector = ois.readByte();
		if (selector < 0)
			selector = 256 + selector;

		if (selector == 255) {
			byte[] bytes = ois.readNBytes(TransactionRequest.hashingForRequests.length());
			TransactionReference reference = new LocalTransactionReference(bytesToHex(bytes));
			memoryTransactionReference.put(memoryTransactionReference.size(), reference);
			return reference;
		}
		else if (selector == 254)
			return memoryTransactionReference.get(ois.readInt());
		else
			return memoryTransactionReference.get(selector);
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    int pos = 0;
	    for (byte b: bytes) {
	        int v = b & 0xFF;
	        hexChars[pos++] = HEX_ARRAY[v >>> 4];
	        hexChars[pos++] = HEX_ARRAY[v & 0x0F];
	    }
	
	    return new String(hexChars, StandardCharsets.UTF_8);
	}

	/**
	 * The string of the hexadecimal digits.
	 */
	private final static String HEX_CHARS = "0123456789abcdef";

	/**
	 * The array of hexadecimal digits.
	 */
	private final static byte[] HEX_ARRAY = HEX_CHARS.getBytes();
}