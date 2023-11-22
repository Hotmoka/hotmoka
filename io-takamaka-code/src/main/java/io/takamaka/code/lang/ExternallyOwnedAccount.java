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

package io.takamaka.code.lang;

import java.math.BigInteger;

/**
 * A contract that can be used to pay for a transaction.
 * Its constructors allow one to create such a contract with an initial
 * amount of coins. In order to initialize its red balance as well,
 * one can later call its {@link io.takamaka.code.lang.PayableContract#receiveRed(int)} method
 * or similar.
 */
public class ExternallyOwnedAccount extends PayableContract implements Account {

	/**
	 * The current nonce of this account. If this account is used for paying
	 * a transaction, the nonce in the request of the transaction must match
	 * this value, otherwise the transaction will be rejected.
	 * This value will be incremented at the end of any transaction
	 * (also for unsuccessful transactions).
	 */
	@SuppressWarnings("all")
	private BigInteger nonce = BigInteger.ZERO;

	/**
	 * The Base64-encoded public key of the account.
	 */
	private String publicKey;

	/**
	 * Creates an externally owned contract with no funds.
	 * 
	 * @param publicKey the Base64-encoded public key that will be assigned to the gamete
	 */
	public ExternallyOwnedAccount(String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(int initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(long initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	/**
	 * Creates an externally owned contract with the given initial green funds.
	 * 
	 * @param initialAmount the initial funds
	 * @param publicKey the Base64-encoded public key that will be assigned to the account
	 */
	@Payable @FromContract
	public ExternallyOwnedAccount(BigInteger initialAmount, String publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public @View String toString() {
		return "an externally owned account";
	}

	@Override
	public @View final BigInteger nonce() {
		return nonce;
	}

	/**
	 * Yields the public key of this account.
	 * 
	 * @return the public key
	 */
	public @View final String publicKey() {
		return publicKey;
	}

	/**
	 * Changes the public key of this account. Only this same account can call this method on itself.
	 * 
	 * @param publicKey the new, Base64-encoded public key that will be assigned to this account.
	 *                  After this rotation, the new key must be used to control the account
	 *                  while the old key will not work anymore
	 */
	public final @FromContract(ExternallyOwnedAccount.class) void rotatePublicKey(String publicKey) {
		Takamaka.require(caller() == this, "only this same externally owned account can call this method");
		this.publicKey = publicKey;
	}
}