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

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.internal.json.TransactionReferenceJson;
import io.hotmoka.node.internal.references.TransactionReferenceImpl;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;

/**
 * Providers of transaction references.
 */
public abstract class TransactionReferences {

	private TransactionReferences() {}

	/**
	 * Yields a transaction reference with the given hash.
	 * 
	 * @param hash the hash of the transaction, as the hexadecimal representation of its {@link TransactionReference#REQUEST_HASH_LENGTH} bytes
	 * @return the transaction reference
	 */
	public static TransactionReference of(String hash) {
		return new TransactionReferenceImpl(hash);
	}

	/**
	 * Yields a transaction reference with the given hash.
	 * 
	 * @param hash the hash of the transaction, as a byte array of length {@link TransactionReference#REQUEST_HASH_LENGTH}
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

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends MappedEncoder<TransactionReference, Json> {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {
			super(Json::new);
		}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends MappedDecoder<TransactionReference, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}

    /**
     * JSON representation.
     */
    public static class Json extends TransactionReferenceJson {

    	/**
    	 * Creates the JSON representation for the given transaction reference.
    	 * 
    	 * @param reference the transaction reference
    	 */
    	public Json(TransactionReference reference) {
    		super(reference);
    	}
    }
}