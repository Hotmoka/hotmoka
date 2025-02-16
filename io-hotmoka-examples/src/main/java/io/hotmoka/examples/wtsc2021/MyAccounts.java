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

package io.hotmoka.examples.wtsc2021;

import java.math.BigInteger;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.ExternallyOwnedAccounts;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;

public class MyAccounts extends ExternallyOwnedAccounts {
	
	/**
	 * Creates the container, for normal accounts.
	 * 
	 * @param amount the total amount of coins distributed to the accounts that get created;
	 *                this must be the sum of all {@code balances}
	 * @param balances the initial, green balances of the accounts; they must be as many as the {@code publicKeys}
	 * @param publicKeys the Base64-encoded public keys of the accounts
	 */
	public @FromContract @Payable MyAccounts(BigInteger amount, BigInteger[] balances, String[] publicKeys) {
		super(amount, balances, publicKeys);
	}

	/**
	 * Creates the container, for normal accounts.
	 * 
	 * @param amount the total amount of coins distributed to the accounts that get created;
	 *                this must be the sum of all {@code balances}
	 * @param balances the initial balances of the accounts,
	 *               as a space-separated sequence of big integers; they must be as many
	 *               as there are public keys in {@code publicKeys}
	 * @param publicKeys the public keys of the accounts,
	 *                   as a space-separated sequence of Base64-encoded public keys
	 */
	public @FromContract @Payable MyAccounts(BigInteger amount, String balances, String publicKeys) {
		super(amount, balances, publicKeys);
	}

	/**
	 * Yields the richest account in this container.
	 * 
	 * @return the richest account, or {@code null} is this container is empty
	 */
	public @View ExternallyOwnedAccount richest() {
		class Richest {
			private ExternallyOwnedAccount account;
		}

		var richest = new Richest();

		forEach(account -> {
			if (richest.account == null || BigIntegerSupport.compareTo(richest.account.balance(), account.balance()) < 0)
				richest.account = account;
		});

		return richest.account;
	}
}