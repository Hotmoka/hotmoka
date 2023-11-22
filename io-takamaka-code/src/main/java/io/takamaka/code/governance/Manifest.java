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

package io.takamaka.code.governance;

import static io.takamaka.code.lang.Takamaka.require;

import java.util.function.Function;

import io.takamaka.code.dao.SharedEntityView;
import io.takamaka.code.lang.ExternallyOwnedAccount;
import io.takamaka.code.lang.Gamete;
import io.takamaka.code.lang.RequirementViolationException;
import io.takamaka.code.lang.View;

/**
 * The manifest of a node. It contains information about the node,
 * that can be helpful for its users. It is an externally-owned account,
 * so that it can be used as caller of view transactions, if needed.
 * 
 * @param <V> the type of the validator contracts
 */
public final class Manifest<V extends Validator> extends ExternallyOwnedAccount {

	/**
	 * The genesis time of the node having this manifest.
	 * This is UTC, in ISO8601 format.
	 */
	private final String genesisTime;

	/**
	 * The chain identifier of the node having this manifest.
	 */
	private final String chainId;

	/**
	 * The account that initially holds all coins.
	 */
	private final Gamete gamete;

	/**
	 * The maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 */
	private final long maxErrorLength;

	/**
	 * The maximal number of dependencies in the classpath of a transaction.
	 */
	private final long maxDependencies;

	/**
	 * The maximal cumulative size (in bytes) of the instrumented jars of the dependencies of a transaction.
	 */
	private final long maxCumulativeSizeOfDependencies;

	/**
	 * True if and only if the use of the {@code @@SelfCharged} annotation is allowed.
	 */
	private final boolean allowsSelfCharged;

	/**
	 * True if and only if the use of the {@code faucet()} methods of the gametes is allowed without a valid signature.
	 */
	private final boolean allowsUnsignedFaucet;

	/**
	 * True if and only if the verification of the classes of the jars installed in the node must be skipped.
	 */
	private final boolean skipsVerification;

	/**
	 * The name of the signature algorithm that must be used to sign the requests sent to the node.
	 */
	private final String signature;

	/**
	 * The initial validators of the node having this manifest. This is immutable and might be empty.
	 */
	public final SharedEntityView<V> initialValidators;

	/**
	 * The current validators of the node having this manifest. This might be empty.
	 */
	public final Validators<V> validators;

	/**
	 * The object that keeps track of the versions of the modules of the node having this manifest.
	 */
	public final Versions<V> versions;

	/**
	 * The object that computes the price of the gas.
	 */
	public final GasStation<V> gasStation;

	/**
	 * An object that can be used to store and retrieve accounts from their public key.
	 * It can be used in order to request somebody to create, and possibly fund,
	 * an account on our behalf, and store it in this ledger for public evidence.
	 */
	public final AccountsLedger accountsLedger;

