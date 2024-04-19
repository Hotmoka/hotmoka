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

import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.messages.api.GetRequestResultMessage;
import io.hotmoka.node.messages.internal.GetRequestResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetRequestResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetRequestResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetRequestResultMessageJson;

/**
 * A provider of {@link GetRequestResultMessage}.
 */
public final class GetRequestResultMessages {

	private GetRequestResultMessages() {}

	/**
	 * Yields a {@link GetRequestResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetRequestResultMessage of(TransactionRequest<?> result, String id) {
		return new GetRequestResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetRequestResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetRequestResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetRequestResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetRequestResultMessage message) {
    		super(message);
    	}
    }
}