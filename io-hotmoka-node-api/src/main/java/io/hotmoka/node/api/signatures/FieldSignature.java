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

package io.hotmoka.node.api.signatures;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.Marshallable;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.types.StorageType;

/**
 * The signature of a field of a class.
 */
@Immutable
public interface FieldSignature extends Marshallable, Comparable<FieldSignature> {

	/**
	 * Yields the class of the field.
	 * 
	 * @return the class of the field
	 */
	ClassType getDefiningClass();

	/**
	 * Yields the name of the field.
	 * 
	 * @return the name of the field
	 */
	String getName();

	/**
	 * Yields the type of the field.
	 * 
	 * @return the type of the field
	 */
	StorageType getType();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}