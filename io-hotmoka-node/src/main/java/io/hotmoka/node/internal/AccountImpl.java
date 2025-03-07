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

package io.hotmoka.node.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.util.function.Function;

import io.hotmoka.crypto.AbstractAccount;
import io.hotmoka.crypto.api.Entropy;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionReferences;
import io.hotmoka.node.api.Account;
import io.hotmoka.node.api.values.StorageReference;

/**
 * The information to control an account of a Hotmoka node.
 * One needs the entropy from which the key pair can be reconstructed and
 * the storage reference of the account in the store of the node.
 */
public class AccountImpl extends AbstractAccount<StorageReference> implements Account {

	/**
	 * Creates the information to control an account.
	 * 
	 * @param entropy the entropy, from which the key pair can be derived
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 */
	public AccountImpl(Entropy entropy, StorageReference reference) {
		super(entropy, reference);

		if (reference.getProgressive().signum() != 0)
			throw new IllegalArgumentException("Accounts are limited to have 0 as progressive index");
	}

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @throws IOException if the PEM file cannot be read
	 */
	public AccountImpl(StorageReference reference) throws IOException {
		super(reference);

		if (reference.getProgressive().signum() != 0)
			throw new IllegalArgumentException("Accounts are limited to have 0 as progressive index");
	}

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param reference the reference to the account. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @param dir the directory where the PEM file must be looked for
	 * @throws IOException if the PEM file cannot be read
	 */
	public AccountImpl(StorageReference reference, String dir) throws IOException {
		super(reference, dir);

		if (reference.getProgressive().signum() != 0)
			throw new IllegalArgumentException("Accounts are limited to have 0 as progressive index");
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
	 * @throws IOException if the PEM file cannot be read
	 * @throws E if {@code reference} is illegal as a storage reference or it is not legal for an account
	 */
	public <E extends Exception> AccountImpl(String reference, Function<String, ? extends E> onIllegalReference) throws IOException, E {
		this(StorageValues.reference(reference, onIllegalReference));
	}

	/**
	 * Creates the information to control an account in a Hotmoka node.
	 * The entropy of the account is recovered from its PEM file.
	 * 
	 * @param <E> the type of the exception thrown if {@code reference} is illegal as a storage reference
	 *            or it is not legal for an account
	 * @param reference the reference to the account, as a string. This is limited to have 0 as progressive,
	 *                  in order to reduce the information needed to represent an account as BIP39 words
	 * @param dir the directory where the PEM file must be looked for
	 * @param onIllegalReference the creator of the exception thrown if {@code reference} is illegal
	 *                           as a storage reference or it is not legal for an account
	 * @throws IOException if the PEM file cannot be read
	 * @throws E if {@code reference} is illegal as a storage reference or it is not legal for an account
	 */
	public <E extends Exception> AccountImpl(String reference, String dir, Function<String, ? extends E> onIllegalReference) throws IOException, E {
		this(StorageValues.reference(reference, onIllegalReference), dir);
	}

	/**
	 * Creates the information to control an account in a Hotmoka node,
	 * from its entropy and from the byte representation of its storage reference.
	 * 
	 * @param entropy the entropy
	 * @param reference the byte representation of the reference
	 */
	public AccountImpl(Entropy entropy, byte[] reference) {
		// TODO: exception should be checked
		this(entropy, StorageValues.reference(TransactionReferences.of(reference, IllegalArgumentException::new), BigInteger.ZERO, IllegalArgumentException::new));
	}

    @Override
    public int compareTo(Entropy other) {
    	int diff = super.compareTo(other);
    	if (diff != 0)
    		return diff;
    	else
    		return reference.compareTo(((AccountImpl) other).reference);
    }

	@Override
	public byte[] getReferenceAsBytes() {
		return reference.getTransaction().getHash();
	}
}