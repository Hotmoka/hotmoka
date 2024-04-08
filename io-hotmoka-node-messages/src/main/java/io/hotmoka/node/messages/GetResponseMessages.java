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
import io.hotmoka.node.messages.api.GetResponseMessage;
import io.hotmoka.node.messages.internal.GetResponseMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetResponseMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetResponseMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetResponseMessageJson;

/**
 * A provider of {@link GetResponseMessage}.
 */
public final class GetResponseMessages {

	private GetResponseMessages() {}

	/**
	 * Yields a {@link GetResponseMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param reference the reference to the transaction whose response is required
	 * @return the message
	 */
	public static GetResponseMessage of(TransactionReference reference, String id) {
		return new GetResponseMessageImpl(reference, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetResponseMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetResponseMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetResponseMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetResponseMessage message) {
    		super(message);
    	}
    }
}