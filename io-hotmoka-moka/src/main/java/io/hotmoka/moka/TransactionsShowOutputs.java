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

package io.hotmoka.moka;

import io.hotmoka.moka.api.transactions.TransactionsShowOutput;
import io.hotmoka.moka.internal.json.TransactionsShowOutputJson;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;
import jakarta.websocket.DecodeException;

/**
 * Providers of outputs for the {@code moka transactions show} command.
 */
public abstract class TransactionsShowOutputs {

	private TransactionsShowOutputs() {}

	/**
	 * Yields the output of the command from its JSON representation.
	 * 
	 * @param json the JSON representation of the output
	 * @return the output of the command
	 * @throws DecodeException if {@code json} cannot be decoded into the output
	 */
	public static TransactionsShowOutput from(String json) throws DecodeException {
		return new Decoder().decode(json);
	}

	/**
	 * JSON representation.
	 */
	public static class Json extends TransactionsShowOutputJson {
	
		/**
		 * Creates the JSON representation for the given output.
		 * 
		 * @param output the output
		 */
		public Json(TransactionsShowOutput output) {
			super(output);
		}
	}

	/**
	 * JSON encoder.
	 */
	public static class Encoder extends MappedEncoder<TransactionsShowOutput, Json> {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {
			super(Json::new);
		}
	}

	/**
	 * JSON decoder.
	 */
	public static class Decoder extends MappedDecoder<TransactionsShowOutput, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}
}