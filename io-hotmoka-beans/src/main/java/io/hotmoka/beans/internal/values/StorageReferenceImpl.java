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

package io.hotmoka.beans.internal.values;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.internal.marshalling.BeanMarshallingContext;
import io.hotmoka.marshalling.api.MarshallingContext;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * A reference to an object of class type that can be stored in the blockchain.
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
	 * @param transaction the transaction that created the object
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 */
	public StorageReferenceImpl(TransactionReference transaction, BigInteger progressive) {
		this.transaction = Objects.requireNonNull(transaction, "transaction cannot be null");
		this.progressive = Objects.requireNonNull(progressive, "progressive cannot be null");
		if (progressive.signum() < 0)
			throw new IllegalArgumentException("progressive cannot be negative");
	}

	/**
	 * Builds a storage reference from its string representation.
	 * 
	 * @param s the string representation
	 */
	public StorageReferenceImpl(String s) {
		this(TransactionReferences.of(s.split("#")[0]), new BigInteger(s.split("#")[1], 16));
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
		return other instanceof StorageReference sr && sr.getTransaction().equals(transaction) && sr.getProgressive().equals(progressive);
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
		try (var baos = new ByteArrayOutputStream(); var context = new BeanMarshallingContext(baos)) {
			intoWithoutSelector(context);
			context.flush();
			return baos.toByteArray();
		}
		catch (IOException e) {
			// impossible for a ByteArrayOutputStream
			throw new RuntimeException("Unexpected exception", e);
		}
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