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

import io.hotmoka.node.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.messages.api.RunInstanceMethodCallTransactionMessage;
import io.hotmoka.node.messages.internal.RunInstanceMethodCallTransactionMessageImpl;
import io.hotmoka.node.messages.internal.json.RunInstanceMethodCallTransactionMessageDecoder;
import io.hotmoka.node.messages.internal.json.RunInstanceMethodCallTransactionMessageEncoder;
import io.hotmoka.node.messages.internal.json.RunInstanceMethodCallTransactionMessageJson;

/**
 * A provider of {@link RunInstanceMethodCallTransactionMessage}.
 */
public abstract class RunInstanceMethodCallTransactionMessages {

	private RunInstanceMethodCallTransactionMessages() {}

	/**
	 * Yields a {@link RunInstanceMethodCallTransactionMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param request the request of the transaction required to run
	 * @return the message
	 */
	public static RunInstanceMethodCallTransactionMessage of(InstanceMethodCallTransactionRequest request, String id) {
		return new RunInstanceMethodCallTransactionMessageImpl(request, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends RunInstanceMethodCallTransactionMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends RunInstanceMethodCallTransactionMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends RunInstanceMethodCallTransactionMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(RunInstanceMethodCallTransactionMessage message) {
    		super(message);
    	}
    }
}