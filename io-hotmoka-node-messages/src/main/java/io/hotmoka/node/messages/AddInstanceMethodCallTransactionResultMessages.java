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

import java.util.Optional;

import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.messages.api.AddInstanceMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.internal.AddInstanceMethodCallTransactionResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.AddInstanceMethodCallTransactionResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.AddInstanceMethodCallTransactionResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.AddInstanceMethodCallTransactionResultMessageJson;

/**
 * A provider of {@link AddInstanceMethodCallTransactionResultMessage}.
 */
public abstract class AddInstanceMethodCallTransactionResultMessages {

	private AddInstanceMethodCallTransactionResultMessages() {}

	/**
	 * Yields a {@link AddInstanceMethodCallTransactionResultMessage}.
	 * 
	 * @param result the result of the call; this might be empty for void methods
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static AddInstanceMethodCallTransactionResultMessage of(Optional<StorageValue> result, String id) {
		return new AddInstanceMethodCallTransactionResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends AddInstanceMethodCallTransactionResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends AddInstanceMethodCallTransactionResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends AddInstanceMethodCallTransactionResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(AddInstanceMethodCallTransactionResultMessage message) {
    		super(message);
    	}
    }
}