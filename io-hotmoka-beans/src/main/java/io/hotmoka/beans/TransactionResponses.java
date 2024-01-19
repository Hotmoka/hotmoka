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
import java.util.stream.Stream;

import io.hotmoka.beans.api.responses.JarStoreInitialTransactionResponse;
import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.internal.gson.TransactionReferenceDecoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceEncoder;
import io.hotmoka.beans.internal.gson.TransactionReferenceJson;
import io.hotmoka.beans.internal.responses.JarStoreInitialTransactionResponseImpl;
import io.hotmoka.beans.internal.responses.TransactionResponseImpl;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of transaction responses.
 */
public abstract class TransactionResponses {

	private TransactionResponses() {}

	/**
	 * Yields the response of a transaction that installs a jar in a yet non-initialized node.
	 * 
	 * @param instrumentedJar the bytes of the jar to install, instrumented
	 * @param dependencies the dependencies of the jar, previously installed in blockchain
	 * @param verificationToolVersion the version of the verification tool
	 * @return the response
	 */
	public static JarStoreInitialTransactionResponse jarStoreInitial(byte[] instrumentedJar, Stream<TransactionReference> dependencies, long verificationToolVersion) {
		return new JarStoreInitialTransactionResponseImpl(instrumentedJar, dependencies, verificationToolVersion);
	}

	/**
	 * Yields a transaction responses unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the transaction response
	 * @throws IOException if the response could not be unmarshalled
     */
	public static TransactionResponse from(UnmarshallingContext context) throws IOException {
		return TransactionResponseImpl.from(context);
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