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

package io.hotmoka.node.internal.references;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import io.hotmoka.crypto.Hex;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.internal.gson.TransactionReferenceJson;
import io.hotmoka.node.internal.marshalling.NodeMarshallingContext;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Implementation of a transaction reference: it refers to a transaction in the store of a node.
 */
public final class TransactionReferenceImpl extends AbstractMarshallable implements TransactionReference {
	private static final long serialVersionUID = 1157696417214320999L;

	/**
	 * The hash of the request that generated the transaction.
	 * This is {@link TransactionReference#REQUEST_HASH_LENGTH} bytes long.
	 */
	private final byte[] hash;

	/**
	 * Builds a transaction reference.
	 * 
	 * @param <E> the type of the exception thrown if {@code hash} is an illegal transaction hash
	 * @param hash the hash of the transaction, as the hexadecimal representation of its {@link TransactionReference#REQUEST_HASH_LENGTH} bytes
	 * @throws E if {@code hash} in not a legal transaction hash
	 */
	public <E extends Exception> TransactionReferenceImpl(String hash, ExceptionSupplier<? extends E> onIllegalHash) throws E {
		if (hash == null)
			throw onIllegalHash.apply("hash cannot be null");
		// each byte is represented by two successive characters
		else if (hash.length() != REQUEST_HASH_LENGTH * 2)
			throw onIllegalHash.apply("Illegal transaction reference: it should be " + REQUEST_HASH_LENGTH + " bytes long");

		this.hash = Hex.fromHexString(hash, onIllegalHash);
	}

	/**
	 * Builds a transaction reference with the given hash.
	 * 
	 * @param <E> the type of the exception thrown if {@code hash} is an illegal transaction hash
	 * @param hash the hash of the transaction, as a byte array of length {@link TransactionReference#REQUEST_HASH_LENGTH}
	 * @throws E if {@code hash} in not a legal transaction hash
	 */
	public <E extends Exception> TransactionReferenceImpl(byte[] hash, ExceptionSupplier<? extends E> onIllegalHash) throws E {
		if (hash == null)
			throw onIllegalHash.apply("hash cannot be null");
		else if (hash.length != REQUEST_HASH_LENGTH)
			throw onIllegalHash.apply("Illegal transaction reference: it should be " + REQUEST_HASH_LENGTH + " bytes long");

		this.hash = hash.clone();
	}

	/**
	 * Creates a transaction reference from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public TransactionReferenceImpl(TransactionReferenceJson json) throws InconsistentJsonException {
		this(json.getHash(), InconsistentJsonException::new);
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
	public boolean equals(Object other) {
		if (other instanceof TransactionReferenceImpl tri) // optimization
			return Arrays.equals(tri.hash, hash);
		else
			return other instanceof TransactionReference tr && Arrays.equals(tr.getHash(), hash);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(hash);
	}

	@Override
	public String toString() {
		return Hex.toHexString(hash);
	}

	@Override
	public int compareTo(TransactionReference other) {
		if (other instanceof TransactionReferenceImpl tri) // optimization
			return Arrays.compare(hash, tri.hash);
		else
			return Arrays.compare(hash, other.getHash());
	}

	@Override
	public void into(MarshallingContext context) throws IOException {
		context.writeObject(TransactionReference.class, this);
	}

	@Override
	public byte[] getHash() {
		return hash.clone();
	}

	@Override
	protected final MarshallingContext createMarshallingContext(OutputStream os) throws IOException {
		return new NodeMarshallingContext(os);
	}
}