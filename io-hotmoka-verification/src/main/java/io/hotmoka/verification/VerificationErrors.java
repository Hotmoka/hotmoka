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

package io.hotmoka.verification;

import io.hotmoka.verification.api.VerificationError;
import io.hotmoka.verification.internal.json.VerificationErrorJson;
import io.hotmoka.websockets.beans.MappedDecoder;
import io.hotmoka.websockets.beans.MappedEncoder;

/**
 * A provider of verification errors.
 */
public abstract class VerificationErrors {

	private VerificationErrors() {}

	/**
	 * JSON encoder.
	 */
	public static class Encoder extends MappedEncoder<VerificationError, Json> {

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
	public static class Decoder extends MappedDecoder<VerificationError, Json> {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {
			super(Json.class);
		}
	}

    /**
     * JSON representation.
     */
    public static class Json extends VerificationErrorJson {

    	/**
    	 * Creates the JSON representation for the given verification error.
    	 * 
    	 * @param error the verification error
    	 */
    	public Json(VerificationError error) {
    		super(error);
    	}
    }
}