package io.takamaka.code.system;

import io.takamaka.code.lang.Account;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful to users of the node.
 */
public final class Manifest extends Storage {
	public final Account gamete;
	public final String takamakaCode;

	public Manifest(Account gamete, String takamakaCode) {
		this.gamete = gamete;
		this.takamakaCode = takamakaCode;
	}

	/**
	 * Yields an account created during the initialization of the node,
	 * that contains the initial stake of the node.
	 * 
	 * @return the account
	 */
	public @View Account getGamete() {
		return gamete;
	}

	/**
	 * Yields the hash of the transaction that installed
	 * the Takamaka base classes in the node.
	 * 
	 * @return the hash
	 */
	public @View String getTakamakaCode() {
		return takamakaCode;
	}
}