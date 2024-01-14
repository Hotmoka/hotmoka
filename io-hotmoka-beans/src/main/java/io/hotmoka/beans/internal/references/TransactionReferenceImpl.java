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

package io.hotmoka.beans.internal.references;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.marshalling.BeanMarshallingContext;
import io.hotmoka.beans.requests.TransactionRequest;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Implementation of a transaction reference that refers to a transaction in the store of a node.
 */
public final class TransactionReferenceImpl extends AbstractMarshallable implements TransactionReference {
	private static final long serialVersionUID = 1157696417214320999L;

	/**
	 * The hash of the request that generated the transaction.
	 */
	private final String hash;

	/**
	 * Builds a transaction reference.
	 * 
	 * @param hash the hash of the transaction, as the hexadecimal representation of its bytes
	 */
	public TransactionReferenceImpl(String hash) {
		this.hash = Objects.requireNonNull(hash, "hash cannot be null").toLowerCase();

		// each byte is represented by two successive characters
		if (hash.length() != TransactionRequest.REQUEST_HASH_LENGTH * 2)
			throw new IllegalArgumentException("Illegal transaction reference " + hash
				+ ": it should hold a hash of " + TransactionRequest.REQUEST_HASH_LENGTH * 2 + " characters");

		if (!hash.chars().allMatch(c -> (c >= '0' && c <='9') || (c >= 'a' && c <= 'f')))
			throw new IllegalArgumentException("Illegal transaction reference " + hash + ": it must be a hexadecimal number");
	}

	/**
	 * Builds a transaction reference.
	 * 
	 * @param hash the hash of the transaction, as a byte array
	 */
	public TransactionReferenceImpl(byte[] hash) {
		this(bytesToHex(hash));
	}

	/**
	 * Yields a transaction reference unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction reference
	 * @throws IOException if the reference could not be unmarshalled
     */
	public static TransactionReference from(UnmarshallingContext context) throws IOException {
		return context.readObject(TransactionReference.class);
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new BeanMarshallingContext(os);
	}

	/**
	 * Translates an array of bytes into a hexadecimal string.
	 * 
	 * @param bytes the bytes
	 * @return the string
	 */
	private static String bytesToHex(byte[] bytes) {
	    var hexChars = new byte[bytes.length * 2];
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

	@Override
	public boolean equals(Object other) {
		return other instanceof TransactionReferenceImpl ltr && ltr.getHash().equals(hash);
	}

	@Override
	public int hashCode() {
		return hash.hashCode();
	}

	@Override
	public String toString() {
		return hash;
	}

	@Override
	public int compareTo(TransactionReference other) {
		return hash.compareTo(other.getHash());
	}

	@Override
	public String getHash() {
		return hash;
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeObject(TransactionReference.class, this);
	}

	@Override
	public byte[] getHashAsBytes() {
		var val = new byte[hash.length() / 2];
		for (int i = 0; i < val.length; i++) {
			int index = i * 2;
			int j = Integer.parseInt(hash.substring(index, index + 2), 16);
			val[i] = (byte) j;
		}

		return val;
	}
}