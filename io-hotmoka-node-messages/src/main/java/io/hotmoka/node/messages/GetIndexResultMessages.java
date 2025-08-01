/*
Copyright 2025 Fausto Spoto

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

import java.util.stream.Stream;

import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.messages.api.GetIndexResultMessage;
import io.hotmoka.node.messages.internal.GetIndexResultMessageImpl;
import io.hotmoka.node.messages.internal.json.GetIndexResultMessageDecoder;
import io.hotmoka.node.messages.internal.json.GetIndexResultMessageEncoder;
import io.hotmoka.node.messages.internal.json.GetIndexResultMessageJson;

/**
 * A provider of {@link GetIndexResultMessage}.
 */
public abstract class GetIndexResultMessages {

	private GetIndexResultMessages() {}

	/**
	 * Yields a {@link GetIndexResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetIndexResultMessage of(Stream<TransactionReference> result, String id) {
		return new GetIndexResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetIndexResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetIndexResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetIndexResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetIndexResultMessage message) {
    		super(message);
    	}
    }
}