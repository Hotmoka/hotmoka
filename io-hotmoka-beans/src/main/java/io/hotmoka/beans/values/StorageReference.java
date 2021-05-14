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

package io.hotmoka.beans.values;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import io.hotmoka.beans.GasCostModel;
import io.hotmoka.beans.MarshallingContext;
import io.hotmoka.beans.UnmarshallingContext;
import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.beans.references.LocalTransactionReference;
import io.hotmoka.beans.references.TransactionReference;

/**
 * A reference to an object of class type that can be stored in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public final class StorageReference extends StorageValue {
	static final byte SELECTOR = 11;

	/**
	 * The transaction that created the object.
	 */
	public final TransactionReference transaction;

	/**
	 * The progressive number of the object among those that have been created
	 * during the same transaction.
	 */
	public final BigInteger progressive;

	/**
	 * Builds a storage reference.
	 * 
	 * @param transaction the transaction that created the object
	 * @param progressive the progressive number of the object among those that have been created
	 *                    during the same transaction
	 */
	public StorageReference(TransactionReference transaction, BigInteger progressive) {
		if (transaction == null)
			throw new IllegalArgumentException("transaction cannot be null");

		if (progressive == null)
			throw new IllegalArgumentException("progressive cannot be null");

		this.progressive = progressive;
		this.transaction = transaction;

	}

	public StorageReference(String s) {
		this(new LocalTransactionReference(s.split("#")[0]), new BigInteger(s.split("#")[1], 16));
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StorageReference &&
			((StorageReference) other).transaction.equals(transaction) &&
			((StorageReference) other).progressive.equals(progressive);
	}

	@Override
	public int hashCode() {
		return progressive.hashCode() ^ transaction.hashCode();
	}

	@Override
	public int compareTo(StorageValue other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;

		diff = transaction.compareTo(((StorageReference) other).transaction);
		if (diff != 0)
			return diff;

		return progressive.compareTo(((StorageReference) other).progressive);
	}

	@Override
	public String toString() {
		return transaction + "#" + progressive.toString(16);
	}

	@Override
	public BigInteger size(GasCostModel gasCostModel) {
		return super.size(gasCostModel).add(gasCostModel.storageCostOf(progressive)).add(gasCostModel.storageCostOf(transaction));
	}

	@Override
	public final void into(MarshallingContext context) throws IOException {
		context.writeByte(SELECTOR);
		intoWithoutSelector(context);
	}

	/**
	 * Marshals this object into a byte array.
	 * 
	 * @return the byte array resulting from marshalling this object
	 * @throws IOException if this object cannot be marshalled
	 */
	public final byte[] toByteArrayWithoutSelector() throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); MarshallingContext context = new MarshallingContext(baos)) {
			intoWithoutSelector(context);
			context.flush();
			return baos.toByteArray();
		}
	}

	public final void intoWithoutSelector(MarshallingContext context) throws IOException {
		context.writeStorageReference(this);
	}

	/**
	 * Factory method that unmarshals a storage reference from the given stream.
	 * 
	 * @param context the unmarshalling context
	 * @return the storage reference
	 * @throws IOException if the storage reference could not be unmarshalled
	 * @throws ClassNotFoundException if the storage reference could not be unmarshalled
	 */
	public static StorageReference from(UnmarshallingContext context) throws IOException, ClassNotFoundException {
		return context.readStorageReference();
	}
}