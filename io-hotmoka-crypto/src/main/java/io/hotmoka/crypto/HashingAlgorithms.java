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

import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.crypto.internal.AbstractHashingAlgorithmImpl;
import io.hotmoka.crypto.internal.Identity;
import io.hotmoka.crypto.internal.SHA256;
import io.hotmoka.crypto.internal.SHABAL256;

/**
 * A provider of algorithms that hash values into bytes.
 */
public final class HashingAlgorithms {

	private HashingAlgorithms() {}

	/**
	 * Yields the SHA256 hashing algorithm.
	 * 
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256 algorithm
	 */
	public static HashingAlgorithm sha256() throws NoSuchAlgorithmException {
		return new SHA256();
	}

	/**
	 * Yields the SHABAL256 hashing algorithm.
	 * 
	 * @return the algorithm
	 */
	public static HashingAlgorithm shabal256() {
		return new SHABAL256();
	}

	/**
	 * Yields the identity hashing algorithm for arrays of one byte.
	 * 
	 * @return the algorithm
	 */
	public static HashingAlgorithm identity1() {
		return new Identity(1);
	}

	/**
	 * Yields the identity hashing algorithm for arrays of 32 byte.
	 * 
	 * @return the algorithm
	 */
	public static HashingAlgorithm identity32() {
		return new Identity(32);
	}

	/**
	 * Yields the hashing algorithm with the given name.
	 * It looks for a factory method with the given name and invokes it.
	 * 
	 * @param name the name of the algorithm, case-insensitive
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the given algorithm
	 */
	public static HashingAlgorithm of(String name) throws NoSuchAlgorithmException {
		return AbstractHashingAlgorithmImpl.of(name);
	}
}