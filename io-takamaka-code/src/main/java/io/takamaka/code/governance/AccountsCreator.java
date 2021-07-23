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

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * A contract that can be used to create accounts and keep track of them
 * from their public key. It can be used in order to request somebody to create,
 * and possibly fund, an account on our behalf, given the public key for the account.
 * Later, we can recover the account from that public key.
 */
public class AccountsCreator extends Contract {
	
	/**
	 * The accounts that have been created with this object, mapped from their public key.
	 */
	private final StorageMap<String, ExternallyOwnedAccount> accounts = new StorageTreeMap<>();

	/**
	 * Yields the account created with this object, for the given public key.
	 * 
	 * @param publicKey the Base64-encoded public key of the account
	 * @return the account, if any. Yields {@code null} otherwise
	 */
	public ExternallyOwnedAccount get(String publicKey) {
		return accounts.get(publicKey);
	}

	/**
	 * Creates an externally owned account with the given public key. This key
	 * must be for the default signature algorithm of the node.
	 * 
	 * @param publicKey the public key
	 * @return the account; if it already existed, yield that existing account
	 */
	public ExternallyOwnedAccount create(String publicKey) {
		return accounts.computeIfAbsent(publicKey, ExternallyOwnedAccount::new);
	}

	/**
	 * Creates and/or fund an externally owned account with the given public key. This key
	 * must be for the default signature algorithm of the node. If an account already existed
	 * for the given public key, it funds it and returns it.
	 * 
	 * @param amount the initial funds for the new account
	 * @param publicKey the public key
	 * @return the account. If it already existed, funds if with extra {@code amount} coins and yields that existing account
	 */
	public @FromContract @Payable ExternallyOwnedAccount create(BigInteger amount, String publicKey) {
		ExternallyOwnedAccount existing = accounts.get(publicKey);
		if (existing != null) {
			existing.receive(amount);
			return existing;
		}
		else {
			ExternallyOwnedAccount account = new ExternallyOwnedAccount(amount, publicKey);
			accounts.put(publicKey, account);
			return account;
		}
	}
}