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

import io.hotmoka.beans.api.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.node.messages.api.RunInstanceMethodCallTransactionRequestMessage;
import io.hotmoka.node.messages.internal.RunInstanceMethodCallTransactionRequestMessageImpl;
import io.hotmoka.node.messages.internal.gson.RunInstanceMethodCallTransactionRequestMessageDecoder;
import io.hotmoka.node.messages.internal.gson.RunInstanceMethodCallTransactionRequestMessageEncoder;
import io.hotmoka.node.messages.internal.gson.RunInstanceMethodCallTransactionRequestMessageJson;

/**
 * A provider of {@link RunInstanceMethodCallTransactionRequestMessage}.
 */
public final class RunInstanceMethodCallTransactionRequestMessages {

	private RunInstanceMethodCallTransactionRequestMessages() {}

	/**
	 * Yields a {@link RunInstanceMethodCallTransactionRequestMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param request the request of the transaction required to run
	 * @return the message
	 */
	public static RunInstanceMethodCallTransactionRequestMessage of(InstanceMethodCallTransactionRequest request, String id) {
		return new RunInstanceMethodCallTransactionRequestMessageImpl(request, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends RunInstanceMethodCallTransactionRequestMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends RunInstanceMethodCallTransactionRequestMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends RunInstanceMethodCallTransactionRequestMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(RunInstanceMethodCallTransactionRequestMessage message) {
    		super(message);
    	}
    }
}