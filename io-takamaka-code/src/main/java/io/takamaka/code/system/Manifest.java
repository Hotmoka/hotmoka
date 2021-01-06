package io.takamaka.code.system;

import io.takamaka.code.lang.Account;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful for its users. It is an externally-owned account,
 * so that it can be used as caller of view transactions, if needed.
 */
public final class Manifest extends ExternallyOwnedAccount {

	/**
	 * The initial chainId of the node having this manifest.
	 */
	private String chainId;

	/**
	 * The account that initially holds all coins.
	 */
	public final Account gamete;

	/**
	 * The current validators of the node having this manifest. This might be empty.
	 */
	public final Validators validators;

	/**
	 * The object that keeps track of the versions of the modules of the node
	 * having this manifest.
	 */
	public final Versions versions;

	/**
	 * The object that computes the price of the gas.
	 */
	public final GasStation gasStation;

	/**
	 * Creates a manifest.
	 * 
	 * @param chainId the initial chainId of the node having the manifest
	 * @param gamete the account that initially holds all coins
	 * @param builderOfValidators the builder of the validators of the node having the manifest
	 * @throws NullPointerException if any parameter is null
	 */
	public Manifest(String chainId, Account gamete, Validators.Builder builderOfValidators) {
		// we pass a non-existent public key, hence this account is not controllable
		super("");

		if (chainId == null)
			throw new NullPointerException("the chain identifier must be non-null");

		if (gamete == null)
			throw new NullPointerException("the gamete must be non-null");

		if (builderOfValidators == null)
			throw new NullPointerException("the builder of the validators must be non-null");

		this.chainId = chainId;
		this.gamete = gamete;

		this.validators = builderOfValidators.apply(this);
		if (validators == null)
			throw new NullPointerException("the validators must be non-null");

		this.versions = new Versions(this);
		this.gasStation = new GasStation(this);
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
	 * Yields the gamete of the node having this manifest.
	 * This is the account that initially holds all coins.
	 * 
	 * @return the gamete
	 */
	public final @View Account getGamete() {
		return gamete;
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
	 * Yields the object that keeps track of the versions of the
	 * modules of the node having this manifest.
	 * 
	 * @return the object that keeps track of the versions
	 */
	public final @View Versions getVersions() {
		return versions;
	}

	/**
	 * Yields the object that controls the price of the gas.
	 * 
	 * @return the object that controls the price of the gas
	 */
	public final @View GasStation getGasStation() {
		return gasStation;
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