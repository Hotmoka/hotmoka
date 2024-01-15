/*
Copyright 2021 Fausto Spoto

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

package io.hotmoka.marshalling.api;

import java.io.IOException;

/**
 * An object that can be marshaled into a stream, in a way more compact
 * than standard Java serialization. Typically, this works because
 * of context information about the structure of the object.
 */
public interface Marshallable { // TODO: make this generic wrt the type of context?

	/**
	 * Marshals this object into a given stream. This method in general
	 * performs better than standard Java serialization, wrt the size of the marshalled data.
	 * 
	 * @param context the context holding the stream
	 * @throws IOException if this object cannot be marshalled
	 */
	void into(MarshallingContext context) throws IOException;

	/**
	 * Marshals this object into a byte array.
	 * 
	 * @return the byte array resulting from marshalling this object
	 */
	byte[] toByteArray();

	/**
	 * Yields the size of this object, in terms of bytes in marshalled form.
	 * 
	 * @return the size
	 */
	int size();
}