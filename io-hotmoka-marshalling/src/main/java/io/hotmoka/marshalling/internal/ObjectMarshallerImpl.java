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

package io.hotmoka.marshalling.internal;

import io.hotmoka.marshalling.api.ObjectMarshaller;

/**
 * Knowledge about how an object of a given class can be marshalled.
 * This can be used to provide the ability to marshall objects of arbitrary classes.
 * 
 * @param <C> the type of the class of the marshalled objects
 */
public abstract class ObjectMarshallerImpl<C> implements ObjectMarshaller<C> {

	/**
	 * The class of the marshalled objects.
	 */
	private final Class<C> clazz;

	/**
	 * Creates the object marshaller.
	 * 
	 * @param clazz the class of the marshalled objects
	 */
	protected ObjectMarshallerImpl(Class<C> clazz) {
		this.clazz = clazz;
	}

	@Override
	public final Class<C> clazz() {
		return clazz;
	}
}