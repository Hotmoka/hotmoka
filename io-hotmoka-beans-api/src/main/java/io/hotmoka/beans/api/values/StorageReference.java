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

package io.hotmoka.beans.api.values;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.marshalling.api.MarshallingContext;

/**
 * A reference to an object of class type that can be stored in the blockchain.
 * It knows the transaction that created the object. Objects created during the
 * same transaction are disambiguated by a progressive number.
 */
@Immutable
public interface StorageReference extends StorageValue, Serializable {

	/**
	 * Yields the transaction that created the object.
	 * 
	 * @return the transaction that created the object
	 */
	TransactionReference getTransaction();

	/**
	 * Yields the progressive number of the object among those that have been created
	 * during the same transaction.
	 * 
	 * @return the progressive number of the object among those that have been created
	 * during the same transaction
	 */
	BigInteger getProgressive();

	/**
	 * Transforms this object into a byte array, without the selector that, instead,
	 * gets added by {@code StorageReference#toByteArray()}.
	 * 
	 * @return the byte array resulting from marshalling this object
	 */
	byte[] toByteArrayWithoutSelector();

	/**
	 * Marshals this object into the given context, without the selector that, instead,
	 * gets added by {@link StorageReference#into(MarshallingContext)}.
	 * 
	 * @param context the context
	 * @throws IOException if the object cannot be marshalled
	 */
	void intoWithoutSelector(MarshallingContext context) throws IOException;
}