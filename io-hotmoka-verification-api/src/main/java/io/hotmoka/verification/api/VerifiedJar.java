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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A jar that has undergone static verification, before being installed into blockchain.
 */
public interface VerifiedJar {

	/**
	 * Determines if the verification of at least one class of the jar failed with an error.
	 * 
	 * @return true if and only if that condition holds
	 */
	boolean hasErrors();

	/**
	 * Yields the first error that occurred during the verification of the origin jar.
	 */
	// TODO: remove?
	Optional<Error> getFirstError();

	/**
	 * Yields the stream of the classes of the jar that passed verification.
	 * 
	 * @return the classes, in increasing order
	 */
	Stream<VerifiedClass> classes();

	/**
	 * Performs the given action on each error generated during the verification of the classes of the jar,
	 * in increasing order.
	 * 
	 * @return action the action
	 */
	void forEachError(Consumer<Error> action);

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

	/**
	 * Yields the utility object that can be used to transform BCEL types into their corresponding
	 * Java class tag, by using the class loader of this jar.
	 * 
	 * @return the utility object
	 */
	BcelToClass getBcelToClass();
}