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

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.internal.BIP39WordsImpl;

/**
 * The information to control an account in a Hotmoka node.
 * One needs the entropy from which the key pair can be reconstructed and
 * the address of the account in the store of the node.
 */
public class Account {

	/**
	 * The entropy.
	 */
	private final byte[] entropy;

	/**
	 * The reference to the account. This is limited to have 0 as progressive, in order to reduce
	 * the information needed to represent an account as BIP39 words.
	 */
	public final StorageReference reference;

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * 
	 * @param entropy the entropy, from which the key pair can be derived
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 */
	public Account(byte[] entropy, StorageReference reference) {
		this.entropy = entropy.clone();
		this.reference = reference;

		if (reference.progressive.signum() != 0)
			throw new IllegalArgumentException("accounts are limited to have 0 as progressive index");
	}

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @throws IOException if the PEM file cannot be read
	 */
	public Account(StorageReference reference) throws IOException {
		if (reference.progressive.signum() != 0)
			throw new IllegalArgumentException("accounts are limited to have 0 as progressive index");

		try (PemReader reader = new PemReader(new FileReader(reference + ".pem"))) {
			entropy = reader.readPemObject().getContent();
		}

		this.reference = reference;
	}

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account, as a string. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @throws IOException if the PEM file cannot be read
	 */
	public Account(String reference) throws IOException {
		this(new StorageReference(reference));
	}

	/**
	 * Yields the entropy of this account, from which its key pair can be reconstructed.
	 * 
	 * @return the entropy
	 */
	public byte[] getEntropy() {
		return entropy.clone();
	}

	@Override
	public String toString() {
		return reference.toString();
	}

	/**
	 * Dumps the entropy of this account into a PEM file.
	 * 
	 * @throws IOException if the PEM file cannot be created
	 */
	public void dump() throws IOException {
		PemObject pemObject = new PemObject("ENTROPY", entropy);

		try (PemWriter pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(reference.toString() + ".pem")))) {
			pemWriter.writeObject(pemObject);
		}
	}

	/**
     * Yields the BIP39 words for this account using the given dictionary.
     * They can be later transformed back into this account by calling the
     * {@link BIP39Words#toAccount()} method.
     * 
     * @param dictionary the dictionary
     */
    public BIP39Words bip39Words(BIP39Dictionary dictionary) {
    	return new BIP39WordsImpl(this, dictionary);
    }

    /**
     * Yields the BIP39 words for this account using the English dictionary.
     * They can be later transformed back into this account by calling the
     * {@link BIP39Words#toAccount()} method.
     */
    public BIP39Words bip39Words() {
    	return bip39Words(BIP39Dictionary.ENGLISH_DICTIONARY);
    }

	/**
	 * Reconstructs the key pair of this account, from its entropy and the password.
	 * 
	 * @param password the password of the account
	 * @param algorithm the signature algorithm of the account
	 * @return the key pair
	 */
	public KeyPair keys(String password, SignatureAlgorithm<SignedTransactionRequest> algorithm) {
		return algorithm.getKeyPair(entropy, BIP39Dictionary.ENGLISH_DICTIONARY, password);
	}
}