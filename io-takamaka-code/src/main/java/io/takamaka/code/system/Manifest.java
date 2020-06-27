package io.takamaka.code.system;

import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.RedGreenExternallyOwnedAccount;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful to users of the node. It is an account,
 * so that it can be used to call @View methods on itself.
 */
public final class Manifest extends ExternallyOwnedAccount {
	public final RedGreenExternallyOwnedAccount gamete;

	/**
	 * The initial chainId of the node having this manifest.
	 */
	private String chainId;

	/**
	 * Creates a manifest.
	 * 
	 * @param gamete the gamete of the node having this manifest;
	 *               this is an account that holds all initial coins
	 * @param publicKey the public key of the manifest. Since the manifest
	 *                  is not expected to hold coins, this key is probably
	 *                  useless, but must be set as for every account
	 * @param chainId the initial chainId of the node having this manifest
	 */
	public Manifest(RedGreenExternallyOwnedAccount gamete, String publicKey, String chainId) {
		super(publicKey);

		this.gamete = gamete;
		this.chainId = chainId;
	}

	/**
	 * Yields an account created during the initialization of the node,
	 * that contains the initial stake of the node.
	 * 
	 * @return the account
	 */
	public @View RedGreenExternallyOwnedAccount getGamete() {
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