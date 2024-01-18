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

package io.hotmoka.beans;

import java.io.IOException;

import io.hotmoka.beans.api.types.StorageType;
import io.hotmoka.beans.api.updates.Update;
import io.hotmoka.beans.internal.gson.StorageTypeDecoder;
import io.hotmoka.beans.internal.gson.StorageTypeEncoder;
import io.hotmoka.beans.internal.gson.StorageTypeJson;
import io.hotmoka.beans.internal.updates.AbstractUpdate;
import io.hotmoka.marshalling.api.UnmarshallingContext;

/**
 * Providers of updates.
 */
public abstract class Updates {

	private Updates() {}

	/**
	 * Yields the update unmarshalled from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the update
	 * @throws IOException if the update cannot be marshalled
     */
	public static Update from(UnmarshallingContext context) throws IOException {
		return AbstractUpdate.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends StorageTypeEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends StorageTypeDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends StorageTypeJson {

    	/**
    	 * Creates the Json representation for the given type.
    	 * 
    	 * @param type the type
    	 */
    	public Json(StorageType type) {
    		super(type);
    	}
    }
}