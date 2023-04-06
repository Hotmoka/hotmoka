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

package io.hotmoka.crypto.internal;

import io.hotmoka.crypto.HashingAlgorithm;

/**
 * A partial implementation of a hashing algorithm, that
 * provides a general implementation for partial hashing.
 * Subclasses might provide better implementations.
 * 
 * @param <T> the type of values that get hashed
 */
public abstract class AbstractHashingAlgorithmImpl<T> implements HashingAlgorithm<T>{

	@Override
	public byte[] hash(T what, int start, int length) {
		if (start < 0)
			throw new IllegalArgumentException("start cannot be negative");

		if (length < 0)
			throw new IllegalArgumentException("length cannot be negative");

		byte[] all = hash(what);
		if (start + length >= all.length)
			throw new IllegalArgumentException("trying to hash a portion larger than the array of bytes");

		for (int pos = 0; pos < all.length; pos++)
			if (pos < start || pos >= start + length)
				all[pos] = 0;

		return all;
	}

	/**
	 * Yields this same instance. Subclasses may redefine.
	 * 
	 * @return this same instance
	 */
	@Override
	public AbstractHashingAlgorithmImpl<T> clone() {
		return this;
	}
}