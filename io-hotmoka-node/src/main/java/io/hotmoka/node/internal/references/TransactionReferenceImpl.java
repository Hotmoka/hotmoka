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
import io.hotmoka.exceptions.ExceptionSupplierFromMessage;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.AbstractMarshallable;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.internal.json.TransactionReferenceJson;
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
	 * Builds a transaction reference with the given hash.
	 * 
	 * @param hash the hash of the transaction, as a byte array of length {@link TransactionReference#REQUEST_HASH_LENGTH}
	 */
	public TransactionReferenceImpl(byte[] hash) {
		this(checkHash(hash, IllegalArgumentException::new).clone(), IllegalArgumentException::new);
	}

	/**
	 * Builds a transaction reference.
	 * 
	 * @param hash the hash of the transaction, as the hexadecimal representation of its {@link TransactionReference#REQUEST_HASH_LENGTH} bytes
	 */
	public TransactionReferenceImpl(String hash) {
		this(hash, IllegalArgumentException::new);
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
	 * Unmarshals a transaction reference from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the unmarshalling failed
	 */
	public TransactionReferenceImpl(UnmarshallingContext context) throws IOException {
		this(
			context.readBytes(TransactionReference.REQUEST_HASH_LENGTH, "Cannot read a transaction reference"),
			IOException::new
		);
	}

	/**
	 * Builds a transaction reference.
	 * 
	 * @param <E> the type of the exception thrown if {@code hash} is an illegal transaction hash
	 * @param hash the hash of the transaction, as the hexadecimal representation of its {@link TransactionReference#REQUEST_HASH_LENGTH} bytes
	 * @param onIllegalHash the generator of the exception thrown if {@code hash} is illegal
	 * @throws E if {@code hash} in not a legal transaction hash
	 */
	private <E extends Exception> TransactionReferenceImpl(String hash, ExceptionSupplierFromMessage<? extends E> onIllegalHash) throws E {
		// each byte is represented by two successive characters
		if (Objects.requireNonNull(hash, "hash cannot be null", onIllegalHash).length() != REQUEST_HASH_LENGTH * 2)
			throw onIllegalHash.apply("Illegal transaction reference: it should be " + REQUEST_HASH_LENGTH + " bytes long");
	
		this.hash = Hex.fromHexString(hash, onIllegalHash);
	}

	/**
	 * Builds a transaction reference with the given hash.
	 * 
	 * @param <E> the type of the exception thrown if {@code hash} is an illegal transaction hash
	 * @param hash the hash of the transaction, as a byte array of length {@link TransactionReference#REQUEST_HASH_LENGTH}
	 * @param onIllegalHash the generator of the exception thrown if {@code hash} is illegal
	 * @throws E if {@code hash} in not a legal transaction hash
	 */
	private <E extends Exception> TransactionReferenceImpl(byte[] hash, ExceptionSupplierFromMessage<? extends E> onIllegalHash) throws E {
		this.hash = hash;
	}

	private static <E extends Exception> byte[] checkHash(byte[] hash, ExceptionSupplierFromMessage<? extends E> onIllegalHash) throws E {
		if (Objects.requireNonNull(hash, "hash cannot be null", onIllegalHash).length != REQUEST_HASH_LENGTH)
			throw onIllegalHash.apply("Illegal transaction reference: it should be " + REQUEST_HASH_LENGTH + " bytes long");

		return hash;
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
	protected final MarshallingContext createMarshallingContext(OutputStream os) {
		return NodeMarshallingContexts.of(os);
	}
}