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

/**
 * A sorted set of (non-{@code null}) storage values,
 * that can be kept in storage. By iterating on this object, one gets
 * the values in the set, in increasing order. This interface includes
 * read and modification methods.
 * 
 * @param <V> the type of the values. This type must be allowed in storage
 */

public interface StorageSet<V> extends StorageSetView<V> {

	/**
	 * Adds the specified value into this set, if it is not already there.
	 *
	 * @param value the value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	void add(V value);

	/**
	 * Removes the smallest value from this set.
	 * 
	 * @throws NoSuchElementException if this set is empty
	 */
	void removeMin();

	/**
	 * Removes the largest value from this set.
	 * 
	 * @throws NoSuchElementException if this set is empty
	 */
	void removeMax();

	/**
	 * Removes the specified value from this set, if it is there.
	 *
	 * @param  value the value
	 * @throws IllegalArgumentException if {@code value} is {@code null}
	 */
	void remove(Object value);

	/**
	 * Yields a view of this set. The view reflects the elements in this set:
	 * any future modification of this set will be seen also through the view.
	 * A view is always {@link io.takamaka.code.lang.Exported}.
	 * 
	 * @return a view of this set
	 */
	StorageSetView<V> view();
}