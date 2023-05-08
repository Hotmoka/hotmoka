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

package io.hotmoka.marshalling;

import java.io.IOException;
import java.io.OutputStream;

import io.hotmoka.marshalling.internal.MarshallingContextImpl;

/**
 * Implementation of a marshalling context, for subclassing.
 */
public abstract class AbstractMarshallingContext extends MarshallingContextImpl {

	/**
	 * Creates a marshalling context for the given output stream.
	 * 
	 * @param oos the output stream
	 * @throws IOException if the marshalling context could not be created
	 */
	protected AbstractMarshallingContext(OutputStream oos) throws IOException {
		super(oos);
	}
}