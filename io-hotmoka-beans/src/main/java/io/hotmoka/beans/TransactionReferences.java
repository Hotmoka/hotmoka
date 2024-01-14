/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.beans;

import java.io.IOException;

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.internal.references.TransactionReferenceImpl;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of transaction references.
 */
public abstract class TransactionReferences {

	private TransactionReferences() {}

	/**
	 * Yields a transaction reference with the given hash.
	 * 
	 * @param hash the hash of the transaction, as the hexadecimal representation of its bytes
	 * @return the transaction reference
	 */
	public static TransactionReference of(String hash) {
		return new TransactionReferenceImpl(hash);
	}

	/**
	 * Yields a transaction reference with the given hash.
	 * 
	 * @param hash the hash of the transaction, as a byte array
	 * @return the transaction reference
	 */
	public static TransactionReference of(byte[] hash) {
		return new TransactionReferenceImpl(hash);
	}

	/**
	 * Yields a transaction reference unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction reference
	 * @throws IOException if the reference could not be unmarshalled
     */
	public static TransactionReference from(UnmarshallingContext context) throws IOException {
		return TransactionReferenceImpl.from(context);
	}
}