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

import io.hotmoka.beans.api.responses.TransactionResponse;
import io.hotmoka.node.messages.api.GetResponseMessage;
import io.hotmoka.node.messages.api.GetResponseResultMessage;
import io.hotmoka.node.messages.internal.GetResponseResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetResponseResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetResponseResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetResponseResultMessageJson;

/**
 * A provider of {@link GetResponseMessage}.
 */
public final class GetResponseResultMessages {

	private GetResponseResultMessages() {}

	/**
	 * Yields a {@link GetResponseMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetResponseResultMessage of(TransactionResponse result, String id) {
		return new GetResponseResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetResponseResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetResponseResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetResponseResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetResponseResultMessage message) {
    		super(message);
    	}
    }
}