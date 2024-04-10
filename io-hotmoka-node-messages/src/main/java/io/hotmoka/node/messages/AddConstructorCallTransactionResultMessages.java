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

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.messages.api.AddConstructorCallTransactionResultMessage;
import io.hotmoka.node.messages.internal.AddConstructorCallTransactionResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.AddConstructorCallTransactionResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.AddConstructorCallTransactionResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.AddConstructorCallTransactionResultMessageJson;

/**
 * A provider of {@link AddConstructorCallTransactionResultMessage}.
 */
public final class AddConstructorCallTransactionResultMessages {

	private AddConstructorCallTransactionResultMessages() {}

	/**
	 * Yields an {@link AddConstructorCallTransactionResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static AddConstructorCallTransactionResultMessage of(StorageReference result, String id) {
		return new AddConstructorCallTransactionResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends AddConstructorCallTransactionResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends AddConstructorCallTransactionResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends AddConstructorCallTransactionResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(AddConstructorCallTransactionResultMessage message) {
    		super(message);
    	}
    }
}