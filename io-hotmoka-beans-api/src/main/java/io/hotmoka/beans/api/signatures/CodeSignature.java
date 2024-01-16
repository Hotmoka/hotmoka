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

package io.hotmoka.beans.api.signatures;

import java.util.stream.Stream;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.types.StorageType;

/**
 * The signature of a method or constructor.
 */
@Immutable
public interface CodeSignature {

	/**
	 * Yields the class of the method or constructor.
	 * 
	 * @return the class
	 */
	ClassType getDefiningClass();

	/**
	 * Yields the formal arguments of the method or constructor, ordered left to right.
	 * 
	 * @return the formal arguments
	 */
	Stream<StorageType> getFormals();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}