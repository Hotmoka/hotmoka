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

package io.hotmoka.verification.api;

import java.util.stream.Stream;

/**
 * A jar that has undergone static verification, before being installed into a Hotmoka node.
 */
public interface VerifiedJar {

	/**
	 * Yields the stream of the classes of the jar that passed verification.
	 * 
	 * @return the classes, in increasing order
	 */
	Stream<VerifiedClass> getClasses();

	/**
	 * Yields the errors generated during the verification of this jar, if any, in order.
	 * 
	 * @return the errors
	 */
	Stream<Error> getErrors();

	/**
	 * Yields the class loader used to load this jar.
	 * 
	 * @return the class loader
	 */
	TakamakaClassLoader getClassLoader();

	/**
	 * Yields the utility object that can be used to check the annotations in the methods in this jar.
	 * 
	 * @return the utility object
	 */
	Annotations getAnnotations();
}