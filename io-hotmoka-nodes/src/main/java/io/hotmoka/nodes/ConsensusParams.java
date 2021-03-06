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

package io.hotmoka.nodes;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * A specification of the consensus parameters of a node. This information
 * is typically contained in the manifest of a node.
 */
public class ConsensusParams {

	/**
	 * The chain identifier of the node. It defaults to the empty string.
	 */
	public final String chainId;

	/**
	 * The maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 * It defaults to 300 characters.
	 */
	public final int maxErrorLength;

	/**
	 * The maximal number of dependencies in the classpath of a transaction.
	 * It defaults to 20.
	 */
	public final int maxDependencies;

	/**
	 * The maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * of a transaction. It defaults to 10,000,000.
	 */
	public final long maxCumulativeSizeOfDependencies;

	/**
	 * True if and only if the use of the {@code @@SelfCharged} annotation is allowed.
	 * It defaults to false.
	 */
	public final boolean allowsSelfCharged;

	/**
	 * True if and only if the use of the faucet of the gamete is allowed without a valid signature.
	 * It defaults to false.
	 */
	public final boolean allowsUnsignedFaucet;

	/**
	 * True if and only if the static verification of the classes of the jars installed in the node must be skipped.
	 * It defaults to false.
	 */
	public final boolean skipsVerification;

	/**
	 * The maximal amount of gas that a non-view transaction can consume.
	 * It defaults to 1_000_000_000.
	 */
	public final BigInteger maxGasPerTransaction;

	/**
	 * True if and only if the node ignores the minimum gas price.
	 * Hence requests that specify a lower gas price
	 * than the current gas price of the node are executed anyway.
	 * This is mainly useful for testing. It defaults to false.
	 */
	public final boolean ignoresGasPrice;

	/**
	 * The units of gas that are aimed to be rewarded at each reward.
	 * If the actual reward is smaller, the price of gas must decrease.
	 * If it is larger, the price of gas must increase.
	 * This defaults to 1_000_000.
	 */
	public final BigInteger targetGasAtReward;

	/**
	 * How quick the gas consumed at previous rewards is forgotten:
	 * 0 means never, 1_000_000 means immediately.
	 * Hence a smaller level means that the latest rewards are heavier
	 * in the determination of the gas price.
	 * It defaults to 50_000.
	 */
	public final long oblivion;

	/**
	 * The inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 0 means 0%, 100,000 means 1%,
	 * 10,000,000 means 100%, 20,000,000 means 200% and so on.
	 * Inflation can be negative. For instance, -30,000 means -0.3%.
	 * This defaults to 10,000 (that is, inflation is 0.1% by default).
	 */
	public final long inflation;

	/**
	 * The version of the verification module to use. It defaults to 0.
	 */
	public final int verificationVersion;

	/**
	 * The amount of coin to pay to start a new poll amount the validators,
	 * for instance in order to change a consensus parameter.
	 */
	public final BigInteger ticketForNewPoll;

	/**
	 * The name of the signature algorithm for signing requests. It defaults to "ed25519".
	 */
	public final String signature;

	private ConsensusParams(Builder builder) throws NoSuchAlgorithmException {
		this.chainId = builder.chainId;
		this.maxErrorLength = builder.maxErrorLength;
		this.allowsSelfCharged = builder.allowsSelfCharged;
		this.allowsUnsignedFaucet = builder.allowsUnsignedFaucet;
		this.maxGasPerTransaction = builder.maxGasPerTransaction;
		this.ignoresGasPrice = builder.ignoresGasPrice;
		this.skipsVerification = builder.skipsVerification;
		this.targetGasAtReward = builder.targetGasAtReward;
		this.oblivion = builder.oblivion;
		this.inflation = builder.inflation;
		this.verificationVersion = builder.verificationVersion;
		this.maxDependencies = builder.maxDependencies;
		this.maxCumulativeSizeOfDependencies = builder.maxCumulativeSizeOfDependencies;
		this.ticketForNewPoll = builder.ticketForNewPoll;
		this.signature = builder.signature;
	}

