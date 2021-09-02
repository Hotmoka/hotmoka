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

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * The entropy information from which an account can be derived.
 * A key pair can be reconstructed from this entropy, given the password
 * associated to the account.
 */
public class Entropy implements Comparable<Entropy> {

	/**
	 * The entropy, 16 bytes.
	 */
	private final byte[] entropy;

	/**
	 * Creates new, random entropy information.
	 * 
	 * @param random the object that is used to generate random entropy
	 */
	public Entropy(SecureRandom random) {
		entropy = new byte[16];
		random.nextBytes(entropy);
	}

	/**
	 * Creates new, random entropy information, by using a {@link java.security.SecureRandom}.
	 */
	public Entropy() {
		this(new SecureRandom());
	}

	/**
	 * Reads the entropy information from a PEM file.
	 * 
	 * @param filePrefix the name of the PEM file, without the trailing <b>.pem</b>
	 * @throws IOException if the PEM file cannot be read
	 */
	public Entropy(String filePrefix) throws IOException {
		try (PemReader reader = new PemReader(new FileReader(filePrefix + ".pem"))) {
			entropy = reader.readPemObject().getContent();
		}

		if (entropy.length != 16)
			throw new IllegalArgumentException("illegal entropy length: 16 bytes expected");
	}

	/**
	 * Copy constructor.
	 * 
	 * @param entropy the entropy to clone
	 */
	public Entropy(Entropy parent) {
		this.entropy = parent.entropy;
	}

	/**
	 * Creates entropy corresponding to the given bytes.
	 * 
	 * @param entropy the 16 bytes of entropy
	 */
	public Entropy(byte[] entropy) {
		this.entropy = entropy.clone();

		if (entropy.length != 16)
			throw new IllegalArgumentException("illegal entropy length: 16 bytes expected");
	}

	/**
	 * Yields the entropy inside this object.
	 * 
	 * @return the entropy (16 bytes)
	 */
	public byte[] getEntropy() {
		return entropy.clone();
	}

	/**
	 * Yields the length of the entropy byte array.
	 * 
	 * @return the length (currently always 16)
	 */
	public int length() {
		return entropy.length;
	}

	@Override
	public String toString() {
		return Hex.toHexString(entropy);
	}

	/**
	 * Dumps this entropy into a PEM file.
	 * 
	 * @param filePrefix the name of the PEM file, without the trailing {@code .pem}
	 * @return the full name of the PEM file ({@code filePrefix} followed by {@code .pem})
	 * @throws IOException if the PEM file cannot be created
	 */
	public String dump(String filePrefix) throws IOException {
		PemObject pemObject = new PemObject("ENTROPY", entropy);
		String fileName = filePrefix + ".pem";

		try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(fileName)))) {
			pemWriter.writeObject(pemObject);
		}

		return fileName;
	}

	/**
	 * Constructs the key pair of this entropy, from the given password.
	 * 
	 * @param password the password
	 * @param algorithm the signature algorithm for the keys
	 * @return the key pair
	 */
	public KeyPair keys(String password, SignatureAlgorithm<?> algorithm) {
		return algorithm.getKeyPair(entropy, password);
	}

	@Override
	public int compareTo(Entropy other) {
		int diff = getClass().getName().compareTo(other.getClass().getName());
		if (diff != 0)
			return diff;
		else {
			// the following currently does not work under Android
			// return Arrays.compare(entropy, other.entropy);
			diff = entropy.length - other.entropy.length;
			if (diff != 0)
				return diff;

			for (int pos = 0; pos < entropy.length; pos++) {
				diff = entropy[pos] - other.entropy[pos];
				if (diff != 0)
					return diff;
			}

			return 0;
		}
	}

	@Override
	public boolean equals(Object other) {
		return other.getClass() == getClass() && Arrays.equals(entropy, ((Entropy) other).entropy);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(entropy);
	}
}