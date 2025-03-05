/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.node.api.nodes;

import java.math.BigInteger;
import java.security.PublicKey;
import java.time.LocalDateTime;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.instrumentation.api.GasCostModel;

/**
 * A specification of the consensus parameters of a Hotmoka node. This information
 * is typically contained in the manifest of the node.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
@Immutable
public interface ConsensusConfig<C extends ConsensusConfig<C,B>, B extends ConsensusConfigBuilder<C,B>> {

	/**
	 * Yields the genesis time, UTC.
	 * 
	 * @return the genesis time
	 */
	LocalDateTime getGenesisTime();

	/**
	 * Yields the chain identifier of the node.
	 * 
	 * @return the chain identifier
	 */
	String getChainId();

	/**
	 * Yields the maximal length of the error messages kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 * 
	 * @return the maximal length
	 */
	int getMaxErrorLength();

	/**
	 * Yields the maximal number of dependencies in the classpath of a transaction.
	 * 
	 * @return the maximal number of dependencies
	 */
	int getMaxDependencies();

	/**
	 * Yields the maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * of a transaction.
	 * 
	 * @return the maximal cumulative size (in bytes)
	 */
	long getMaxCumulativeSizeOfDependencies();

	// TODO: probably add the maximal size of an installed jar

	/**
	 * Yields true if and only if the use of the faucet of the gamete is allowed without a valid signature.
	 * 
	 * @return true if and only if the condition holds
	 */
	boolean allowsUnsignedFaucet();

	/**
	 * Yields true if and only if the static verification of the classes of the jars installed in the node must be skipped.
	 * 
	 * @return true if and only if the condition holds
	 */
	boolean skipsVerification();

	/**
	 * Yields the public key of the gamete account.
	 * 
	 * @return the public key
	 */
	PublicKey getPublicKeyOfGamete();

	/**
	 * Yields the Base64-encoded public key of the gamete account.
	 * 
	 * @return the Base64-encoded public key
	 */
	String getPublicKeyOfGameteBase64();

	/**
	 * Yields the initial gas price.
	 * 
	 * @return the initial gas price
	 */
	BigInteger getInitialGasPrice();

	/**
	 * Yields the maximal amount of gas that a non-view transaction can consume.
	 * 
	 * @return the maximal amount of gas that a non-view transaction can consume
	 */
	BigInteger getMaxGasPerTransaction();

	/**
	 * Yields true if and only if the node ignores the minimum gas price.
	 * Hence requests that specify a lower gas price
	 * than the current gas price of the node are executed anyway.
	 * This is mainly useful for testing.
	 * 
	 * @return true if and only if the gas price must be ignored
	 */
	boolean ignoresGasPrice();

	/**
	 * Yields the units of gas that are aimed to be rewarded at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 * 
	 * @return the units of gas that are aimed to be rewarded at each reward
	 */
	BigInteger getTargetGasAtReward();

	/**
	 * Yields how quickly the gas consumed at previous rewards is forgotten:
	 * 0 means never, 1_000_000 means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 * A value of 0 means that the gas price is constant.
	 * 
	 * @return how quickly the gas consumed at previous rewards is forgotten:
	 *         0 means never, 1_000_000 means immediately
	 */
	long getOblivion();

	/**
	 * Yields the initial inflation applied to the gas consumed by transactions before it gets sent
	 * as reward. 1,000,000 means 1%.
	 * Inflation can be negative. For instance, -300,000 means -0.3%.
	 * 
	 * @return the initial inflation applied to the gas consumed by transactions; 1,000,000 means 1%
	 */
	long getInitialInflation();

	/**
	 * Yields the version of the verification module to use.
	 * 
	 * @return the version of the verification module to use
	 */
	long getVerificationVersion();

	/**
	 * Yields the initial supply of coins in the node.
	 * 
	 * @return the initial supply of coins in the node
	 */
	BigInteger getInitialSupply();

	/**
	 * Yields the final supply of coins in the node. Once the current supply reaches
	 * this final amount, it remains constant.
	 * 
	 * @return the final supply of coins in the node
	 */
	BigInteger getFinalSupply();

	/**
	 * Yields the amount of coins to pay to start a new poll amount the voters,
	 * for instance in order to change a consensus parameter.
	 * 
	 * @return the amount of coins to pay to start a new poll
	 */
	BigInteger getTicketForNewPoll();

	/**
	 * Yields the signature algorithm for signing requests.
	 * 
	 * @return the signature algorithm
	 */
	SignatureAlgorithm getSignatureForRequests();

	/**
	 * Yields the gas cost model used for the instrumentation of the jars installed in the node.
	 * 
	 * @return the gas cost model
	 */
	GasCostModel getGasCostModel();

	/**
	 * Yields a TOML representation of this configuration.
	 * 
	 * @return the TOML representation, as a string
	 */
	String toToml();

	/**
	 * Yields a builder initialized with the information in this object.
	 * 
	 * @return the builder
	 */
	B toBuilder();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

	@Override
	String toString();
}