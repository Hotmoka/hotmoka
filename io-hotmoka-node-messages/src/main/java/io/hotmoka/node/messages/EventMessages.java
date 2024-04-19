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
import io.hotmoka.node.messages.api.EventMessage;
import io.hotmoka.node.messages.internal.EventMessageImpl;
import io.hotmoka.node.messages.internal.gson.EventMessageDecoder;
import io.hotmoka.node.messages.internal.gson.EventMessageEncoder;
import io.hotmoka.node.messages.internal.gson.EventMessageJson;

/**
 * A provider of {@link EventMessage}.
 */
public final class EventMessages {

	private EventMessages() {}

	/**
	 * Creates an {@link EventMessage}.
	 * 
	 * @param creator the reference to the creator of the event
	 * @param event the reference to the event object
	 * @return the message
	 */
	public static EventMessage of(StorageReference creator, StorageReference event) {
		return new EventMessageImpl(creator, event);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends EventMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends EventMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends EventMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(EventMessage message) {
    		super(message);
    	}
    }
}