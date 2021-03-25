package io.hotmoka.beans.references;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

import io.hotmoka.beans.Marshallable;
import io.hotmoka.beans.annotations.Immutable;

/**
 * A unique identifier for a transaction.
 */
@Immutable
public abstract class TransactionReference extends Marshallable implements Comparable<TransactionReference> {

	/**
	 * Yields the hash of the request that generated the transaction.
	 * 
	 * @return the hash
	 */
	public abstract String getHash();

	/**
	 * Factory method that unmarshals a transaction reference from the given stream.
	 * 
	 * @param ois the stream
	 * @return the transaction reference
	 * @throws IOException if the transaction reference could not be unmarshalled
	 * @throws ClassNotFoundException if the transaction reference could not be unmarshalled
	 */
	public static TransactionReference from(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		return new LocalTransactionReference(bytesToHex(readSharedByteArray(ois)));
	}

	private static byte[] readSharedByteArray(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		return (byte[]) ois.readObject();
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