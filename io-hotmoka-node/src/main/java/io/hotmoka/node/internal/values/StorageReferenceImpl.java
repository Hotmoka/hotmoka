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

package io.hotmoka.node.internal.values;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.function.Function;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.NodeMarshallingContexts;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.internal.json.StorageValueJson;
import io.hotmoka.node.internal.references.TransactionReferenceImpl;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * A reference to an object of class type that can be stored in a Hotmoka node.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public final class StorageReferenceImpl extends AbstractStorageValue implements StorageReference, Serializable {
	private static final long serialVersionUID = 1899009680134694798L;

	static final byte SELECTOR = 11;

	/**
	 * The transaction that created the object.
	 */
	private final TransactionReference transaction;

	/**
	 * The progressive number of the object among those that have been created
	 * during the same transaction.
	 */
	private final BigInteger progressive;

	/**
	 * Builds a storage reference from its transaction reference and progressive.
	 * 
	 * @param <E> the type of the exception to throw if the result would be an illegal storage reference
	 * @param transaction the transaction that created the object
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 * @param onIllegalReference the creator of the exception thrown if the result would be an illegal storage reference
	 * @throws E if the result would be an illegal storage reference
	 */
	public StorageReferenceImpl(TransactionReference transaction, BigInteger progressive) {
		this(transaction, progressive, IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a storage reference from the given stream.
	 * The selector of the value has been already processed.
	 * 
	 * @param context the unmarshalling context
	 * @throws IOException if the response could not be unmarshalled
	 */
	public StorageReferenceImpl(UnmarshallingContext context) throws IOException {
		this(TransactionReferences.from(context), context.readBigInteger(), IOException::new);
	}

	/**
	 * Creates a storage reference from the given JSON representation.
	 * 
	 * @param json the JSON representation
	 * @throws InconsistentJsonException if {@code json} is inconsistent
	 */
	public StorageReferenceImpl(StorageValueJson json) throws InconsistentJsonException {
		this(
			Objects.requireNonNull(json.getTransaction(), "transaction cannot be null in a storage reference", InconsistentJsonException::new).unmap(),
			json.getProgressive(),
			InconsistentJsonException::new
		);
	}

	/**
	 * Builds a storage reference from its transaction reference and progressive.
	 * 
	 * @param <E> the type of the exception to throw if the result would be an illegal storage reference
	 * @param transaction the transaction that created the object
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 * @param onIllegalReference the creator of the exception thrown if the result would be an illegal storage reference
	 * @throws E if the result would be an illegal storage reference
	 */
	private <E extends Exception> StorageReferenceImpl(TransactionReference transaction, BigInteger progressive, ExceptionSupplier<? extends E> onIllegalReference) throws E {
		this.transaction = Objects.requireNonNull(transaction, "transaction cannot be null", onIllegalReference);
		this.progressive = Objects.requireNonNull(progressive, "progressive cannot be null", onIllegalReference);
		if (progressive.signum() < 0)
			throw onIllegalReference.apply("progressive cannot be negative");
	}

	/**
	 * Builds a storage reference from its string representation.
	 * 
	 * @param s the string representation
	 */
	public <E extends Exception> StorageReferenceImpl(String s) {
		String[] splits = Objects.requireNonNull(s, "s cannot be null", IllegalArgumentException::new).split("#");
		if (splits.length != 2)
			throw new IllegalArgumentException("A storage reference should have the form transaction#progressive");

		this.transaction = new TransactionReferenceImpl(splits[0]);

		try {
			this.progressive = new BigInteger(splits[1], 16);
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("The progressive part of a storage reference should be a positive hexadecimal number", e);
		}

		if (progressive.signum() < 0)
			throw new IllegalArgumentException("The progressive part of a storage reference should be a positive hexadecimal number");
	}

	@Override
	public TransactionReference getTransaction() {
		return transaction;
	}

	@Override
	public BigInteger getProgressive() {
		return progressive;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference sr && sr.getProgressive().equals(progressive) && sr.getTransaction().equals(transaction);
	}

	@Override
	public int hashCode() {
		return progressive.hashCode() ^ transaction.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		if (other instanceof StorageReference sr) {
			int diff = transaction.compareTo(sr.getTransaction());
			if (diff != 0)
				return diff;
			else
				return progressive.compareTo(sr.getProgressive());
		}
		else
			return super.compareTo(other);
	}

	@Override
	public String toString() {
		return transaction + "#" + progressive.toString(16);
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		intoWithoutSelector(context);
	}

	@Override
	public final byte[] toByteArrayWithoutSelector() {
		try (var baos = new ByteArrayOutputStream(); var context = NodeMarshallingContexts.of(baos)) {
			intoWithoutSelector(context);
			context.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			// impossible for a ByteArrayOutputStream
			throw new RuntimeException(e);
		}
	}

	@Override
	public <E extends Exception> StorageReference asReference(Function<StorageValue, ? extends E> exception) {
		return this;
	}

	@Override
	public <E extends Exception> StorageReference asReturnedReference(NonVoidMethodSignature method, Function<String, ? extends E> exception) throws E {
		return this;
	}

	@Override
	public final void intoWithoutSelector(MarshallingContext context) throws IOException {
		context.writeObject(StorageReference.class, this);
	}

	/**
	 * Factory method that unmarshals a storage reference from the given stream.
	 * It assumes that there is no selector at the beginning of the marshalled data.
	 * 
	 * @param context the unmarshalling context
	 * @return the storage reference
	 * @throws IOException 
	 */
	public static StorageReference fromWithoutSelector(UnmarshallingContext context) throws IOException {
		return context.readObject(StorageReference.class);
	}
}