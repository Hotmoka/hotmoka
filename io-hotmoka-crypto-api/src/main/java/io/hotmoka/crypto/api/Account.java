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

/**
 * The information to control an account.
 * One needs the entropy from which the key pair can be reconstructed and
 * potentially also a reference that might not be derived from the key,
 * such as, in Hotmoka, the address of the account in the store of the node.
 * 
 * @param <R> the type of reference that identifies the account
 */
public interface Account<R extends Comparable<? super R>> extends Entropy {

	/**
	 * Yields the reference of the account.
	 * 
	 * @return the reference
	 */
	R getReference();

	/**
	 * Dumps the entropy of this account into a PEM file, in the current directory,
	 * with, as name, the reference of this account, followed by {@code .pem}.
	 * 
	 * @return the full path of the PEM file (name of the reference of this account followed by {@code .pem})
	 * @throws IOException if the PEM file cannot be created
	 */
	Path dump() throws IOException;

	/**
	 * Removes the PEM file, in the current directory,
	 * with the name of the reference of this account, followed by {@code .pem}, if it exists.
	 * 
	 * @throws IOException if the PEM file cannot be deleted
	 */
	void delete() throws IOException;

	/**
     * Yields the BIP39 words for this account using the given dictionary.
     * They can be later transformed back into this account by calling the
     * {@link BIP39Mnemonic#toAccount(java.util.function.BiFunction)} method.
     * 
     * @param dictionary the dictionary
     * @return the words
     */
	BIP39Mnemonic bip39Words(BIP39Dictionary dictionary);

    /**
     * Yields the BIP39 words for this account using the English dictionary.
     * They can be later transformed back into this account by calling the
     * {@link BIP39Mnemonic#toAccount(java.util.function.BiFunction)} method.
     * 
     * @return the BIP39 words for this account
     */
	BIP39Mnemonic bip39Words();

    /**
     * Yields a byte representation of the reference of this account.
     * 
     * @return the byte representation
     */
	byte[] getReferenceAsBytes();
}