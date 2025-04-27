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

import io.hotmoka.node.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.messages.api.PostStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.internal.PostStaticMethodCallTransactionMessageImpl;
import io.hotmoka.node.messages.internal.json.PostStaticMethodCallTransactionMessageDecoder;
import io.hotmoka.node.messages.internal.json.PostStaticMethodCallTransactionMessageEncoder;
import io.hotmoka.node.messages.internal.json.PostStaticMethodCallTransactionMessageJson;

/**
 * A provider of {@link PostStaticMethodCallTransactionMessage}.
 */
public abstract class PostStaticMethodCallTransactionMessages {

	private PostStaticMethodCallTransactionMessages() {}

	/**
	 * Yields a {@link PostStaticMethodCallTransactionMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param request the request of the transaction required to post
	 * @return the message
	 */
	public static PostStaticMethodCallTransactionMessage of(StaticMethodCallTransactionRequest request, String id) {
		return new PostStaticMethodCallTransactionMessageImpl(request, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends PostStaticMethodCallTransactionMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends PostStaticMethodCallTransactionMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends PostStaticMethodCallTransactionMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(PostStaticMethodCallTransactionMessage message) {
    		super(message);
    	}
    }
}