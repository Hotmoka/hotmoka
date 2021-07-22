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

package io.takamaka.code.governance;

import static io.takamaka.code.lang.Takamaka.require;

import java.util.function.Function;

import io.takamaka.code.lang.Account;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.ExternallyOwnedAccountED25519;
import io.takamaka.code.lang.ExternallyOwnedAccountQTESLA1;
import io.takamaka.code.lang.ExternallyOwnedAccountQTESLA3;
import io.takamaka.code.lang.ExternallyOwnedAccountSHA256DSA;
import io.takamaka.code.lang.RequirementViolationException;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * An object that can be used to create accounts and keep track of them
 * from their public key. It can be used in order to request somebody to create
 * an account on our behalf, given the public key for the account. Later,
 * we can recover the account from that public key.
 */
public class AccountsCreator extends Storage {
	
	/**
	 * The accounts that have been created with this object, from their public key.
	 */
	private final StorageMap<String, Account> accounts = new StorageTreeMap<>();

	/**
	 * Yields the account created with this object, for the given public key.
	 * 
	 * @param publicKey the Base64-encoded public key of the account
	 * @return the account, if any. Yields {@code null} otherwise
	 */
	public Account get(String publicKey) {
		return accounts.get(publicKey);
	}

	/**
	 * Creates an externally owned account with the given public key. This key
	 * must be for the default signature algorithm of the node.
	 * 
	 * @param publicKey the public key
	 * @return the account
	 * @throws RequirementViolationException if an account with the given public key
	 *                                       has already been created with this object
	 */
	public Account create(String publicKey) {
		return create(publicKey, ExternallyOwnedAccount::new);
	}

	/**
	 * Creates an externally owned account with the given public key. This key
	 * must be for the ED25519 signature algorithm.
	 * 
	 * @param publicKey the public key
	 * @return the account
	 * @throws RequirementViolationException if an account with the given public key
	 *                                       has already been created with this object
	 */
	public Account createED25519(String publicKey) {
		return create(publicKey, ExternallyOwnedAccountED25519::new);
	}

	/**
	 * Creates an externally owned account with the given public key. This key
	 * must be for the SHA256DSA signature algorithm.
	 * 
	 * @param publicKey the public key
	 * @return the account
	 * @throws RequirementViolationException if an account with the given public key
	 *                                       has already been created with this object
	 */
	public Account createSHA256DSA(String publicKey) {
		return create(publicKey, ExternallyOwnedAccountSHA256DSA::new);
	}

	/**
	 * Creates an externally owned account with the given public key. This key
	 * must be for the qTESLA-p-I signature algorithm.
	 * 
	 * @param publicKey the public key
	 * @return the account
	 * @throws RequirementViolationException if an account with the given public key
	 *                                       has already been created with this object
	 */
	public Account createQTESLA1(String publicKey) {
		return create(publicKey, ExternallyOwnedAccountQTESLA1::new);
	}

	/**
	 * Creates an externally owned account with the given public key. This key
	 * must be for the qTESLA-p-III signature algorithm.
	 * 
	 * @param publicKey the public key
	 * @return the account
	 * @throws RequirementViolationException if an account with the given public key
	 *                                       has already been created with this object
	 */
	public Account createQTESLA3(String publicKey) {
		return create(publicKey, ExternallyOwnedAccountQTESLA3::new);
	}

	private Account create(String publicKey, Function<String, Account> creator) {
		require(!accounts.containsKey(publicKey), "an account with this public key has been already created");
		Account account = creator.apply(publicKey);
		accounts.put(publicKey, account);
		return account;
	}
}