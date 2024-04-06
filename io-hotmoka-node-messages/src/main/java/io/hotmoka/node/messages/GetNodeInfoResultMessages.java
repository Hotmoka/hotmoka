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

import io.hotmoka.beans.api.nodes.NodeInfo;
import io.hotmoka.node.messages.api.GetNodeInfoResultMessage;
import io.hotmoka.node.messages.internal.GetNodeInfoResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetNodeInfoResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetNodeInfoResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetNodeInfoResultMessageJson;

/**
 * A provider of {@link GetNodeInfoResultMessage}.
 */
public final class GetNodeInfoResultMessages {

	private GetNodeInfoResultMessages() {}

	/**
	 * Yields a {@link GetNodeInfoResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetNodeInfoResultMessage of(NodeInfo result, String id) {
		return new GetNodeInfoResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetNodeInfoResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetNodeInfoResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetNodeInfoResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetNodeInfoResultMessage message) {
    		super(message);
    	}
    }
}