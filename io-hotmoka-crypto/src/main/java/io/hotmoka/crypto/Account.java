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

import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.crypto.internal.BIP39WordsImpl;

/**
 * The information to control an account in a Hotmoka node.
 * One needs the entropy from which the key pair can be reconstructed and
 * the address of the account in the store of the node.
 */
public class Account extends Entropy {

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
	public Account(Entropy entropy, StorageReference reference) {
		super(entropy);

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
		super(reference.toString());

		if (reference.progressive.signum() != 0)
			throw new IllegalArgumentException("accounts are limited to have 0 as progressive index");

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

	@Override
	public String toString() {
		return reference.toString();
	}

	/**
	 * Dumps the entropy of this account into a PEM file with the name of the reference of this account.
	 * 
	 * @return the full name of the PEM file (name of the reference of this account followed by {@code .pem})
	 * @throws IOException if the PEM file cannot be created
	 */
	public String dump() throws IOException {
		return super.dump(reference.toString());
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
    	return new BIP39WordsImpl(this, BIP39Dictionary.ENGLISH_DICTIONARY);
    }

    @Override
    public int compareTo(Entropy other) {
    	int diff = super.compareTo(other);
    	if (diff != 0)
    		return diff;
    	else
    		return reference.compareTo(((Account) other).reference);
    }

    @Override
    public boolean equals(Object other) {
    	return super.equals(other) && reference.equals(((Account) other).reference);
    }

    @Override
    public int hashCode() {
    	return super.hashCode() ^ reference.hashCode();
    }
}