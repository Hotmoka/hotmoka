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

package io.hotmoka.nodes.api;

import java.math.BigInteger;

import io.hotmoka.annotations.Immutable;

/**
 * A specification of the consensus parameters of a Hotmoka node. This information
 * is typically contained in the manifest of the node.
 */
@Immutable
public interface ConsensusConfig {

	/**
	 * Yields the genesis time, UTC, in ISO8601 pattern.
	 * 
	 * #return the genesis time
	 */
	String getGenesisTime();

	/**
	 * Yields the chain identifier of the node.
	 */
	String getChainId();

	/**
	 * Yields the maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 */
	int getMaxErrorLength();

	/**
	 * Yields the maximal number of dependencies in the classpath of a transaction.
	 */
	int getMaxDependencies();

	/**
	 * Yields the maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * of a transaction.
	 */
	long getMaxCumulativeSizeOfDependencies();

	/**
	 * Yields true if and only if the use of the {@code @@SelfCharged} annotation is allowed.
	 */
	boolean allowsSelfCharged();

	/**
	 * Yields true if and only if the use of the faucet of the gamete is allowed without a valid signature.
	 */
	boolean allowsUnsignedFaucet();

	/**
	 * Yields true if and only if the gamete of the node can call, for free, the add method of the accounts ledger
	 * and the mint/burn methods of the accounts, without paying gas and without paying for the minted coins.
	 */
	boolean allowsMintBurnFromGamete();

	/**
	 * Yields true if and only if the static verification of the classes of the jars installed in the node must be skipped.
	 */
	boolean skipsVerification();

	/**
	 * Yields the Base64-encoded public key of the gamete account.
	 */
	String getPublicKeyOfGamete();

	/**
	 * Yields the initial gas price.
	 */
	BigInteger getInitialGasPrice();

	/**
	 * Yields the maximal amount of gas that a non-view transaction can consume.
	 */
	BigInteger getMaxGasPerTransaction();

	/**
	 * Yields true if and only if the node ignores the minimum gas price.
	 * Hence requests that specify a lower gas price
	 * than the current gas price of the node are executed anyway.
	 * This is mainly useful for testing.
	 */
	boolean ignoresGasPrice();

	/**
	 * Yields the units of gas that are aimed to be rewarded at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 */
	BigInteger getTargetGasAtReward();

	/**
	 * Yields how quick the gas consumed at previous rewards is forgotten:
	 * 0 means never, 1_000_000 means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 * A value of 0 means that the gas price is constant.
	 */
	long getOblivion();

	/**
	 * Yields the initial inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 1,000,000 means 1%.
	 * Inflation can be negative. For instance, -300,000 means -0.3%.
	 */
	long getInitialInflation();

	/**
	 * Yields the version of the verification module to use.
	 */
	int getVerificationVersion();

	/**
	 * Yields the initial supply of coins in the node.
	 */
	BigInteger getInitialSupply();

	/**
	 * Yields the final supply of coins in the node. Once the current supply reaches
	 * this final amount, it remains constant.
	 */
	BigInteger getFinalSupply();

	/**
	 * Yields the initial supply of red coins in the node.
	 */
	BigInteger getInitialRedSupply();

	/**
	 * Yields the amount of coin to pay to start a new poll amount the validators,
	 * for instance in order to change a consensus parameter.
	 */
	BigInteger getTicketForNewPoll();

	/**
	 * Yields the name of the signature algorithm for signing requests.
	 */
	String getSignature();

	/**
	 * Yields the amount of validators' rewards that gets staked. The rest is sent to the validators immediately.
	 * 1000000 = 1%.
	 */
	int getPercentStaked();

	/**
	 * Yields extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the offer cost). 1000000 = 1%.
	 */
	int getBuyerSurcharge();

	/**
	 * Yields the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%.
	 */
	int getSlashingForMisbehaving();

	/**
	 * Yields the percent of stake that gets slashed for validators that do not behave
	 * (or do not vote). 1000000 means 1%.
	 */
	int getSlashingForNotBehaving();

	/**
	 * Yields a toml representation of this configuration.
	 * 
	 * @return the toml representation, as a string
	 */
	String toToml();

	/**
	 * Yields a builder initialized with the information in this object.
	 * 
	 * @return the builder
	 */
	<T extends ConsensusConfigBuilder<T>> ConsensusConfigBuilder<T> intoBuilder(ConsensusConfigBuilder<T> builder);

	@Override
	boolean equals(Object other);

	@Override
	String toString();
}