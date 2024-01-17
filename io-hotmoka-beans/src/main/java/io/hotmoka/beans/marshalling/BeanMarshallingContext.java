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

package io.hotmoka.beans.marshalling;

import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.beans.marshalling.internal.FieldSignatureMarshaller;
import io.hotmoka.beans.marshalling.internal.StorageReferenceMarshaller;
import io.hotmoka.beans.marshalling.internal.TransactionReferenceMarshaller;
import io.hotmoka.marshalling.AbstractMarshallingContext;

/**
 * A context used during bean marshalling into bytes.
 */
public class BeanMarshallingContext extends AbstractMarshallingContext {

	/**
	 * Creates the context.
	 * 
	 * @param oos the stream where bytes are marshalled.
	 * @throws IOException if the context cannot be created
	 */
	public BeanMarshallingContext(OutputStream oos) throws IOException {
		super(oos);
		
		registerObjectMarshaller(new TransactionReferenceMarshaller());
		registerObjectMarshaller(new StorageReferenceMarshaller());
		registerObjectMarshaller(new FieldSignatureMarshaller());
	}
}