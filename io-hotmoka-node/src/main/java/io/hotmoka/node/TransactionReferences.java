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

package io.hotmoka.node;

import java.io.IOException;
import java.util.function.Function;

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.internal.gson.TransactionReferenceDecoder;
import io.hotmoka.node.internal.gson.TransactionReferenceEncoder;
import io.hotmoka.node.internal.gson.TransactionReferenceJson;
import io.hotmoka.node.internal.references.TransactionReferenceImpl;

/**
 * Providers of transaction references.
 */
public abstract class TransactionReferences {

	private TransactionReferences() {}

	/**
	 * Yields a transaction reference with the given hash.
	 * 
	 * @param <E> the type of the exception thrown if {@code hash} is an illegal transaction hash
	 * @param hash the hash of the transaction, as the hexadecimal representation of its {@link TransactionReference#REQUEST_HASH_LENGTH} bytes
	 * @return the transaction reference
	 * @throws E if {@code hash} in not a legal transaction hash
	 */
	public static <E extends Exception> TransactionReference of(String hash, Function<String, ? extends E> onIllegalHash) throws E {
		return new TransactionReferenceImpl(hash, onIllegalHash);
	}

	/**
	 * Yields a transaction reference with the given hash.
	 * 
	 * @param <E> the type of the exception thrown if {@code hash} is an illegal transaction hash
	 * @param hash the hash of the transaction, as a byte array of length {@link TransactionReference#REQUEST_HASH_LENGTH}
	 * @return the transaction reference
	 * @throws E if {@code hash} in not a legal transaction hash
	 */
	public static <E extends Exception> TransactionReference of(byte[] hash, Function<String, ? extends E> onIllegalHash) throws E {
		return new TransactionReferenceImpl(hash, onIllegalHash);
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

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends TransactionReferenceEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends TransactionReferenceDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends TransactionReferenceJson {

    	/**
    	 * Creates the Json representation for the given transaction reference.
    	 * 
    	 * @param reference the transaction reference
    	 */
    	public Json(TransactionReference reference) {
    		super(reference);
    	}
    }
}