	/**
	 * Chain a builder initialized with the information in this object.
	 * 
	 * @return the builder
	 */
	public Builder toBuilder() {
		return new Builder()
			.setChainId(chainId)
			.setMaxErrorLength(maxErrorLength)
			.setMaxDependencies(maxDependencies)
			.setMaxCumulativeSizeOfDependencies(maxCumulativeSizeOfDependencies)
			.allowSelfCharged(allowsSelfCharged)
			.allowUnsignedFaucet(allowsUnsignedFaucet)
			.signRequestsWith(signature)
			.setMaxGasPerTransaction(maxGasPerTransaction)
			.ignoreGasPrice(ignoresGasPrice)
			.skipVerification(skipsVerification)
			.setTargetGasAtReward(targetGasAtReward)
			.setOblivion(oblivion)
			.setInflation(inflation)
			.setVerificationVersion(verificationVersion)
			.setTicketForNewPoll(ticketForNewPoll);
	}

	public static class Builder {
		private String chainId = "";
		private int maxErrorLength = 300;
		private boolean allowsSelfCharged = false;
		private boolean allowsUnsignedFaucet = false;
		private String signature = "ed25519";
		private BigInteger maxGasPerTransaction = BigInteger.valueOf(1_000_000_000L);
		private int maxDependencies = 20;
		private long maxCumulativeSizeOfDependencies = 10_000_000;
		private boolean ignoresGasPrice = false;
		private boolean skipsVerification = false;
		private BigInteger targetGasAtReward = BigInteger.valueOf(1_000_000L);
		private long oblivion = 250_000L;
		private long inflation = 10_000L; // 0.1%
		private int verificationVersion = 0;
		private BigInteger ticketForNewPoll = BigInteger.valueOf(100);

		/**
		 * Builds the parameters.
		 * 
		 * @return the parameters
		 * @throws NoSuchAlgorithmException if the required signature algorithm is not available in the Java installation
		 */
		public ConsensusParams build() throws NoSuchAlgorithmException {
			return new ConsensusParams(this);
		}

		/**
		 * Specifies the chain identifier that will be set for the node.
		 * This defaults to the empty string.
		 * 
		 * @param chainId the chain identifier
		 * @return this same builder
		 */
		public Builder setChainId(String chainId) {
			if (chainId == null)
				throw new NullPointerException("the chain identifier cannot be null");

			this.chainId = chainId;
			return this;
		}

		/**
		 * Sets the maximal length of the error message kept in the store of the node.
		 * Beyond this threshold, the message gets truncated.
		 * It defaults to 300 characters.
		 * 
		 * @param maxErrorLength the maximal error length
		 * @return this builder
		 */
		public Builder setMaxErrorLength(int maxErrorLength) {
			if (maxErrorLength < 0)
				throw new IllegalArgumentException("the maximal error length cannot be negative");

			this.maxErrorLength = maxErrorLength;
			return this;
		}

		/**
		 * Sets the maximal number of dependencies per transaction. These are the jars that form
		 * the class path of a transaction. It defaults to 20.
		 * 
		 * @param maxDependencies the maximal number of dependencies
		 * @return this builder
		 */
		public Builder setMaxDependencies(int maxDependencies) {
			if (maxDependencies < 1)
				throw new IllegalArgumentException("the maximal number of dependencies per transaction must be at least 1");

			this.maxDependencies = maxDependencies;
			return this;
		}

		/**
		 * Sets the maximal cumulative size of the dependencies per transaction. These are the number of bytes
		 * of the instrumented jars that form the class path of a transaction. It defaults to 10,000,000.
		 * 
		 * @param maxSizeOfDependencies the maximal number of dependencies
		 * @return this builder
		 */
		public Builder setMaxCumulativeSizeOfDependencies(long maxSizeOfDependencies) {
			if (maxSizeOfDependencies < 100_000)
				throw new IllegalArgumentException("the maximal cumulative size of the dependencies per transaction must be at least 100,000");

			this.maxCumulativeSizeOfDependencies = maxSizeOfDependencies;
			return this;
		}

		/**
		 * Specifies to allow the {@code @@SelfCharged} annotation in the Takamaka
		 * code that runs in the node.
		 * 
		 * @param allowsSelfCharged true if and only if the annotation is allowed
		 * @return this builder
		 */
		public Builder allowSelfCharged(boolean allowsSelfCharged) {
			this.allowsSelfCharged = allowsSelfCharged;

			return this;
		}

		/**
		 * Specifies to allow the {@code faucet()} methods of the gametes without a valid signature.
		 * This is only useful for testing networks, where users can freely fill their accounts at the faucet.
		 * 
		 * @param allowsUnsignedFaucet true if and only if the faucet of the gametes can be used without a valid signature
		 * @return this builder
		 */
		public Builder allowUnsignedFaucet(boolean allowsUnsignedFaucet) {
			this.allowsUnsignedFaucet = allowsUnsignedFaucet;

			return this;
		}

