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
import io.hotmoka.node.messages.api.GetManifestResultMessage;
import io.hotmoka.node.messages.internal.GetManifestResultMessageImpl;
import io.hotmoka.node.messages.internal.gson.GetManifestResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.GetManifestResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.GetManifestResultMessageJson;

/**
 * A provider of {@link GetManifestResultMessage}.
 */
public final class GetManifestResultMessages {

	private GetManifestResultMessages() {}

	/**
	 * Yields a {@link GetManifestResultMessage}.
	 * 
	 * @param result the result of the call
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static GetManifestResultMessage of(StorageReference result, String id) {
		return new GetManifestResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends GetManifestResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends GetManifestResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends GetManifestResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(GetManifestResultMessage message) {
    		super(message);
    	}
    }
}