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
import java.nio.file.Path;
import java.security.SecureRandom;

import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.internal.EntropyImpl;

/**
 * Provider of entropy information from which an account can be derived.
 */
public final class Entropies {

	private Entropies() {}

	/**
	 * Yields new, random entropy information.
	 * 
	 * @param random the object that is used to generate random entropy
	 * @return the entropy
	 */
	public static Entropy random(SecureRandom random) {
		return new EntropyImpl(random);
	}

	/**
	 * Yields new, random entropy information, by using a {@link java.security.SecureRandom}.
	 * 
	 * @return the entropy
	 */
	public static Entropy random() {
		return new EntropyImpl();
	};

	/**
	 * Yields new entropy information read from a PEM file.
	 * 
	 * @param path the file
	 * @return the entropy
	 * @throws IOException if the PEM file cannot be read
	 */
	public static Entropy load(Path path) throws IOException {
		return new EntropyImpl(path);
	}

	/**
	 * Yields new entropy information read from a PEM file.
	 * 
	 * @param filePrefix the name of the file, without the trailing {@code .pem}
	 * @return the entropy
	 * @throws IOException if the PEM file cannot be read
	 */
	public static Entropy load(String filePrefix) throws IOException {
		return new EntropyImpl(filePrefix);
	}

	/**
	 * Yields a copy of the given entropy.
	 * 
	 * @param parent the entropy to clone
	 * @return the copy
	 */
	public static Entropy copy(Entropy parent) {
		return new EntropyImpl(parent);
	}

	/**
	 * Yields new entropy corresponding to the given bytes.
	 * 
	 * @param entropy the 16 bytes of entropy
	 * @return the entropy
	 */
	public static Entropy of(byte[] entropy) {
		return new EntropyImpl(entropy);
	}
}