/*
Copyright 2023 Fausto Spoto

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

/**
 * An object that hashes a value into bytes.
 *
 * @param <T> the type of values that get hashed
 */
public interface Hasher<T> {

	/**
	 * Hashes the given value into a sequence of bytes.
	 * 
	 * @param what the value to hash
	 * @return the sequence of bytes, whose length is {@link #length()}
	 */
	byte[] hash(T what);

	/**
	 * Hashes a portion of the given value into a sequence of bytes.
	 * It first transforms {@code what} into bytes then selects the
	 * bytes from {@code start} (inclusive) to {@code start + length}
	 * (exclusive) and computes the hash of that part only.
	 * 
	 * @param what the value to hash
	 * @param start the initial byte position to consider for hashing;
	 *              this must be a position inside the translation of
	 *              {@code what} into bytes
	 * @param length the number of bytes (starting at {@code start})
	 *               that must be considered for hashing; this cannot be
	 *               negative and must lead to an existing position inside the
	 *               translation of {@code what} into bytes
	 * @return the sequence of bytes, whose length is {@link #length()}
	 */
	byte[] hash(T what, int start, int length);

	/**
	 * The length of the sequence of bytes resulting from hashing a value.
	 * This length must be constant, independent from the specific value that gets hashed.
	 *
	 * @return the length
	 */
	int length();
}