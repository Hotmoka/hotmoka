package io.takamaka.code.system;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful for its users.
 */
public final class Manifest extends Storage {

	/**
	 * The initial chainId of the node having this manifest.
	 */
	private String chainId;

	/**
	 * The current validators of the node having this manifest. This might be empty.
	 */
	private final Validators validators;

	/**
	 * Creates a manifest.
	 * 
	 * @param chainId the initial chainId of the node having the manifest
	 * @param validators the initial validators of the node having the manifest
	 * @throws NullPointerException if any parameter is null
	 */
	public Manifest(String chainId, Validators validators) {
		if (chainId == null)
			throw new NullPointerException("the chain identifier must be non-null");

		if (validators == null)
			throw new NullPointerException("the validators must be non-null");

		this.chainId = chainId;
		this.validators = validators;
	}

	/**
	 * Yields the current chain identifier for the node having this manifest.
	 * 
	 * @return the chain identifier
	 */
	public final @View String getChainId() {
		return chainId;
	}

	/**
	 * Yields the current validators of the node having this manifest.
	 * 
	 * @return the current validators. This might be empty
	 */
	public final @View Validators getValidators() {
		return validators;
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