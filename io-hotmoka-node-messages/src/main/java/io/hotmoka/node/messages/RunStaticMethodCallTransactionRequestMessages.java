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

import io.hotmoka.beans.api.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.node.messages.api.RunStaticMethodCallTransactionRequestMessage;
import io.hotmoka.node.messages.internal.RunStaticMethodCallTransactionRequestMessageImpl;
import io.hotmoka.node.messages.internal.gson.RunStaticMethodCallTransactionRequestMessageDecoder;
import io.hotmoka.node.messages.internal.gson.RunStaticMethodCallTransactionRequestMessageEncoder;
import io.hotmoka.node.messages.internal.gson.RunStaticMethodCallTransactionRequestMessageJson;

/**
 * A provider of {@link RunStaticMethodCallTransactionRequestMessage}.
 */
public final class RunStaticMethodCallTransactionRequestMessages {

	private RunStaticMethodCallTransactionRequestMessages() {}

	/**
	 * Yields a {@link RunStaticMethodCallTransactionRequestMessage}.
	 * 
	 * @param id the identifier of the message
	 * @param request the request of the transaction required to run
	 * @return the message
	 */
	public static RunStaticMethodCallTransactionRequestMessage of(StaticMethodCallTransactionRequest request, String id) {
		return new RunStaticMethodCallTransactionRequestMessageImpl(request, id);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends RunStaticMethodCallTransactionRequestMessageEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends RunStaticMethodCallTransactionRequestMessageDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

	/**
     * Json representation.
     */
    public static class Json extends RunStaticMethodCallTransactionRequestMessageJson {

    	/**
    	 * Creates the Json representation for the given message.
    	 * 
    	 * @param message the message
    	 */
    	public Json(RunStaticMethodCallTransactionRequestMessage message) {
    		super(message);
    	}
    }
}