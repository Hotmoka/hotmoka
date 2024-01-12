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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.hotmoka.crypto.api.Account;
import io.hotmoka.crypto.api.BIP39Dictionary;
import io.hotmoka.crypto.api.BIP39Mnemonic;
import io.hotmoka.crypto.api.Entropy;

/**
 * Partial implementation of the information to control an account.
 * One needs the entropy from which the key pair can be reconstructed and
 * potentially also a reference that might not be derived from the key,
 * such as, in Hotmoka, the address of the account in the store of the node.
 * 
 * @param <R> the type of reference that identifies the account
 */
public abstract class AbstractAccountImpl<R extends Comparable<? super R>> extends EntropyImpl implements Account<R> {

	/**
	 * The reference of the account.
	 */
	protected final R reference;

	/**
	 * Creates the information to control an account.
	 * 
	 * @param entropy the entropy, from which the key pair can be derived
	 * @param reference the reference to the account
	 */
	protected AbstractAccountImpl(Entropy entropy, R reference) {
		super(entropy);

		this.reference = reference;
	}

	/**
	 * Creates the information to control an account.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account
	 * @throws IOException if the PEM file cannot be read
	 */
	protected AbstractAccountImpl(R reference) throws IOException {
		super(reference.toString());

		this.reference = reference;
	}

	/**
	 * Creates the information to control an account.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account
	 * @param dir the directory where the PEM file must be looked for
	 * @throws IOException if the PEM file cannot be read
	 */
	protected AbstractAccountImpl(R reference, String dir) throws IOException {
		super(dir + File.separatorChar + reference.toString());

		this.reference = reference;
	}

	@Override
	public R getReference() {
		return reference;
	}

	@Override
	public String toString() {
		return reference.toString();
	}

	@Override
	public Path dump() throws IOException {
		var path = Paths.get(this + ".pem");
		dump(path);
		return path;
	}

	@Override
	public void delete() throws IOException {
		Files.delete(Paths.get(this + ".pem"));
	}

	@Override
    public BIP39Mnemonic bip39Words(BIP39Dictionary dictionary) {
    	return new BIP39MnemonicImpl(this, dictionary);
    }

	@Override
    public BIP39Mnemonic bip39Words() {
    	return new BIP39MnemonicImpl(this, io.hotmoka.crypto.BIP39Dictionaries.ENGLISH_DICTIONARY);
    }

    @Override
    public boolean equals(Object other) {
    	return super.equals(other) && reference.equals(((AbstractAccountImpl<?>) other).reference);
    }

    @Override
    public int hashCode() {
    	return super.hashCode() ^ reference.hashCode();
    }
}