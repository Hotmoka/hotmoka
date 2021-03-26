package io.hotmoka.beans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
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
	private final ObjectInputStream ois;
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
			StorageReference reference = new StorageReference(TransactionReference.from(this), readBigInteger());
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

	public byte readByte() throws IOException {
		return ois.readByte();
	}

	public char readChar() throws IOException {
		return ois.readChar();
	}

	public boolean readBoolean() throws IOException {
		return ois.readBoolean();
	}

	public int readInt() throws IOException {
		return ois.readInt();
	}

	/**
	 * Reads a small integer.
	 * 
	 * @return the integer
	 * @throws IOException if the integer cannot be written
	 */
	public int readCompactInt() throws IOException {
		int i = readByte();
		if (i < 0)
			i += 256;

		if (i == 255)
			i = readInt();

		return i;
	}

	public short readShort() throws IOException {
		return ois.readShort();
	}

	public long readLong() throws IOException {
		return ois.readLong();
	}

	public float readFloat() throws IOException {
		return ois.readFloat();
	}

	public double readDouble() throws IOException {
		return ois.readDouble();
	}

	public String readUTF() throws IOException {
		return ois.readUTF();
	}

	public byte[] readBytes(int length, String errorMessage) throws IOException {
		byte[] bytes = new byte[length];
		if (length != ois.readNBytes(bytes, 0, length))
			throw new IOException(errorMessage);

		return bytes;
	}

	public Object readObject() throws ClassNotFoundException, IOException {
		return ois.readObject();
	}

	/**
	 * Reads a big integer, taking into account
	 * optimized representations used for the big integer.
	 * 
	 * @return the big integer
	 * @throws ClassNotFoundException if the big integer could not be written
	 * @throws IOException if the big integer could not be written
	 */
	public BigInteger readBigInteger() throws ClassNotFoundException, IOException {
		byte selector = readByte();
		switch (selector) {
		case 0: return BigInteger.valueOf(readShort());
		case 1: return BigInteger.valueOf(readInt());
		case 2: return BigInteger.valueOf(readLong());
		case 3: {
			int numBytes = readCompactInt();
			return new BigInteger(readBytes(numBytes, "BigInteger length mismatch"));
		}
		default: {
			if (selector - 4 < 0)
				return BigInteger.valueOf(selector + 252);
			else
				return BigInteger.valueOf(selector - 4);
		}
		}
	}
}