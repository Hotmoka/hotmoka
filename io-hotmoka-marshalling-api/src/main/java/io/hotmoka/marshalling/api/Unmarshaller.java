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

package io.hotmoka.marshalling.api;

import java.io.IOException;

/**
 * A function that unmarshals a single marshallable.
 *
 * @param <T> the type of the marshallable
 */
public interface Unmarshaller<T extends Marshallable> {

	/**
	 * Yields the marshallable extracted from the given context.
	 * 
	 * @param context the context
	 * @return the marshallable
	 * @throws IOException if an I/O error occurs
	 */
	T from(UnmarshallingContext context) throws IOException;
}