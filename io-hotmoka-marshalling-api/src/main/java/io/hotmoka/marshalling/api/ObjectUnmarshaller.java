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
 * Knowledge about how an object of a given class can be unmarshalled.
 * This can be used to provide the ability to unmarshall objects of arbitrary classes.
 * 
 * @param <C> the type of the class of the unmarshalled objects
 */
public interface ObjectUnmarshaller<C> {

	/**
	 * Yields the class of the objects unmarshalled by this object.
	 * 
	 * @return the class
	 */
	Class<C> clazz();

	/**
	 * How an object of class <code>C</code> can be unmarshalled.
	 * 
	 * @param context the unmarshalling context
	 * @return the unmarshalled object
	 * @throws IOException if the object could not be unmarshalled
	 */
	C read(UnmarshallingContext context) throws IOException;
}