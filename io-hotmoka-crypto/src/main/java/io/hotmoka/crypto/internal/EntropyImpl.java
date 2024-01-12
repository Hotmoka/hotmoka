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

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.crypto.api.SignatureAlgorithm;

/**
 * The entropy information from which an account can be derived.
 * A key pair can be reconstructed from this entropy, given the password
 * associated to the account.
 */
public class EntropyImpl implements Entropy	 {

	/**
	 * The entropy, 16 bytes.
	 */
	private final byte[] entropy;

	/**
	 * Creates new, random entropy information.
	 * 
	 * @param random the object that is used to generate random entropy
	 */
	public EntropyImpl(SecureRandom random) {
		entropy = new byte[16];
		random.nextBytes(entropy);
	}

	/**
	 * Creates new, random entropy information, by using a {@link java.security.SecureRandom}.
	 */
	public EntropyImpl() {
		this(new SecureRandom());
	}

	/**
	 * Reads the entropy information from a PEM file.
	 * 
	 * @param filePrefix the name of the PEM file, without the trailing {@code .pem}
	 * @throws IOException if the PEM file cannot be read
	 */
	public EntropyImpl(String filePrefix) throws IOException {
		this(Paths.get(filePrefix + ".pem"));
	}

	/**
	 * Reads the entropy information from a PEM file.
	 * 
	 * @param path the file
	 * @throws IOException if the PEM file cannot be read
	 */
	public EntropyImpl(Path path) throws IOException {
		long length = path.toFile().length();
		// without this check, the access to the file would take very long and terminate with an error anyway
		if (length > 10000L)
			throw new IOException("The pem file " + path + " is too long for being a PEM file!");

		try (var reader = new PemReader(new FileReader(path.toFile()))) {
			entropy = reader.readPemObject().getContent();
		}

		if (entropy.length != 16)
			throw new IOException("Illegal entropy length: 16 bytes expected");
	}

	/**
	 * Copy constructor.
	 * 
	 * @param parent the entropy to clone
	 */
	public EntropyImpl(Entropy parent) {
		this.entropy = parent.getEntropyAsBytes();
	}

	/**
	 * Creates entropy corresponding to the given bytes.
	 * 
	 * @param entropy the 16 bytes of entropy
	 */
	public EntropyImpl(byte[] entropy) {
		if (entropy.length != 16)
			throw new IllegalArgumentException("Illegal entropy length: 16 bytes expected");

		this.entropy = entropy.clone();
	}

	@Override
	public byte[] getEntropyAsBytes() {
		return entropy.clone();
	}

	@Override
	public int length() {
		return entropy.length;
	}

	@Override
	public String toString() {
		return Hex.toHexString(entropy);
	}

	@Override
	public void dump(Path path) throws IOException {
		try (var pemWriter = new PemWriter(new OutputStreamWriter(Files.newOutputStream(path)))) {
			pemWriter.writeObject(new PemObject("ENTROPY", entropy));
		}
	}

	@Override
	public KeyPair keys(String password, SignatureAlgorithm algorithm) {
		return algorithm.getKeyPair(entropy, password);
	}

	@Override
	public int compareTo(Entropy other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else
			return compareBytes(entropy, other.getEntropyAsBytes());
	}

	/**
	 * Compares two arrays of bytes, lexicographically.
	 * 
	 * @param bytes1 the first array of bytes
	 * @param bytes2 the second array of bytes
	 * @return the result of the comparison
	 */
	private static int compareBytes(byte[] bytes1, byte[] bytes2) {
		// the following currently does not work under Android
		// return Arrays.compare(entropy, other.entropy);
		int diff = bytes1.length - bytes2.length;
		if (diff != 0)
			return diff;

		for (int pos = 0; pos < bytes1.length; pos++) {
			diff = bytes1[pos] - bytes2[pos];
			if (diff != 0)
				return diff;
		}

		return 0;
	}

	@Override
	public boolean equals(Object other) {
		return other.getClass() == getClass() && Arrays.equals(entropy, ((EntropyImpl) other).entropy);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(entropy);
	}
}