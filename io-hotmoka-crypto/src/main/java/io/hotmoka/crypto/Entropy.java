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

import java.io.IOException;
import java.security.SecureRandom;

import io.hotmoka.crypto.internal.EntropyImpl;

/**
 * The entropy information from which an account can be derived.
 * A key pair can be reconstructed from this entropy, given the password
 * associated to the account.
 */
public interface Entropy extends io.hotmoka.crypto.api.Entropy {

	/**
	 * Yields new, random entropy information.
	 * 
	 * @param random the object that is used to generate random entropy
	 * @return the entropy
	 */
	static Entropy of(SecureRandom random) {
		return new EntropyImpl(random);
	}

	/**
	 * Yields new, random entropy information, by using a {@link java.security.SecureRandom}.
	 * 
	 * @return the entropy
	 */
	static Entropy of() {
		return new EntropyImpl();
	};

	/**
	 * Yields new entropy information read from a PEM file.
	 * 
	 * @param filePrefix the name of the PEM file, without the trailing <b>.pem</b>
	 * @return the entropy
	 * @throws IOException if the PEM file cannot be read
	 */
	static Entropy of(String filePrefix) throws IOException {
		return new EntropyImpl(filePrefix);
	}

	/**
	 * Yields a copy of the given entropy.
	 * 
	 * @param parent the entropy to clone
	 * @return the copy
	 */
	static Entropy of(io.hotmoka.crypto.api.Entropy parent) {
		return new EntropyImpl(parent);
	}

	/**
	 * Yields new entropy corresponding to the given bytes.
	 * 
	 * @param entropy the 16 bytes of entropy
	 * @return the entropy
	 */
	static Entropy of(byte[] entropy) {
		return new EntropyImpl(entropy);
	}
}