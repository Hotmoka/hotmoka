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
import io.takamaka.code.lang.Exported;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.ExternallyOwnedAccountED25519;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Gamete;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageMap;
import io.takamaka.code.util.StorageTreeMap;

/**
 * An object that can be used to store and retrieve accounts from their public key.
 * It can be used in order to request somebody to create, and possibly fund,
 * an account on our behalf, and store it in this ledger for public evidence.
 * The gamete is assumed to be always in the accounts ledger.
 */
@Exported
public class AccountsLedger extends Contract {

	/**
	 * The manifest of the node having this accounts ledger.
	 */
	private final Gamete gamete;

	/**
	 * Builds an accounts ledger for a node having the given manifest.
	 * 
	 * @param manifest the manifest of the node
	 */
	AccountsLedger(Manifest<?> manifest) {
		this.gamete = manifest.getGamete();
	}

	/**
	 * The accounts in this ledger, mapped from their Base64-encoded public key.
	 */
	private final StorageMap<String, ExternallyOwnedAccount> accounts = new StorageTreeMap<>();

	/**
	 * Yields the account in this ledger, for the given public key.
	 * 
	 * @param publicKey the Base64-encoded public key of the gamete or the Base64-encoded
	 *                  ed25519 public key of the account
	 * @return the account, if any. Yields {@code null} otherwise
	 */
	public @View ExternallyOwnedAccount get(String publicKey) {
		if (gamete.publicKey().equals(publicKey))
			return gamete;
		else
			return accounts.get(publicKey);
	}

	/**
	 * Adds to this ledger an account for the given public key, if it does not exist already.
	 * Then sends {@code amount} coins to that account (old or new).
	 * This method is special since, if its caller is the gamete of the node and
	 * if {@link Manifest#allowsMintBurnFromGamete()} is true for the manifest of the node,
	 * then that gamete can successfully call this method with any gas price (hence also for free).
	 * 
	 * @param amount the coins to send
	 * @param publicKey the Base64-encoded public key of the gamete or the Base64-encoded ed25519 public key of the account
	 * @return the account in the ledger, old or new
	 */
	public final @FromContract @Payable ExternallyOwnedAccount add(BigInteger amount, String publicKey) {
		if (gamete.publicKey().equals(publicKey)) {
			gamete.receive(amount);
			return gamete;
		}
		else {
			ExternallyOwnedAccount account = accounts.computeIfAbsent(publicKey, ExternallyOwnedAccountED25519::new);
			account.receive(amount);
			return account;
		}
	}
}