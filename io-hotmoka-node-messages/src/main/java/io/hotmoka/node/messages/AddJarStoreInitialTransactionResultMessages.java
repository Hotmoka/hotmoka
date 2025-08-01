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

import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.messages.api.AddJarStoreInitialTransactionResultMessage;
import io.hotmoka.node.messages.internal.AddJarStoreInitialTransactionResultMessageImpl;
import io.hotmoka.node.messages.internal.json.AddJarStoreInitialTransactionResultMessageJson;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;

/**
 * A provider of {@link AddJarStoreInitialTransactionResultMessage}.
 */
public abstract class AddJarStoreInitialTransactionResultMessages {

	private AddJarStoreInitialTransactionResultMessages() {}

	/**
	 * Yields a {@link AddJarStoreInitialTransactionResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static AddJarStoreInitialTransactionResultMessage of(TransactionReference result, String id) {
		return new AddJarStoreInitialTransactionResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends MappedEncoder<AddJarStoreInitialTransactionResultMessage, Json> {

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
	public static class Decoder extends MappedDecoder<AddJarStoreInitialTransactionResultMessage, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}

	/**
     * Json representation.
     */
    public static class Json extends AddJarStoreInitialTransactionResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(AddJarStoreInitialTransactionResultMessage message) {
    		super(message);
    	}
    }
}