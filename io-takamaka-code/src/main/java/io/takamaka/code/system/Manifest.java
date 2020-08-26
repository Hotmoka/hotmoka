package io.takamaka.code.system;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful its users.
 */
public final class Manifest extends Storage {

	/**
	 * The initial chainId of the node having this manifest.
	 */
	private String chainId;

	/**
	 * Creates a manifest.
	 * 
	 * @param chainId the initial chainId of the node having this manifest
	 * @throws NullPointerException if any parameter is null
	 */
	public Manifest(String chainId) {
		if (chainId == null)
			throw new NullPointerException("the chain identifier must be non-null");

		this.chainId = chainId;
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