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

package io.takamaka.code.util;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

import io.takamaka.code.lang.View;

/**
 * A sorted set of (non-{@code null}) storage values.
 * By iterating on this object, one gets the values in the set, in increasing order.
 * This interface has only access methods. Its sub-interface
 * {@link StorageSet} includes modification methods as well.
 * 
 * @param <V> the type of the values. This type must be allowed in storage
 */

public interface StorageSetView<V> extends Iterable<V> {

	/**
	 * Returns the number of values in this set.
	 * 
	 * @return the number of values in this set
	 */
	@View int size();

	/**
	 * Determines if this set is empty.
	 * 
	 * @return {@code true} if and only if this set is empty
	 */
	@View boolean isEmpty();

	/**
	 * Determines if this set contains the given value.
	 * 
	 * @param value the value
	 * @return {@code true} if and only if this set contains {@code value}
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View boolean contains(Object value);

	/**
	 * Yields the smallest value in this set.
	 * 
	 * @return the smallest value in this set
	 * @throws NoSuchElementException if the set is empty
	 */
	@View V min();

	/**
	 * Yields the largest value in this set.
	 * 
	 * @return the largest value in this set
	 * @throws NoSuchElementException if the set is empty
	 */
	@View V max();

	/**
	 * Yields the largest value in this set less than or equal to {@code value}.
	 * 
	 * @param value the reference value
	 * @return the largest value in this set less than or equal to {@code value}
	 * @throws NoSuchElementException if there is no such value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View V floorKey(Object value);

	/**
	 * Yields the smallest value in this set greater than or equal to {@code value}.
	 * 
	 * @param value the value
	 * @return the smallest value in this set greater than or equal to {@code value}
	 * @throws NoSuchElementException if there is no such value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View V ceilingKey(Object value);

	/**
	 * Yields the value in this set whose rank is {@code k}.
	 * This is the (k+1)st smallest value in this set. 
	 *
	 * @param  k the rank
	 * @return the value in this set, of rank {@code k}
	 * @throws IllegalArgumentException unless {@code k} is between 0 and {@code size()-1}
	 */
	@View V select(int k);

	/**
	 * Yields the number of values in this set strictly less than {@code value}.
	 * 
	 * @param value the value
	 * @return the number of values in this set strictly less than {@code value}
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	@View int rank(Object value);

	/**
	 * Yields an ordered stream of the values in this set, in increasing order.
	 * 
	 * @return the stream
	 */
	Stream<V> stream();

	/**
	 * Yields a snapshot of this set. The snapshot contains the elements in this set
	 * but is independent from this set: any future modification of this set will
	 * not be seen through the snapshot. A snapshot is always
	 * {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a snapshot of this set
	 */
	StorageSetView<V> snapshot();
}