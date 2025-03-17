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

package io.hotmoka.node;

import java.io.IOException;

import io.hotmoka.marshalling.api.UnmarshallingContext;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;
import io.hotmoka.node.internal.gson.ConstructorSignatureDecoder;
import io.hotmoka.node.internal.gson.ConstructorSignatureEncoder;
import io.hotmoka.node.internal.gson.ConstructorSignatureJson;
import io.hotmoka.node.internal.signatures.ConstructorSignatureImpl;

/**
 * Providers of constructor signatures.
 */
public abstract class ConstructorSignatures {

	private ConstructorSignatures() {}

	/**
	 * Yields the signature of a constructor.
	 * 
	 * @param definingClass the class defining the constructor
	 * @param formals the formal arguments of the constructor
	 * @return the signature of the constructor
	 */
	public static ConstructorSignature of(ClassType definingClass, StorageType... formals) {
		return new ConstructorSignatureImpl(definingClass, formals, IllegalArgumentException::new);
	}

	/**
	 * Unmarshals a constructor signature from the given context.
	 * 
	 * @param context the unmarshalling context
	 * @return the constructor signature
	 * @throws IOException if the constructor signature cannot be unmarshalled
	 */
	public static ConstructorSignature from(UnmarshallingContext context) throws IOException {
		return ConstructorSignatureImpl.from(context);
	}

	/**
	 * Gson encoder.
	 */
	public static class Encoder extends ConstructorSignatureEncoder {

		/**
		 * Creates a new encoder.
		 */
		public Encoder() {}
	}

	/**
	 * Gson decoder.
	 */
	public static class Decoder extends ConstructorSignatureDecoder {

		/**
		 * Creates a new decoder.
		 */
		public Decoder() {}
	}

    /**
     * Json representation.
     */
    public static class Json extends ConstructorSignatureJson {

    	/**
    	 * Creates the Json representation for the given constructor signature.
    	 * 
    	 * @param constructor the constructor signature
    	 */
    	public Json(ConstructorSignature constructor) {
    		super(constructor);
    	}
    }

    /**
	 * The constructor of an externally owned account.
	 */
	public final static ConstructorSignature EOA_CONSTRUCTOR = ConstructorSignatureImpl.EOA_CONSTRUCTOR;
}