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

package io.hotmoka.whitelisting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * An object that knows about the fields, methods and constructors that can be called from
 * Takamaka code and their proof-obligations.
 */
public interface WhiteListingWizard {

	/**
	 * Looks for a white-listing model of the given field. That is a field declaration
	 * that justifies why the field is white-listed. It can be the field itself, if it
	 * belongs to a class installed in blockchain, or otherwise a field of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param field the field whose model is looked for
	 * @return the model of its white-listing, if it exists
	 */
	Optional<Field> whiteListingModelOf(Field field);

	/**
	 * Looks for a white-listing model of the given constructor. That is a constructor declaration
	 * that justifies why the constructor is white-listed. It can be the constructor itself, if it
	 * belongs to a class installed in blockchain, or otherwise a constructor of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param constructor the constructor whose model is looked for
	 * @return the model of its white-listing, if it exists
     */
	Optional<Constructor<?>> whiteListingModelOf(Constructor<?> constructor);

	/**
	 * Looks for a white-listing model of the given method. That is a method declaration
	 * that justifies why the method is white-listed. It can be the method itself, if it
	 * belongs to a class installed in blockchain, or otherwise a method of a white-listing
	 * class, if it belongs to some Java run-time support class.
	 * 
	 * @param method the method whose model is looked for
	 * @return the model of its white-listing, if it exists
     */
	Optional<Method> whiteListingModelOf(Method method);
}