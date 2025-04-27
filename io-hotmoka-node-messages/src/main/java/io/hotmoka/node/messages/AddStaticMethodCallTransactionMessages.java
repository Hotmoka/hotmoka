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
import io.hotmoka.node.messages.api.AddStaticMethodCallTransactionMessage;
import io.hotmoka.node.messages.internal.AddStaticMethodCallTransactionMessageImpl;
import io.hotmoka.node.messages.internal.json.AddStaticMethodCallTransactionMessageDecoder;
import io.hotmoka.node.messages.internal.json.AddStaticMethodCallTransactionMessageEncoder;
import io.hotmoka.node.messages.internal.json.AddStaticMethodCallTransactionMessageJson;

/**
 * A provider of {@link AddStaticMethodCallTransactionMessage}.
 */
public abstract class AddStaticMethodCallTransactionMessages {

	private AddStaticMethodCallTransactionMessages() {}

	/**
	 * Yields a {@link AddStaticMethodCallTransactionMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param request the request of the transaction required to add
	 * @return the message
	 */
	public static AddStaticMethodCallTransactionMessage of(StaticMethodCallTransactionRequest request, String id) {
		return new AddStaticMethodCallTransactionMessageImpl(request, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends AddStaticMethodCallTransactionMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends AddStaticMethodCallTransactionMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends AddStaticMethodCallTransactionMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(AddStaticMethodCallTransactionMessage message) {
    		super(message);
    	}
    }
}