	/**
	 * Creates a manifest.
	 * 
	 * @param genesisTime a string the will be used to report the genesis time of the node
	 * @param chainId the initial chainId of the node having the manifest
	 * @param maxErrorLength the maximal length of the error message kept in the store of the node.
	 *                       Beyond this threshold, the message gets truncated
	 * @param maxDependencies the maximal number of dependencies per transaction
	 * @param maxCumulativeSizeOfDependencies the maximal cumulative size of the the dependencies per transaction
	 * @param allowsSelfCharged true if and only if the use of the {@code @@SelfCharged} annotation is allowed
	 * @param skipsVerification true if and only if the verification of the classes of the jars installed in the node must be skipped
	 * @param signature the name of the signature algorithm that must be used to sign the requests sent to the node
	 * @param gamete the account that initially holds all coins
	 * @param verificationVersion the version of the verification module to use
	 * @param builderOfValidators the builder of the validators of the node having the manifest
	 * @param builderOfGasStation the builder of the gas station of the node having the manifest
	 * @throws RequirementViolationException if any parameter is null or any builder yields null or the maximal error length is negative
	 */
	public Manifest(String genesisTime, String chainId, long maxErrorLength, long maxDependencies, long maxCumulativeSizeOfDependencies, boolean allowsSelfCharged,
			boolean allowsFaucet, boolean skipsVerification, String signature, Gamete gamete, long verificationVersion,
			Function<Manifest<V>, Validators<V>> builderOfValidators, Function<Manifest<V>, GasStation<V>> builderOfGasStation) {

		super(""); // we pass a non-existent public key, hence this account is not controllable

		require(genesisTime != null, "the genesis time must be non-null");
		require(chainId != null, "the chain identifier must be non-null");
		require(gamete != null, "the gamete must be non-null");
		require(builderOfValidators != null, "the builder of the validators must be non-null");
		require(maxErrorLength >= 0, "the maximal error length must be non-negative");
		require(maxDependencies >= 1, "the maximal number of dependencies per transaction must be at least 1");
		require(maxCumulativeSizeOfDependencies >= 100_000, "the maximal cumulative size of the dependencies per transaction must be at least 100,000");
		require(signature != null, "the name of the signature algorithm cannot be null");
		require(verificationVersion >= 0, "the verification version must be non-negative");

		this.genesisTime = genesisTime;
		this.chainId = chainId;
		this.gamete = gamete;
		this.maxErrorLength = maxErrorLength;
		this.maxDependencies = maxDependencies;
		this.maxCumulativeSizeOfDependencies = maxCumulativeSizeOfDependencies;
		this.allowsSelfCharged = allowsSelfCharged;
		this.allowsUnsignedFaucet = allowsFaucet;
		this.skipsVerification = skipsVerification;
		this.signature = signature;
		this.validators = builderOfValidators.apply(this);
		require(validators != null, "the validators must be non-null");
		this.initialValidators = validators.snapshot();
		this.versions = new Versions<>(this, verificationVersion);
		this.gasStation = builderOfGasStation.apply(this);
		require(gasStation != null, "the gas station must be non-null");
		this.accountsLedger = new AccountsLedger(this);
	}

	/**
	 * Yields the genesis time for the node having this manifest.
	 * 
	 * @return the genesis time, UTC, in ISO8601 format
	 */
	public final @View String getGenesisTime() {
		return genesisTime;
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
	 * 
	 * @return the length
	 */
	public final @View long getMaxErrorLength() {
		return maxErrorLength;
	}

	/**
	 * Yields the maximal number of dependencies per transaction.
	 * Beyond this threshold, a transaction gets rejected.
	 * 
	 * @return the maximal number of dependencies
	 */
	public final @View long getMaxDependencies() {
		return maxDependencies;
	}

	/**
	 * Yields the maximal cumulative size of the dependencies per transaction.
	 * Beyond this threshold, a transaction gets rejected.
	 * 
	 * @return the maximal cumulative size
	 */
	public final @View long getMaxCumulativeSizeOfDependencies() {
		return maxCumulativeSizeOfDependencies;
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
	 * Determines if the use of the {@code faucet()} methods of the method is allowed
	 * without a valid signature.
	 * 
	 * @return true if and only if it is allowed
	 */
	public final @View boolean allowsUnsignedFaucet() {
		return allowsUnsignedFaucet;
	}

	/**
	 * Determines if the verification of the classes of the jars installed in the node must be skipped.
	 * 
	 * @return true if and only if verification is skipped
	 */
	public final @View boolean skipsVerification() {
		return skipsVerification;
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
	public final @View Gamete getGamete() {
		return gamete;
	}

	/**
	 * Yields the initial set of validators of the node having this manifest.
	 *
	 * @return the initial validators. This is immutable and might be empty
	 */
	public final @View SharedEntityView<V> getInitialValidators() {
		return initialValidators;
	}

	/**
	 * Yields the current validators of the node having this manifest.
	 * 
	 * @return the current validators. This might be empty
	 */
	public final @View Validators<V> getValidators() {
		return validators;
	}

	/**
	 * Yields the object that keeps track of the versions of the
	 * modules of the node having this manifest.
	 * 
	 * @return the object that keeps track of the versions
	 */
	public final @View Versions<V> getVersions() {
		return versions;
	}

	/**
	 * Yields the object that controls the price of the gas.
	 * 
	 * @return the object that controls the price of the gas
	 */
	public final @View GasStation<V> getGasStation() {
		return gasStation;
	}

	/**
	 * Yields the object that can be used to store and retrieve accounts from their public key.
	 * It can be used in order to request somebody to create, and possibly fund,
	 * an account on our behalf, and store it in this ledger for public evidence.
	 * 
	 * @return the object that implements the account ledger
	 */
	public final @View AccountsLedger getAccountsLedger() {
		return accountsLedger;
	}

	@Override
	public String toString() {
		return "a manifest of a Hotmoka node";
	}
}