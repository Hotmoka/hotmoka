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

import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.crypto.internal.AbstractSignatureAlgorithmImpl;
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
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation of Java does not include the SHA256withDSA algorithm
	 */
	public static SignatureAlgorithm sha256dsa() throws NoSuchAlgorithmException {
		return new SHA256DSA();
	}

	/**
	 * Yields the ed25519 signature algorithm.
	 * 
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the ed25519 algorithm
	 */
	public static SignatureAlgorithm ed25519() throws NoSuchAlgorithmException {
		return new ED25519();
	}

	/**
	 * Yields a signature algorithm that uses the ed25519 cryptography. It generates
	 * keys in a deterministic order, hence must NOT be used in production.
	 * It is useful instead for testing, since it makes deterministic the
	 * sequence of keys of the accounts in the tests and consequently
	 * also the gas costs of such accounts when they are put into maps, for instance.
	 * 
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the ed25519 algorithm
	 */
	public static SignatureAlgorithm ed25519det() throws NoSuchAlgorithmException {
		return new ED25519DET();
	}

	/**
	 * Yields the qTESLA-p-I signature algorithm.
	 *
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the qTESLA-p-I algorithm
	 */
	public static SignatureAlgorithm qtesla1() throws NoSuchAlgorithmException {
		return new QTESLA1();
	}

	/**
	 * Yields the qTESLA-p-III signature algorithm.
	 *
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the qTESLA-p-III algorithm
	 */
	public static SignatureAlgorithm qtesla3() throws NoSuchAlgorithmException {
		return new QTESLA3();
	}

	/**
	 * Yields an empty signature algorithm that signs everything with an empty array of bytes.
	 * 
	 * @return the algorithm
	 */
	public static SignatureAlgorithm empty() {
		return new EMPTY();
	}

	/**
	 * Yields the signature algorithm with the given name.
	 * It looks for a factory method with the given name and invokes it.
	 * 
	 * @param name the name of the algorithm, case-insensitive
	 * @return the algorithm
	 * @throws NoSuchAlgorithmException if the installation does not include the given algorithm
	 */
	public static SignatureAlgorithm of(String name) throws NoSuchAlgorithmException {
		return AbstractSignatureAlgorithmImpl.of(name);
	}
}