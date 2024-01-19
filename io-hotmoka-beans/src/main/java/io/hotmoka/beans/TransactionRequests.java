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

import io.hotmoka.beans.api.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.api.requests.TransactionRequest;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.internal.gson.TransactionReferenceDecoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceEncoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceJson;
import io.hotmoka.beans.internal.requests.JarStoreInitialTransactionRequestImpl;
import io.hotmoka.beans.internal.requests.TransactionRequestImpl;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of transaction requests.
 */
public abstract class TransactionRequests {

	private TransactionRequests() {}

	/**
	 * Yields a transaction request to install a jar in a yet non-initialized node.
	 * 
	 * @param jar the bytes of the jar to install
	 * @param dependencies the dependencies of the jar, already installed in blockchain
	 * @return the request
	 */
	public static JarStoreInitialTransactionRequest jarStoreInitial(byte[] jar, TransactionReference... dependencies) {
		return new JarStoreInitialTransactionRequestImpl(jar, dependencies);
	}

	/**
	 * Yields a transaction request unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction request
	 * @throws IOException if the request could not be unmarshalled
     */
	public static TransactionRequest<?> from(UnmarshallingContext context) throws IOException {
		return TransactionRequestImpl.from(context);
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