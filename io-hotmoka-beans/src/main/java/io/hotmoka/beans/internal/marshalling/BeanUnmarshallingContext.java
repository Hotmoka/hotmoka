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

package io.hotmoka.beans.internal.marshalling;

import java.io.IOException;
import java.io.InputStream;

import io.hotmoka.marshalling.AbstractUnmarshallingContext;

/**
 * A context used during bytes unmarshalling into beans.
 * It understands the optimized marshalling obtained through
 * a {@link BeanMarshallingContext}.
 */
public class BeanUnmarshallingContext extends AbstractUnmarshallingContext {

	/**
	 * Creates the context.
	 * 
	 * @param is the stream from which bytes get unmarshalled
	 * @throws IOException if the context cannot be created
	 */
	public BeanUnmarshallingContext(InputStream is) throws IOException {
		super(is);

		registerObjectUnmarshaller(new StorageReferenceUnmarshaller());
		registerObjectUnmarshaller(new TransactionReferenceUnmarshaller());
		registerObjectUnmarshaller(new FieldSignatureUnmarshaller());
	}
}