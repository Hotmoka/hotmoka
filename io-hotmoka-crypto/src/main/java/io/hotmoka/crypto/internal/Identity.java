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

import io.hotmoka.crypto.AbstractHashingAlgorithm;

/**
 * The identity hashing algorithm. It hashes arrays of bytes into themselves.
 */
public class Identity extends AbstractHashingAlgorithm {

	/**
	 * The size of the arrays of bytes that can be hashed with this algorithm.
	 */
	private final int size;

	/**
	 * Creates the hashing algorithm.
	 * 
	 * @param size the size of the arrays of bytes that can be hashed with this algorithm
	 */
	public Identity(int size) {
		if (size < 0)
			throw new IllegalArgumentException("size cannot be negative");

		this.size = size;
	}

	@Override
	protected byte[] hash(byte[] bytes) {
		if (bytes.length != size)
			throw new IllegalArgumentException("This hashing algorithm works over arrays of length " + size + " only");

		return bytes;
	}

	@Override
	protected byte[] hash(byte[] bytes, int start, int length) {
		if (bytes.length != size)
			throw new IllegalArgumentException("This hashing algorithm works over arrays of length " + size + " only");

		byte[] copy = bytes.clone();

		// we set to 0 all bytes outside the selected window
		for (int pos = 0; pos < size; pos++)
			if (pos < start || pos >= start + length)
				copy[pos] = 0;

		return copy;
	}

	@Override
	public int length() {
		return size;
	}

	@Override
	public Identity clone() {
		return new Identity(size);
	}

	@Override
	public String getName() {
		return super.getName() + size;
	}
}