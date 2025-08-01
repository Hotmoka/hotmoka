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

import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.GetIndexMessage;
import io.hotmoka.node.messages.internal.GetIndexMessageImpl;
import io.hotmoka.node.messages.internal.json.GetIndexMessageDecoder;
import io.hotmoka.node.messages.internal.json.GetIndexMessageEncoder;
import io.hotmoka.node.messages.internal.json.GetIndexMessageJson;

/**
 * A provider of {@link GetIndexMessage}.
 */
public abstract class GetIndexMessages {

	private GetIndexMessages() {}

	/**
	 * Yields a {@link GetIndexMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param reference the reference to the object whose index is required
	 * @return the message
	 */
	public static GetIndexMessage of(StorageReference reference, String id) {
		return new GetIndexMessageImpl(reference, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetIndexMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetIndexMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetIndexMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetIndexMessage message) {
    		super(message);
    	}
    }
}