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

import java.util.Optional;

import io.hotmoka.node.api.values.StorageValue;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionResultMessage;
import io.hotmoka.node.messages.internal.RunStaticMethodCallTransactionResultMessageImpl;
import io.hotmoka.node.messages.internal.json.RunStaticMethodCallTransactionResultMessageJson;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;

/**
 * A provider of {@link RunStaticMethodCallTransactionResultMessage}.
 */
public abstract class RunStaticMethodCallTransactionResultMessages {

	private RunStaticMethodCallTransactionResultMessages() {}

	/**
	 * Yields a {@link RunStaticMethodCallTransactionResultMessage}.
	 * 
	 * @param result the result of the call; this might be empty for void methods
	 * @param id the identifier of the message
	 * @return the message
	 */
	public static RunStaticMethodCallTransactionResultMessage of(Optional<StorageValue> result, String id) {
		return new RunStaticMethodCallTransactionResultMessageImpl(result, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends MappedEncoder<RunStaticMethodCallTransactionResultMessage, Json> {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {
			super(Json::new);
		}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends MappedDecoder<RunStaticMethodCallTransactionResultMessage, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}

	/**
     * Json representation.
     */
    public static class Json extends RunStaticMethodCallTransactionResultMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(RunStaticMethodCallTransactionResultMessage message) {
    		super(message);
    	}
    }
}