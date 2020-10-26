package io.takamaka.code.system;

import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageArray;

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
	 * The current validators of this node. This might be empty.
	 */
	private final Validators validators;

	/**
	 * Creates a manifest.
	 * 
	 * @param chainId the initial chainId of the node having this manifest
	 * @param validators the initial validators of the node having this manifest. This can be empty
	 * @throws NullPointerException if any parameter is null
	 */
	public Manifest(String chainId, StorageArray<Validator> validators) {
		if (chainId == null)
			throw new NullPointerException("the chain identifier must be non-null");

		this.chainId = chainId;
		this.validators = mkValidators(validators);
	}

	/**
	 * Yields the specific implementation of the validators set for this manifest.
	 * Subclasses might redefine.
	 * 
	 * @param validators the initial validators of the node having this manifest. This can be empty
	 *                   but is never {@code null}
	 * @return the validators set
	 */
	protected Validators mkValidators(StorageArray<Validator> validators) {
		return new Validators(validators);
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
	 * Yields the set of the current validators of the node having this manifest.
	 * 
	 * @return the set of current validators. This might be empty
	 */
	public @View Validators getValidators() {
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