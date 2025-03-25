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

import io.hotmoka.node.messages.api.GetInfoMessage;
import io.hotmoka.node.messages.internal.GetInfoMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetInfoMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetInfoMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetInfoMessageJson;

/**
 * A provider of {@link GetInfoMessage}.
 */
public abstract class GetInfoMessages {

	private GetInfoMessages() {}

	/**
	 * Yields a {@link GetInfoMessage}.
	 * 
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetInfoMessage of(String id) {
		return new GetInfoMessageImpl(id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetInfoMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetInfoMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetInfoMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetInfoMessage message) {
    		super(message);
    	}
    }
}