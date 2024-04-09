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

package io.hotmoka.node.messages;

import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.node.messages.api.AddJarStoreTransactionResultMessage;
import io.hotmoka.node.messages.internal.AddJarStoreTransactionResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.AddJarStoreTransactionResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.AddJarStoreTransactionResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.AddJarStoreTransactionResultMessageJson;

/**
 * A provider of {@link AddJarStoreTransactionResultMessage}.
 */
public final class AddJarStoreTransactionResultMessages {

	private AddJarStoreTransactionResultMessages() {}

	/**
	 * Yields a {@link AddJarStoreTransactionResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static AddJarStoreTransactionResultMessage of(TransactionReference result, String id) {
		return new AddJarStoreTransactionResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends AddJarStoreTransactionResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends AddJarStoreTransactionResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends AddJarStoreTransactionResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(AddJarStoreTransactionResultMessage message) {
    		super(message);
    	}
    }
}