		/**
		 * Specifies to signature algorithm to use to sign the requests sent to the node.
		 * It defaults to "ed25519";
		 * 
		 * @param signature the name of the signature algorithm. Currently, this includes
		 *                  "ed25519", "ed25519det", "sha256dsa", "empty", "qtesla1" and "qtesla3"
		 * @return this builder
		 */
		public Builder signRequestsWith(String signature) {
			if (signature == null)
				throw new NullPointerException("the signature algorithm name cannot be null");

			this.signature = signature;

			return this;
		}

		/**
		 * Sets the maximal amount of gas that a non-view transaction can consume.
		 * It defaults to 1_000_000_000.
		 */
		public Builder setMaxGasPerTransaction(BigInteger maxGasPerTransaction) {
			if (maxGasPerTransaction == null)
				throw new NullPointerException("the maximal amount of gas per transaction cannot be null");

			if (maxGasPerTransaction.signum() <= 0)
				throw new IllegalArgumentException("the maximal amount of gas per transaction must be positive");

			this.maxGasPerTransaction = maxGasPerTransaction;
			return this;
		}

		/**
		 * Sets the units of gas that are aimed to be rewarded at each reward.
		 * If the actual reward is smaller, the price of gas must decrease.
		 * If it is larger, the price of gas must increase.
		 * It defaults to 1_000_000.
		 */
		public Builder setTargetGasAtReward(BigInteger targetGasAtReward) {
			if (targetGasAtReward == null)
				throw new NullPointerException("the target gas at reward cannot be null");

			if (targetGasAtReward.signum() <= 0)
				throw new IllegalArgumentException("the target gas at reward must be positive");

			this.targetGasAtReward = targetGasAtReward;
			return this;
		}

		/**
		 * Sets how quick the gas consumed at previous rewards is forgotten:
		 * 0 means never, 1_000_000 means immediately.
		 * Hence a smaller level means that the latest rewards are heavier
		 * in the determination of the gas price.
		 * It defaults to 50_000.
		 */
		public Builder setOblivion(long oblivion) {
			if (oblivion < 0 || oblivion > 1_000_000L)
				throw new IllegalArgumentException("oblivion must be between 0 and 1_000_000");

			this.oblivion = oblivion;
			return this;
		}

		/**
		 * Sets the inflation applied to the gas consumed by transactions before it gets sent
		 * as reward to the validators. 0 means 0%, 100,000 means 1%,
		 * 10,000,000 means 100%, 20,000,000 means 200% and so on.
		 * Inflation can be negative. For instance, -30,000 means -0.3%.
		 * It defaults to 10,000 (that is, inflation is 0.1% by default).
		 */
		public Builder setInflation(long inflation) {
			this.inflation = inflation;
			return this;
		}

		/**
		 * Specifies that the minimum gas price for transactions is 0, so that the current
		 * gas price is not relevant for the execution of the transactions. It defaults to false.
		 * 
		 * @param ignoresGasPrice true if and only if the minimum gas price must be ignored
		 * @return this builder
		 */
		public Builder ignoreGasPrice(boolean ignoresGasPrice) {
			this.ignoresGasPrice = ignoresGasPrice;
			return this;
		}

		/**
		 * Requires to skip the verification of the classes of the jars installed in the node.
		 * It defaults to false.
		 * 
		 * @param skipsVerification true if and only if the verification must be disabled
		 * @return this builder
		 */
		public Builder skipVerification(boolean skipsVerification) {
			this.skipsVerification = skipsVerification;
			return this;
		}

		/**
		 * Sets the version of the verification module to use.
		 * It defaults to 0.
		 * 
		 * @param verificationVersion the version of the verification module
		 * @return this builder
		 */
		public Builder setVerificationVersion(int verificationVersion) {
			if (verificationVersion < 0)
				throw new IllegalArgumentException("the verification version must be non-negative");

			this.verificationVersion = verificationVersion;
			return this;
		}

		/**
		 * Sets the amount of coins that must be payed to start a new poll amount
		 * to validators, for instance to change a consensus parameter.
		 * It defaults to 100.
		 */
		public Builder setTicketForNewPoll(BigInteger ticketForNewPoll) {
			if (ticketForNewPoll == null)
				throw new NullPointerException("the ticket for a new poll cannot be null");

			if (ticketForNewPoll.signum() < 0)
				throw new IllegalArgumentException("the ticket for new poll must be non-negative");

			this.ticketForNewPoll = ticketForNewPoll;
			return this;
		}
	}
}