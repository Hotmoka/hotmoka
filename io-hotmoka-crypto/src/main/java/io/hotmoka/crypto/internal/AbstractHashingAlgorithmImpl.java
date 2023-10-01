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

import java.util.function.Function;

import io.hotmoka.crypto.api.Hasher;
import io.hotmoka.crypto.api.HashingAlgorithm;

/**
 * A partial implementation of a hashing algorithm, that
 * provides a general implementation for partial hashing.
 * Subclasses might provide better implementations.
 */
public abstract class AbstractHashingAlgorithmImpl implements HashingAlgorithm {

	/**
	 * Hashes the given bytes.
	 * 
	 * @param bytes the bytes to hash
	 * @return the resulting hash; this must have length equals to {@linkplain #length()}
	 */
	protected abstract byte[] hash(byte[] bytes);

	/**
	 * Hashes a portion of the given array of bytes, from
	 * {@code start} (inclusive) to {@code start + length}
	 * (exclusive) and computes the hash of that part only.
	 * 
	 * @param bytes the bytes to hash
	 * @param start the initial byte position to consider for hashing;
	 *              this must be a position inside {@code bytes}
	 * @param length the number of bytes (starting at {@code start})
	 *               that must be considered for hashing; this cannot be
	 *               negative and must lead to an existing position inside {@code bytes}
	 * @return the hash; this must have length equals to {@linkplain #length()}
	 */
	protected byte[] hash(byte[] bytes, int start, int length) {
		if (start < 0)
			throw new IllegalArgumentException("start cannot be negative");

		if (length < 0)
			throw new IllegalArgumentException("length cannot be negative");

		byte[] all = hash(bytes); // TODO: no!!!
		if (start + length >= all.length)
			throw new IllegalArgumentException("Trying to hash a portion larger than the array of bytes");

		for (int pos = 0; pos < all.length; pos++)
			if (pos < start || pos >= start + length)
				all[pos] = 0;

		return all;
	}

	@Override
	public <T> Hasher<T> getHasher(Function<? super T, byte[]> toBytes) {
		return new Hasher<>() {

			@Override
			public byte[] hash(T what) {
				return AbstractHashingAlgorithmImpl.this.hash(toBytes.apply(what));
			}

			@Override
			public byte[] hash(T what, int start, int length) {
				return AbstractHashingAlgorithmImpl.this.hash(toBytes.apply(what), start, length);
			}

			@Override
			public int length() {
				return AbstractHashingAlgorithmImpl.this.length();
			}
		};
	}

	/**
	 * Yields this same instance. Subclasses may redefine.
	 * 
	 * @return this same instance
	 */
	@Override
	public AbstractHashingAlgorithmImpl clone() {
		return this;
	}

	@Override
	public boolean equals(Object other) {
		return other != null && getClass() == other.getClass();
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return getName();
	}
}