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
import io.hotmoka.node.messages.internal.gson.RunStaticMethodCallTransactionResultMessageDecoder;
import io.hotmoka.node.messages.internal.gson.RunStaticMethodCallTransactionResultMessageEncoder;
import io.hotmoka.node.messages.internal.gson.RunStaticMethodCallTransactionResultMessageJson;

/**
 * A provider of {@link RunStaticMethodCallTransactionResultMessage}.
 */
public final class RunStaticMethodCallTransactionResultMessages {

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
	public static class Encoder extends RunStaticMethodCallTransactionResultMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends RunStaticMethodCallTransactionResultMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
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