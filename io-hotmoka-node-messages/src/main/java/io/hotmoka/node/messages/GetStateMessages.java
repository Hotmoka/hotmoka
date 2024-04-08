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

import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.messages.api.GetStateMessage;
import io.hotmoka.node.messages.internal.GetStateMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetStateMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetStateMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetStateMessageJson;

/**
 * A provider of {@link GetStateMessage}.
 */
public final class GetStateMessages {

	private GetStateMessages() {}

	/**
	 * Yields a {@link GetStateMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param reference the reference to the object whose state is required
	 * @return the message
	 */
	public static GetStateMessage of(StorageReference reference, String id) {
		return new GetStateMessageImpl(reference, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetStateMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetStateMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetStateMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetStateMessage message) {
    		super(message);
    	}
    }
}