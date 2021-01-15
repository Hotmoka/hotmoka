package io.takamaka.code.system;

import static io.takamaka.code.lang.Takamaka.require;

import java.util.function.Function;

import io.takamaka.code.lang.Account;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.RequirementViolationException;
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
	private final String chainId;

	/**
	 * The account that initially holds all coins.
	 */
	private final Account gamete;

	/**
	 * The maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 */
	private final int maxErrorLength;

	/**
	 * True if and only if the use of the {@code @@SelfCharged} annotation is allowed.
	 */
	private final boolean allowsSelfCharged;

	/**
	 * The name of the signature algorithm that must be used to sign the requests sent to the node.
	 */
	private final String signature;

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
	 * @param maxErrorLength the maximal length of the error message kept in the store of the node.
	 *                       Beyond this threshold, the message gets truncated
	 * @param allowsSelfCharged true if and only if the use of the {@code @@SelfCharged} annotation is allowed
	 * @param signature the name of the signature algorithm that must be used to sign the requests sent to the node
	 * @param gamete the account that initially holds all coins
	 * @param verificationVersion the version of the verification module to use
	 * @param builderOfValidators the builder of the validators of the node having the manifest
	 * @param builderOfGasStation the builder of the gas station of the node having the manifest
	 * @throws RequirementViolationException if any parameter is null or any builder yields null or the maximal error length is negative
	 */
	public Manifest(String chainId, int maxErrorLength, boolean allowsSelfCharged, String signature, Account gamete, int verificationVersion, Function<Manifest, Validators> builderOfValidators, Function<Manifest, GasStation> builderOfGasStation) {
		super(""); // we pass a non-existent public key, hence this account is not controllable

		require(chainId != null, "the chain identifier must be non-null");
		require(gamete != null, "the gamete must be non-null");
		require(builderOfValidators != null, "the builder of the validators must be non-null");
		require(maxErrorLength >= 0, "the maximal error length must be non-negative");
		require(signature != null, "the name of the signature algorithm cannot be null");
		require(verificationVersion >= 0, "the verification version must be non-negative");

		this.chainId = chainId;
		this.gamete = gamete;
		this.maxErrorLength = maxErrorLength;
		this.allowsSelfCharged = allowsSelfCharged;
		this.signature = signature;
		this.validators = builderOfValidators.apply(this);
		require(validators != null, "the validators must be non-null");
		this.versions = new Versions(this, verificationVersion);
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
	 * Yields the maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 */
	public final @View int getMaxErrorLength() {
		return maxErrorLength;
	}

	/**
	 * Determines if the use of the {@code @@SelfCharged} annotation is allowed.
	 * 
	 * @return true if and only if it is allowed
	 */
	public final @View boolean allowsSelfCharged() {
		return allowsSelfCharged;
	}

	/**
	 * Yields the name of the signature algorithm that must be used to sign the
	 * requests sent to the node.
	 * 
	 * @return the name of the signature algorithm
	 */
	public final @View String getSignature() {
		return signature;
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