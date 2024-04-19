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
import io.hotmoka.node.messages.api.GetTakamakaCodeResultMessage;
import io.hotmoka.node.messages.internal.GetTakamakaCodeResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetTakamakaCodeResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetTakamakaCodeResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetTakamakaCodeResultMessageJson;

/**
 * A provider of {@link GetTakamakaCodeResultMessage}.
 */
public final class GetTakamakaCodeResultMessages {

	private GetTakamakaCodeResultMessages() {}

	/**
	 * Yields a {@link GetTakamakaCodeResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetTakamakaCodeResultMessage of(TransactionReference result, String id) {
		return new GetTakamakaCodeResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetTakamakaCodeResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetTakamakaCodeResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetTakamakaCodeResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetTakamakaCodeResultMessage message) {
    		super(message);
    	}
    }
}