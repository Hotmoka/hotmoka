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

	/**
	 * The initial chainId of the node having this manifest.
	 */
	private String chainId;

	/**
	 * Creates a manifest.
	 * 
	 * @param gamete the gamete of the node having this manifest;
	 *               this is an account that holds all initial coins
	 * @param chainId the initial chainId of the node having this manifest
	 * @throws NullPointerException if any parameter is null
	 */
	public Manifest(Account gamete, String chainId) {
		if (gamete == null)
			throw new NullPointerException("the gamete must be non-null");

		if (chainId == null)
			throw new NullPointerException("the chain identifier must be non-null");

		this.gamete = gamete;
		this.chainId = chainId;
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
	 * Yields the current chain identifier for the node having this manifest.
	 * 
	 * @return the chain identifier
	 */
	public @View String getChainId() {
		return chainId;
	}

	/**
	 * Changes the chain identifier of the node having this manifest.
	 * 
	 * @param newChainId the new chain identifier of the node
	 */
	public void setChainId(String newChainId) {
		throw new UnsupportedOperationException("this manifest does not allow one to change the node's chain identifier");
	}
}