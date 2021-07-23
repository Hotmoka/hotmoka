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

import io.takamaka.code.lang.ExternallyOwnedAccountED25519;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * An object that can be used to store and retrieve accounts from their public key.
 * It can be used in order to request somebody to create, and possibly fund,
 * an account on our behalf, and store it in this ledger for public evidence.
 */
public class AccountsLedger extends Storage {
	
	/**
	 * The accounts in this ledger, mapped from their public key.
	 */
	private final StorageMap<String, ExternallyOwnedAccountED25519> accounts = new StorageTreeMap<>();

	/**
	 * Yields the account in this ledger, for the given public key.
	 * 
	 * @param publicKey the Base64-encoded public key of the account
	 * @return the account, if any. Yields {@code null} otherwise
	 */
	public @View ExternallyOwnedAccountED25519 get(String publicKey) {
		return accounts.get(publicKey);
	}

	/**
	 * Adds the given account to this ledger, mapped from its public key.
	 * 
	 * @param account the account to put in this ledger
	 */
	public void put(ExternallyOwnedAccountED25519 account) {
		accounts.putIfAbsent(account.publicKey(), account);
	}
}