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

package io.hotmoka.crypto.api;

import java.util.function.Function;

/**
 * An algorithm that hashes bytes into bytes.
 */
public interface HashingAlgorithm extends Cloneable {

	/**
	 * Yields a hasher with this hashing algorithm.
	 * 
	 * @param <T> the type of values that get hashed
	 * @param toBytes the function to use to transform the values into bytes before hashing
	 * @return the hasher
	 */
	<T> Hasher<T> getHasher(Function<? super T, byte[]> toBytes);

	/**
	 * The length of the sequence of bytes resulting from hashing a value.
	 * This length must be constant, independent from the specific value that gets hashed.
	 *
	 * @return the length
	 */
	int length();

	/**
	 * Yields the name of the algorithm.
	 * 
	 * @return the name of the algorithm
	 */
	String getName();

	/**
	 * Yields a clone of this hashing algorithm. This can be useful
	 * to run a parallel computation using this algorithm, since otherwise
	 * a single hashing algorithm object would synchronize the access.
	 * 
	 * @return the clone of this algorithm
	 */
	HashingAlgorithm clone();

	/**
     * Determines if this hashing algorithm is the same as another.
     * 
     * @param other the other object
     * @return true only if other is the same hashing algorithm
     */
    @Override
    boolean equals(Object other);

    @Override
    int hashCode();

    @Override
    String toString();
}