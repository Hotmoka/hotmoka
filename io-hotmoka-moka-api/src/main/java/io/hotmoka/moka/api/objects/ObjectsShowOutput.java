/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.api.objects;

import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.node.api.updates.ClassTag;
import io.hotmoka.node.api.updates.UpdateOfField;

/**
 * The output of the {@code moka objects show} command.
 */
@Immutable
public interface ObjectsShowOutput {

	/**
	 * Yields the class tag of the object.
	 * 
	 * @return the class tag of the object
	 */
	ClassTag getClassTag();

	/**
	 * Yields the updates that describe the state of the object.
	 * 
	 * @return the updates that describe the state of the object
	 */
	Stream<UpdateOfField> getFields();

	/**
	 * Yields the descriptions of the white-listed constructors of the object.
	 * 
	 * @return the descriptions of the constructors
	 */
	Stream<ConstructorDescription> getConstructorDescriptions();

	/**
	 * Yields the descriptions of the instance white-listed methods of the object,
	 * including those inherited from superclasses.
	 * 
	 * @return the descriptions of the methods
	 */
	Stream<MethodDescription> getMethodDescriptions();

	/**
	 * The description of a constructor.
	 */
	@Immutable
	interface ConstructorDescription extends Comparable<ConstructorDescription> {

		/**
		 * Yields the description of the annotations of the constructor.
		 * 
		 * @return the description of the annotations of the constructor
		 */
		String getAnnotations();

		/**
		 * Yields the description of the signature of the constructor.
		 * 
		 * @return the description of the signature of the constructor
		 */
		String getSignature();
	}

	/**
	 * The description of a method.
	 */
	@Immutable
	interface MethodDescription extends Comparable<MethodDescription> {

		/**
		 * Yields the description of the annotations of the method.
		 * 
		 * @return the description of the annotations of the method
		 */
		String getAnnotations();

		/**
		 * Yields the description of the class defining the method.
		 * 
		 * @return the description of the class defining the method
		 */
		String getDefiningClass();

		/**
		 * Yields the description of the signature of the method.
		 * 
		 * @return the description of the signature of the method
		 */
		String getSignature();
	}
}