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

import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.messages.api.GetClassTagResultMessage;
import io.hotmoka.node.messages.internal.GetClassTagResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetClassTagResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetClassTagResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetClassTagResultMessageJson;

/**
 * A provider of {@link GetClassTagResultMessage}.
 */
public abstract class GetClassTagResultMessages {

	private GetClassTagResultMessages() {}

	/**
	 * Yields a {@link GetClassTagResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetClassTagResultMessage of(ClassTag result, String id) {
		return new GetClassTagResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetClassTagResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetClassTagResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetClassTagResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetClassTagResultMessage message) {
    		super(message);
    	}
    }
}