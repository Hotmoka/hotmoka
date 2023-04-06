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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import io.hotmoka.crypto.internal.BIP39WordsImpl;

/**
 * The information to control an account.
 * One needs the entropy from which the key pair can be reconstructed and
 * potentially also a reference that might not be derived from the key,
 * such as, in Hotmoka, the address of the account in the store of the node.
 * 
 * @param <R> the type of reference that identifies the account
 */
public abstract class Account<R extends Comparable<? super R>> extends Entropy implements io.hotmoka.crypto.api.Account<R> {

	/**
	 * The reference of the account.
	 */
	public final R reference;

	/**
	 * Creates the information to control an account.
	 * 
	 * @param entropy the entropy, from which the key pair can be derived
	 * @param reference the reference to the account
	 */
	protected Account(io.hotmoka.crypto.api.Entropy entropy, R reference) {
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
	protected Account(R reference) throws IOException {
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
	protected Account(R reference, String dir) throws IOException {
		super(dir + File.separatorChar + reference.toString());

		this.reference = reference;
	}

	@Override
	public String toString() {
		return reference.toString();
	}

	@Override
	public String dump(Path where) throws IOException {
		return super.dump(where, toString());
	}

	@Override
	public String dump() throws IOException {
		return dump(toString());
	}

	@Override
	public void delete() throws IOException {
		delete(toString());
	}

	@Override
    public BIP39Words bip39Words(io.hotmoka.crypto.api.BIP39Dictionary dictionary) {
    	return new BIP39WordsImpl(this, dictionary);
    }

	@Override
    public BIP39Words bip39Words() {
    	return new BIP39WordsImpl(this, BIP39Dictionary.ENGLISH_DICTIONARY);
    }

    @Override
    public boolean equals(Object other) {
    	return super.equals(other) && reference.equals(((Account<?>) other).reference);
    }

    @Override
    public int hashCode() {
    	return super.hashCode() ^ reference.hashCode();
    }

    /**
     * Yields a byte representation of the reference of this account.
     * 
     * @return the byte representation
     */
    public abstract byte[] getReferenceAsBytes();
}