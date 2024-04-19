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

package io.hotmoka.node.api.types;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.marshalling.api.Marshallable;

/**
 * The types that can be used in storage objects in blockchain.
 */
@Immutable
public interface StorageType extends Marshallable, Comparable<StorageType> {

	/**
	 * Yields the fully-qualified name of this type.
	 * 
	 * @return the fully-qualified name
	 */
	String getName();

	/**
	 * Compares this storage type with another. Puts first basic types, by name,
	 * then class types ordered wrt class name.
	 * 
	 * @param other the other type
	 * @return -1 if {@code this} comes first, 1 if {@code other} comes first, 0 if they are equal
	 */
	@Override
	int compareTo(StorageType other);

	/**
	 * Determines if this type is eager.
	 * 
	 * @return true if and only if this type is eager
	 */
	boolean isEager();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}