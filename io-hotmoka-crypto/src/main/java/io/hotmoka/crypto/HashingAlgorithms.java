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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;

import io.hotmoka.crypto.api.HashingAlgorithm;
import io.hotmoka.crypto.internal.SHA256;
import io.hotmoka.crypto.internal.SHABAL256;

/**
 * A provider of algorithms that hash values into bytes.
 */
public interface HashingAlgorithms {

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

	/**
	 * Yields the hashing algorithm with the given name.
	 * It looks for a factory method with the given name and invokes it.
	 * 
	 * @param <T> the type of the values that get hashed
	 * @param name the name of the algorithm, case-insensitive
	 * @param supplier how values get transformed into bytes, before being hashed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the given algorithm
	 */
	@SuppressWarnings("unchecked")
	static <T> HashingAlgorithm<T> mk(String name, BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		name = name.toLowerCase();

		try {
			// only sha256, shabal256 are currently found below
			Method method = HashingAlgorithms.class.getMethod(name, BytesSupplier.class);
			return (HashingAlgorithm<T>) method.invoke(null, supplier);
		}
		catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
			throw new NoSuchAlgorithmException("unknown hashing algorithm named " + name, e);
		}
	}

	/**
	 * Yields the hashing algorithm for the given type of values.
	 * 
	 * @param <T> the type of the values that get hashed
	 * @param type the type of the algorithm
	 * @param supplier how values get transformed into bytes, before being hashed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the given algorithm
	 */
	static <T> HashingAlgorithm<T> mk(TYPES type, BytesSupplier<? super T> supplier) throws NoSuchAlgorithmException {
		return mk(type.name(), supplier);
	}

	/**
	 * The alternatives of hashing algorithms currently implemented.
	 */
	enum TYPES {
		SHA256,
		SHABAL256
	}
}