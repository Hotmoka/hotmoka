/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.node;

import java.io.IOException;

import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.internal.AccountImpl;

/**
 * Providers of accounts.
 */
public final class Accounts {

	private Accounts() {}

	/**
	 * Yields an account in a Hotmoka node.
	 * 
	 * @param entropy the entropy, from which the key pair can be derived
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @return the account
	 */
	public static Account of(Entropy entropy, StorageReference reference) {
		return new AccountImpl(entropy, reference);
	}

	/**
	 * Yields an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @return the account
	 * @throws IOException if the PEM file cannot be read
	 */
	public static Account of(StorageReference reference) throws IOException {
		return new AccountImpl(reference);
	}

	/**
	 * Yields an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @param dir the directory where the PEM file must be looked for
	 * @return the account
	 * @throws IOException if the PEM file cannot be read
	 */
	public static Account of(StorageReference reference, String dir) throws IOException {
		return new AccountImpl(reference, dir);
	}

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param <E> the type of the exception thrown if {@code reference} is illegal as a storage reference
	 *            or it is not legal for an account
	 * @param reference the reference to the account, as a string. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @param onIllegalReference the creator of the exception thrown if {@code reference} is illegal
	 *                           as a storage reference or it is not legal for an account
	 * @return the account
	 * @throws IOException if the PEM file cannot be read
	 * @throws E if {@code reference} is illegal as a storage reference or it is not legal for an account
	 */
	public static <E extends Exception> Account of(String reference, ExceptionSupplier<? extends E> onIllegalReference) throws IOException, E {
		return new AccountImpl(reference, onIllegalReference);
	}

	/**
	 * Yields the information to control an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param <E> the type of the exception thrown if {@code reference} is illegal as a storage reference
	 *            or it is not legal for an account
	 * @param reference the reference to the account, as a string. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @param dir the directory where the PEM file must be looked for
	 * @param onIllegalReference the creator of the exception thrown if {@code reference} is illegal
	 *                           as a storage reference or it is not legal for an account
	 * @return the account
	 * @throws IOException if the PEM file cannot be read
	 * @throws E if {@code reference} is illegal as a storage reference or it is not legal for an account
	 */
	public static <E extends Exception> Account of(String reference, String dir, ExceptionSupplier<? extends E> onIllegalReference) throws IOException, E {
		return new AccountImpl(reference, dir, onIllegalReference);
	}

	/**
	 * Yields an account in a Hotmoka node,
	 * from its entropy and from the byte representation of its storage reference.
	 * 
	 * @param entropy the entropy
	 * @param reference the byte representation of the reference
	 * @return the account
	 */
	public static Account of(Entropy entropy, byte[] reference) {
		return new AccountImpl(entropy, reference);
	}
}