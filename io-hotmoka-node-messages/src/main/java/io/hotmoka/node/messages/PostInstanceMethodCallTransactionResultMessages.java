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
import io.hotmoka.node.messages.api.PostInstanceMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.internal.PostInstanceMethodCallTransactionResultMessageImpl;
import io.hotmoka.node.messages.internal.json.PostInstanceMethodCallTransactionResultMessageDecoder;
import io.hotmoka.node.messages.internal.json.PostInstanceMethodCallTransactionResultMessageEncoder;
import io.hotmoka.node.messages.internal.json.PostInstanceMethodCallTransactionResultMessageJson;

/**
 * A provider of {@link PostInstanceMethodCallTransactionResultMessage}.
 */
public abstract class PostInstanceMethodCallTransactionResultMessages {

	private PostInstanceMethodCallTransactionResultMessages() {}

	/**
	 * Yields a {@link PostInstanceMethodCallTransactionResultMessage}.
	 * 
	 * @param result the result of the call; this is the reference of the transaction that has been posted
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static PostInstanceMethodCallTransactionResultMessage of(TransactionReference result, String id) {
		return new PostInstanceMethodCallTransactionResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends PostInstanceMethodCallTransactionResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends PostInstanceMethodCallTransactionResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends PostInstanceMethodCallTransactionResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(PostInstanceMethodCallTransactionResultMessage message) {
    		super(message);
    	}
    }
}