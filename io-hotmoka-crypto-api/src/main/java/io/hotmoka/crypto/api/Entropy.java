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

package io.hotmoka.crypto.api;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;

/**
 * The entropy information from which an account can be derived.
 * A key pair can be reconstructed from this entropy, given the password
 * associated to the account.
 */
public interface Entropy extends Comparable<Entropy> {

	/**
	 * Yields the entropy as bytes.
	 * 
	 * @return the entropy (16 bytes)
	 */
	byte[] getEntropy();

	/**
	 * Yields the length of the entropy byte array.
	 * 
	 * @return the length (currently always 16)
	 */
	int length();

	/**
	 * Dumps this entropy into a PEM file.
	 * 
	 * @param where the directory where the file must be dumped
	 * @param filePrefix the name of the PEM file, without the trailing {@code .pem}
	 * @return the full path of the PEM file ({@code filePrefix} followed by {@code .pem})
	 * @throws IOException if the PEM file cannot be created
	 */
	Path dump(Path where, String filePrefix) throws IOException;

	/**
	 * Dumps this entropy into a PEM file in the current directory.
	 * 
	 * @param filePrefix the name of the PEM file, without the trailing {@code .pem}
	 * @return the full path of the PEM file ({@code filePrefix} followed by {@code .pem})
	 * @throws IOException if the PEM file cannot be created
	 */
	Path dump(String filePrefix) throws IOException;

	/**
	 * Deletes the PEM file in the current directory.
	 * 
	 * @param filePrefix the name of the PEM file, without the trailing {@code .pem}
	 * @throws IOException if the PEM file cannot be deleted
	 */
	void delete(String filePrefix) throws IOException;

	/**
	 * Constructs the key pair of this entropy, from the given password.
	 * 
	 * @param password the password
	 * @param algorithm the signature algorithm for the keys
	 * @return the key pair
	 */
	KeyPair keys(String password, SignatureAlgorithm algorithm);
}