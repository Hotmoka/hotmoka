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
import java.util.function.Function;

import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.internal.ED25519;
import io.hotmoka.crypto.internal.ED25519DET;
import io.hotmoka.crypto.internal.EMPTY;
import io.hotmoka.crypto.internal.QTESLA1;
import io.hotmoka.crypto.internal.QTESLA3;
import io.hotmoka.crypto.internal.SHA256DSA;

/**
 * Provider of algorithms that sign values and verify signatures back.
 */
public final class SignatureAlgorithms {

	private SignatureAlgorithms() {}

	/**
	 * Yields a signature algorithm that uses the SHA256 hashing algorithm and then the DSA algorithm.
	 * 
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256withDSA algorithm
	 */
	public static <T> SignatureAlgorithm<T> sha256dsa(Function<? super T, byte[]> supplier) throws NoSuchAlgorithmException {
		return new SHA256DSA<>(supplier);
	}

	/**
	 * Yields the ed25519 signature algorithm.
	 * 
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the ed25519 algorithm
	 */
	public static <T> SignatureAlgorithm<T> ed25519(Function<? super T, byte[]> supplier) throws NoSuchAlgorithmException {
		return new ED25519<>(supplier);
	}

	/**
	 * Yields a signature algorithm that uses the ed25519 cryptography. It generates
	 * keys in a deterministic order, hence must NOT be used in production.
	 * It is useful instead for testing, since it makes deterministic the
	 * sequence of keys of the accounts in the tests and consequently
	 * also the gas costs of such accounts when they are put into maps, for instance.
	 * 
	 * @param <T> the type of the values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the ed25519 algorithm
	 */
	public static <T> SignatureAlgorithm<T> ed25519det(Function<? super T, byte[]> supplier) throws NoSuchAlgorithmException {
		return new ED25519DET<>(supplier);
	}

	/**
	 * Yields the qTESLA-p-I signature algorithm.
	 *
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the qTESLA-p-I algorithm
	 */
	public static <T> SignatureAlgorithm<T> qtesla1(Function<? super T, byte[]> supplier) throws NoSuchAlgorithmException {
		return new QTESLA1<>(supplier);
	}

	/**
	 * Yields the qTESLA-p-III signature algorithm.
	 *
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the qTESLA-p-III algorithm
	 */
	public static <T> SignatureAlgorithm<T> qtesla3(Function<? super T, byte[]> supplier) throws NoSuchAlgorithmException {
		return new QTESLA3<>(supplier);
	}

	/**
	 * Yields an empty signature algorithm that signs everything with an empty array of bytes.
	 * 
	 * @param <T> the type of values that get signed
	 * @param supplier how values get transformed into bytes, before being hashed and then signed;
	 *                 this is not actually used by this algorithm
	 * @return the algorithm
	 */
	public static <T> SignatureAlgorithm<T> empty(Function<? super T, byte[]> supplier) {
		return new EMPTY<>();
	}

	/**
	 * Yields the signature algorithm with the given name.
	 * It looks for a factory method with the given name and invokes it.
	 * 
	 * @param <T> the type of the values that get signed
	 * @param name the name of the algorithm, case-insensitive
	 * @param supplier how values get transformed into bytes, before being hashed and then signed
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the given algorithm
	 */
	@SuppressWarnings("unchecked")
	public static <T> SignatureAlgorithm<T> of(String name, Function<? super T, byte[]> supplier) throws NoSuchAlgorithmException {
		name = name.toLowerCase();

		try {
			// only sha256dsa, ed25519, empty, qtesla1 and qtesla3 are currently found below
			Method method = SignatureAlgorithms.class.getMethod(name, Function.class);
			return (SignatureAlgorithm<T>) method.invoke(null, supplier);
		}
		catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
			throw new NoSuchAlgorithmException("Unknown signature algorithm named " + name, e);
		}
	}
}