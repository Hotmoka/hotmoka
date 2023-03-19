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

package io.hotmoka.crypto;

import java.security.NoSuchAlgorithmException;

import io.hotmoka.crypto.internal.SHA256;
import io.hotmoka.crypto.internal.SHABAL256;

/**
 * An algorithm that hashes values into bytes.
 *
 * @param <T> the type of values that get hashed.
 */
public interface HashingAlgorithm<T> {

	/**
	 * Hashes the given value into a sequence of bytes.
	 * 
	 * @param what the value to hash
	 * @return the sequence of bytes; this must have length equals to {@linkplain #length()}
	 */
	byte[] hash(T what);

	/**
	 * The length of the sequence of bytes resulting from hashing a value.
	 * This length must be constant, independent from the specific value that gets hashed.
	 *
	 * @return the length
	 */
	int length();

	/**
	 * Yields the SHA256 hashing algorithm.
	 * 
	 * @param <T> the type of values that get hashed
	 * @param supplier how values get transformed into bytes, before being hashed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256 algorithm
	 */
	static <T> HashingAlgorithm<T> sha256(BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return new SHA256<>(supplier);
	}

	/**
	 * Yields the SHABAL256 hashing algorithm.
	 * 
	 * @param <T> the type of values that get hashed
	 * @param supplier how values get transformed into bytes, before being hashed
	 * @return the algorithm
	 */
	static <T> HashingAlgorithm<T> shabal256(BytesSupplier<? super T> supplier) {
		return new SHABAL256<>(supplier);
	}
}