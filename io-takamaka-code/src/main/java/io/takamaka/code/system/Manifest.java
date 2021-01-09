package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.require;

import java.util.function.Function;

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
	 * The chain identifier of the node having this manifest.
	 */
	public final String chainId;

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
	 * @throws RequirementViolationException if any parameter is null or any builder yields null
	 */
	public Manifest(String chainId, Account gamete, Function<Manifest, Validators> builderOfValidators, Function<Manifest, GasStation> builderOfGasStation) {
		super(""); // we pass a non-existent public key, hence this account is not controllable

		require(chainId != null, "the chain identifier must be non-null");
		require(gamete != null, "the gamete must be non-null");
		require(builderOfValidators != null, "the builder of the validators must be non-null");

		this.chainId = chainId;
		this.gamete = gamete;
		this.validators = builderOfValidators.apply(this);
		require(validators != null, "the validators must be non-null");
		this.versions = new Versions(this);
		this.gasStation = builderOfGasStation.apply(this);
		require(gasStation != null, "the gas station must be non-null");
	}

	/**
	 * Yields the chain identifier for the node having this manifest.
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
}