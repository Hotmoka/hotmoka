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

import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.messages.api.GetClassTagMessage;
import io.hotmoka.node.messages.internal.GetClassTagMessageImpl;
import io.hotmoka.node.messages.internal.json.GetClassTagMessageDecoder;
import io.hotmoka.node.messages.internal.json.GetClassTagMessageEncoder;
import io.hotmoka.node.messages.internal.json.GetClassTagMessageJson;

/**
 * A provider of {@link GetClassTagMessage}.
 */
public abstract class GetClassTagMessages {

	private GetClassTagMessages() {}

	/**
	 * Yields a {@link GetClassTagMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param reference the reference to the object whose class tag is required
	 * @return the message
	 */
	public static GetClassTagMessage of(StorageReference reference, String id) {
		return new GetClassTagMessageImpl(reference, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetClassTagMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetClassTagMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetClassTagMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetClassTagMessage message) {
    		super(message);
    	}
    }
}