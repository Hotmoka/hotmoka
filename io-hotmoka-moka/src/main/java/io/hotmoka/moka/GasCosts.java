/*
Copyright 2023 Fausto Spoto

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

import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.internal.json.GasCostJson;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;

/**
 * Providers of gas costs incurred for the already occurred execution of some requests.
 */
public abstract class GasCosts {

	private GasCosts() {}

	// TODO: maybe remove varargs?

	/**
	 * JSON representation.
	 */
	public static class Json extends GasCostJson {
	
		/**
		 * Creates the JSON representation for the given gas cost.
		 * 
		 * @param output the gas cost
		 */
		public Json(GasCost output) {
			super(output);
		}
	}

	/**
	 * JSON encoder.
	 */
	public static class Encoder extends MappedEncoder<GasCost, Json> {

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
	public static class Decoder extends MappedDecoder<GasCost, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}
}