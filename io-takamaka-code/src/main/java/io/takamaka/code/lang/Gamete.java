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

import static io.takamaka.code.lang.Takamaka.require;
import static java.math.BigInteger.ZERO;

import java.math.BigInteger;

/**
 * A contract that can be used as gamete of a network. It is an externally-owned
 * account with a faucet method for providing funds to other externally-owned accounts.
 * The faucet can be disabled by fixing its maximum to zero.
 */
public final class Gamete extends ExternallyOwnedAccount {

	/**
	 * The maximal amount of coins that the faucet can provide at each call.
	 */
	private BigInteger maxFaucet = ZERO;

	/**
	 * The maximal amount of red coins that the faucet can provide at each call.
	 */
	private BigInteger maxRedFaucet = ZERO;

	/**
	 * Creates a gamete without initial funds.
	 * 
	 * @param publicKey the Base64-encoded public key of the gamete
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	public Gamete(String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates a gamete with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the gamete
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	@Payable @FromContract
	public Gamete(int initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates a gamete with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the gamete
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	@Payable @FromContract
	public Gamete(long initialAmount, String publicKey) {
		super(publicKey);
	}

	/**
	 * Creates a gamete with the given initial fund.
	 * 
	 * @param initialAmount the initial fund
	 * @param publicKey the Base64-encoded public key of the gamete
	 * @throws NullPointerException if {@code publicKey} is null
	 */
	@Payable @FromContract
	public Gamete(BigInteger initialAmount, String publicKey) {
		super(publicKey);
	}

	@Override
	public String toString() {
		return "a gamete";
	}

	/**
	 * Yields the maximal amount of coins that the faucet can provide at each call.
	 * 
	 * @return the maximal amount of coins
	 */
	public final @View BigInteger getMaxFaucet() {
		return maxFaucet;
	}

	/**
	 * Yields the maximal amount of red coins that the faucet can provide at each call.
	 * 
	 * @return the maximal amount of red coins
	 */
	public final @View BigInteger getMaxRedFaucet() {
		return maxRedFaucet;
	}

	/**
	 * Sets the maximal threshold for the faucet of this gamete.
	 * Only the gamete itself can call this method.
	 * 
	 * @param maxFaucet the maximal threshold for the green coins; use zero to disable the green faucet
	 * @param maxRedFaucet the maximal threshold for the red coins; use zero to disable the red faucet
	 */
	public final @FromContract void setMaxFaucet(BigInteger maxFaucet, BigInteger maxRedFaucet) {
		require(maxFaucet != null && maxFaucet.signum() >= 0, "the threshold of the faucet must be a non-negative BigInteger");
		require(maxRedFaucet != null && maxRedFaucet.signum() >= 0, "the threshold of the red faucet must be a non-negative BigInteger");
		require(caller() == this, "only the gamete can change the thresholds of its own faucet");

		this.maxFaucet = maxFaucet;
		this.maxRedFaucet = maxRedFaucet;
	}

