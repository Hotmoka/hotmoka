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

import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.messages.api.PostInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.internal.PostInstanceMethodCallTransactionMessageImpl;
import io.hotmoka.node.messages.internal.gson.PostInstanceMethodCallTransactionMessageDecoder;
import io.hotmoka.node.messages.internal.gson.PostInstanceMethodCallTransactionMessageEncoder;
import io.hotmoka.node.messages.internal.gson.PostInstanceMethodCallTransactionMessageJson;

/**
 * A provider of {@link PostInstanceMethodCallTransactionMessage}.
 */
public abstract class PostInstanceMethodCallTransactionMessages {

	private PostInstanceMethodCallTransactionMessages() {}

	/**
	 * Yields a {@link PostInstanceMethodCallTransactionMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param request the request of the transaction required to post
	 * @return the message
	 */
	public static PostInstanceMethodCallTransactionMessage of(InstanceMethodCallTransactionRequest request, String id) {
		return new PostInstanceMethodCallTransactionMessageImpl(request, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends PostInstanceMethodCallTransactionMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends PostInstanceMethodCallTransactionMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends PostInstanceMethodCallTransactionMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(PostInstanceMethodCallTransactionMessage message) {
    		super(message);
    	}
    }
}