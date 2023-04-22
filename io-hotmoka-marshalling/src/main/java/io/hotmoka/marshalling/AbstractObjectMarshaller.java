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

import io.hotmoka.marshalling.internal.ObjectMarshallerImpl;

/**
 * Partial implementation of an object marshaller, for subclassing.
 * 
 * @param <C> the type of the class of the marshalled objects
 */
public abstract class AbstractObjectMarshaller<C> extends ObjectMarshallerImpl<C> {

	/**
	 * Creates the object marshaller.
	 * 
	 * @param clazz the class of the marshalled objects
	 */
	protected AbstractObjectMarshaller(Class<C> clazz) {
		super(clazz);
	}
}