	/**
	 * Yields a new account with the given initial green coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccount faucet(BigInteger green, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		return new ExternallyOwnedAccount(green, publicKey);
	}

	/**
	 * Yields a new ED25519 account with the given initial green coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountED25519 faucetED25519(BigInteger green, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		return new ExternallyOwnedAccountED25519(green, publicKey);
	}

	/**
	 * Yields a new SHA256DSA account with the given initial green coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountSHA256DSA faucetSHA256DSA(BigInteger green, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		return new ExternallyOwnedAccountSHA256DSA(green, publicKey);
	}

	/**
	 * Yields a new QTESLA1 account with the given initial green coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountQTESLA1 faucetQTESLA1(BigInteger green, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		return new ExternallyOwnedAccountQTESLA1(green, publicKey);
	}

	/**
	 * Yields a new QTESLA3 account with the given initial green coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountQTESLA3 faucetQTESLA3(BigInteger green, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		return new ExternallyOwnedAccountQTESLA3(green, publicKey);
	}

	/**
	 * Yields a new account with the given initial green coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccount faucet(int green, String publicKey) {
		require(green >= 0 && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		return new ExternallyOwnedAccount(green, publicKey);
	}
	
	/**
	 * Yields a new account with the given initial green coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccount faucet(long green, String publicKey) {
		require(green >= 0L && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		return new ExternallyOwnedAccount(green, publicKey);
	}

	/**
	 * Yields a new account with the given initial green and red coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param red the initial red funds of the new account, between 0 and the maximal threshold
	 *            set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccount faucet(BigInteger green, BigInteger red, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red != null && red.signum() >= 0 && red.compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		ExternallyOwnedAccount account = new ExternallyOwnedAccount(green, publicKey);
		account.receiveRed(red);
		return account;
	}

	/**
	 * Yields a new ED25519 account with the given initial green and red coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param red the initial red funds of the new account, between 0 and the maximal threshold
	 *            set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountED25519 faucetED25519(BigInteger green, BigInteger red, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red != null && red.signum() >= 0 && red.compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		ExternallyOwnedAccountED25519 account = new ExternallyOwnedAccountED25519(green, publicKey);
		account.receiveRed(red);
		return account;
	}

	/**
	 * Yields a new SHA256DSA account with the given initial green and red coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param red the initial red funds of the new account, between 0 and the maximal threshold
	 *            set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountSHA256DSA faucetSHA256DSA(BigInteger green, BigInteger red, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red != null && red.signum() >= 0 && red.compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		ExternallyOwnedAccountSHA256DSA account = new ExternallyOwnedAccountSHA256DSA(green, publicKey);
		account.receiveRed(red);
		return account;
	}

	/**
	 * Yields a new QTESLA1 account with the given initial green and red coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param red the initial red funds of the new account, between 0 and the maximal threshold
	 *            set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountQTESLA1 faucetQTESLA1(BigInteger green, BigInteger red, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red != null && red.signum() >= 0 && red.compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		ExternallyOwnedAccountQTESLA1 account = new ExternallyOwnedAccountQTESLA1(green, publicKey);
		account.receiveRed(red);
		return account;
	}

	/**
	 * Yields a new QTESLA3 account with the given initial green and red coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param red the initial red funds of the new account, between 0 and the maximal threshold
	 *            set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccountQTESLA3 faucetQTESLA3(BigInteger green, BigInteger red, String publicKey) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red != null && red.signum() >= 0 && red.compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		ExternallyOwnedAccountQTESLA3 account = new ExternallyOwnedAccountQTESLA3(green, publicKey);
		account.receiveRed(red);
		return account;
	}

	/**
	 * Yields a new account with the given initial green and red coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param red the initial red funds of the new account, between 0 and the maximal threshold
	 *            set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccount faucet(int green, int red, String publicKey) {
		require(green >= 0 && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red >= 0 && BigInteger.valueOf(red).compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		ExternallyOwnedAccount account = new ExternallyOwnedAccount(green, publicKey);
		account.receiveRed(red);
		return account;
	}

	/**
	 * Yields a new account with the given initial green and red coins, paid by this gamete.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param green the initial funds of the new account, between 0 and the maximal threshold
	 *              set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param red the initial red funds of the new account, between 0 and the maximal threshold
	 *            set with {@link #setMaxFaucet(BigInteger, BigInteger)}
	 * @param publicKey the public key of the new account
	 * @return the new account
	 */
	public final @FromContract ExternallyOwnedAccount faucet(long green, long red, String publicKey) {
		require(green >= 0 && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red >= 0 && BigInteger.valueOf(red).compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		ExternallyOwnedAccount account = new ExternallyOwnedAccount(green, publicKey);
		account.receiveRed(red);
		return account;
	}

	/**
	 * Sends the given amount of coins to the given payable contract.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param contract the payable contract that will receive the coins
	 * @param green the coins to send to {@code contract}
	 */
	public final @FromContract void faucet(PayableContract contract, BigInteger green) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		contract.receive(green);
	}

	/**
	 * Sends the given amount of coins to the given payable contract.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param contract the payable account that will receive the coins
	 * @param green the coins to send to {@code contract}
	 */
	public final @FromContract void faucet(PayableContract contract, int green) {
		require(green >= 0 && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		contract.receive(green);
	}

	/**
	 * Sends the given amount of coins to the given payable contract.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param contract the payable contract that will receive the coins
	 * @param green the coins to send to {@code contract}
	 */
	public final @FromContract void faucet(PayableContract contract, long green) {
		require(green >= 0 && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		contract.receive(green);
	}

	/**
	 * Sends the given amount of coins to the given red/green payable contract.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param contract the red/green payable contract that will receive the coins
	 * @param green the coins to send to {@code contract}
	 * @param red the red coins to send to {@code contract}
	 */
	public final @FromContract void faucet(PayableContract contract, BigInteger green, BigInteger red) {
		require(green != null && green.signum() >= 0 && green.compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red != null && red.signum() >= 0 && red.compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		contract.receive(green);
		contract.receiveRed(red);
	}

	/**
	 * Sends the given amount of coins to the given red/green payable contract.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param contract the red/green payable contract that will receive the coins
	 * @param green the coins to send to {@code contract}
	 * @param red the red coins to send to {@code contract}
	 */
	public final @FromContract void faucet(PayableContract contract, int green, int red) {
		require(green >= 0 && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red >= 0 && BigInteger.valueOf(red).compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		contract.receive(green);
		contract.receiveRed(red);
	}

	/**
	 * Sends the given amount of coins to the given red/green payable contract.
	 * Only the gamete itself can call this method.
	 * This method is special, in the sense that it can be called without a correct
	 * signature, if the {@code allowsUnsignedFaucet} consensus option is set.
	 * 
	 * @param contract the red/green payable contract that will receive the coins
	 * @param green the coins to send to {@code contract}
	 * @param red the red coins to send to {@code contract}
	 */
	public final @FromContract void faucet(PayableContract contract, long green, long red) {
		require(green >= 0 && BigInteger.valueOf(green).compareTo(maxFaucet) <= 0, () -> "the balance must be between 0 and " + maxFaucet + " inclusive");
		require(red >= 0 && BigInteger.valueOf(red).compareTo(maxRedFaucet) <= 0, () -> "the red balance must be between 0 and " + maxRedFaucet + " inclusive");
		require(caller() == this, "only the gamete can call its own faucet");
		contract.receive(green);
		contract.receiveRed(red);
	}
}