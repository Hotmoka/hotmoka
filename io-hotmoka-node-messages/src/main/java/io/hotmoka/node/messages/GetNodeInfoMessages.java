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

import io.hotmoka.node.messages.api.GetNodeInfoMessage;
import io.hotmoka.node.messages.internal.GetNodeInfoMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetNodeInfoMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetNodeInfoMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetNodeInfoMessageJson;

/**
 * A provider of {@link GetNodeInfoMessage}.
 */
public final class GetNodeInfoMessages {

	private GetNodeInfoMessages() {}

	/**
	 * Yields a {@link GetNodeInfoMessage}.
	 * 
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetNodeInfoMessage of(String id) {
		return new GetNodeInfoMessageImpl(id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetNodeInfoMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetNodeInfoMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetNodeInfoMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetNodeInfoMessage message) {
    		super(message);
    	}
    }
}