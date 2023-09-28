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

package io.hotmoka.node.internal;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.node.api.ConsensusConfig;
import io.hotmoka.node.api.ConsensusConfigBuilder;

/**
 * Implementation of the consensus parameters of a Hotmoka node. This information
 * is typically contained in the manifest of the node.
 * 
 * @param <C> the concrete type of the configuration
 * @param <B> the concrete type of the builder
 */
@Immutable
public abstract class ConsensusConfigImpl<C extends ConsensusConfig<C,B>, B extends ConsensusConfigBuilder<C,B>> implements ConsensusConfig<C,B> {

	/**
	 * The genesis time, UTC. It defaults to the time of
	 * construction of the builder of this object.
	 */
	public final LocalDateTime genesisTime;

	/**
	 * The chain identifier of the node.
	 */
	public final String chainId;

	/**
	 * The maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 */
	public final long maxErrorLength;

	/**
	 * The maximal number of dependencies in the classpath of a transaction.
	 */
	public final long maxDependencies;

	/**
	 * The maximal cumulative size (in bytes) of the instrumented jars of the dependencies
	 * of a transaction.
	 */
	public final long maxCumulativeSizeOfDependencies;

	/**
	 * True if and only if the use of the {@code @@SelfCharged} annotation is allowed.
	 */
	public final boolean allowsSelfCharged;

	/**
	 * True if and only if the use of the faucet of the gamete is allowed without a valid signature.
	 * It defaults to false.
	 */
	public final boolean allowsUnsignedFaucet;

	/**
	 * True if and only if the gamete of the node can call, for free, the add method of the accounts ledger
	 * and the mint/burn methods of the accounts, without paying gas and without paying for the minted coins.
	 */
	public final boolean allowsMintBurnFromGamete;

	/**
	 * True if and only if the static verification of the classes of the jars installed in the node must be skipped.
	 * It defaults to false.
	 */
	public final boolean skipsVerification;

	/**
	 * The Base64-encoded public key of the gamete account.
	 */
	public final String publicKeyOfGamete;

	/**
	 * The initial gas price. It defaults to 100.
	 */
	public final BigInteger initialGasPrice;

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
	 * A value of 0 means that the gas price is constant.
	 * It defaults to 250_000L.
	 */
	public final long oblivion;

	/**
	 * The initial inflation applied to the gas consumed by transactions before it gets sent
	 * as reward to the validators. 1,000,000 means 1%.
	 * Inflation can be negative. For instance, -300,000 means -0.3%.
	 * This defaults to 10,000 (that is, inflation is 0.1% by default).
	 */
	public final long initialInflation;

	/**
	 * The version of the verification module to use. It defaults to 0.
	 */
	public final long verificationVersion;

	/**
	 * The initial supply of coins in the node.
	 */
	public final BigInteger initialSupply;

	/**
	 * The final supply of coins in the node. Once the current supply reaches
	 * this final amount, it remains constant.
	 */
	public final BigInteger finalSupply;

	/**
	 * The initial supply of red coins in the node.
	 */
	public final BigInteger initialRedSupply;

	/**
	 * The amount of coin to pay to start a new poll amount the validators,
	 * for instance in order to change a consensus parameter.
	 */
	public final BigInteger ticketForNewPoll;

	/**
	 * The signature algorithm for signing requests. It defaults to "ed25519".
	 */
	public final SignatureAlgorithm signature;

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param builder the builder where information is extracted from
	 */
	protected ConsensusConfigImpl(ConsensusConfigBuilderImpl<C,B> builder) {
		this.genesisTime = builder.genesisTime;
		this.chainId = builder.chainId;
		this.maxErrorLength = builder.maxErrorLength;
		this.maxDependencies = builder.maxDependencies;
		this.maxCumulativeSizeOfDependencies = builder.maxCumulativeSizeOfDependencies;
		this.allowsSelfCharged = builder.allowsSelfCharged;
		this.allowsUnsignedFaucet = builder.allowsUnsignedFaucet;
		this.allowsMintBurnFromGamete = builder.allowsMintBurnFromGamete;
		this.initialGasPrice = builder.initialGasPrice;
		this.maxGasPerTransaction = builder.maxGasPerTransaction;
		this.ignoresGasPrice = builder.ignoresGasPrice;
		this.skipsVerification = builder.skipsVerification;
		this.targetGasAtReward = builder.targetGasAtReward;
		this.oblivion = builder.oblivion;
		this.initialInflation = builder.initialInflation;
		this.verificationVersion = builder.verificationVersion;
		this.ticketForNewPoll = builder.ticketForNewPoll;
		this.initialSupply = builder.initialSupply;
		this.finalSupply = builder.finalSupply;
		this.initialRedSupply = builder.initialRedSupply;
		this.publicKeyOfGamete = builder.publicKeyOfGamete;
		this.signature = builder.signature;
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && getClass() == other.getClass()) {
			var otherConfig = (ConsensusConfigImpl<?,?>) other;
			return genesisTime.equals(otherConfig.genesisTime) &&
				chainId.equals(otherConfig.chainId) &&
				maxErrorLength == otherConfig.maxErrorLength &&
				maxDependencies == otherConfig.maxDependencies &&
				maxCumulativeSizeOfDependencies == otherConfig.maxCumulativeSizeOfDependencies &&
				allowsSelfCharged == otherConfig.allowsSelfCharged &&
				allowsUnsignedFaucet == otherConfig.allowsUnsignedFaucet &&
				allowsMintBurnFromGamete == otherConfig.allowsMintBurnFromGamete &&
				initialGasPrice.equals(otherConfig.initialGasPrice) &&
				maxGasPerTransaction.equals(otherConfig.maxGasPerTransaction) &&
				ignoresGasPrice == otherConfig.ignoresGasPrice &&
				skipsVerification == otherConfig.skipsVerification &&
				targetGasAtReward.equals(otherConfig.targetGasAtReward) &&
				oblivion == otherConfig.oblivion &&
				initialInflation == otherConfig.initialInflation &&
				verificationVersion == otherConfig.verificationVersion &&
				ticketForNewPoll.equals(otherConfig.ticketForNewPoll) &&
				initialSupply.equals(otherConfig.initialSupply) &&
				finalSupply.equals(otherConfig.finalSupply) &&
				initialRedSupply.equals(otherConfig.initialRedSupply) &&
				publicKeyOfGamete.equals(otherConfig.publicKeyOfGamete) &&
				signature.equals(otherConfig.signature);
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return toToml();
	}

	@Override
	public String toToml() {
		var sb = new StringBuilder();

		sb.append("# This is a TOML config file for the consensus of a Hotmoka network.\n");
		sb.append("# For more information about TOML, see https://github.com/toml-lang/toml\n");
		sb.append("# For more information about Hotmoka, see https://www.hotmoka.io\n");
		sb.append("\n");
		sb.append("# the genesis time, UTC, in ISO8601 format\n");
		sb.append("genesis_time = \"" + genesisTime + "\"\n");
		sb.append("\n");
		sb.append("# the chain identifier of the node\n");
		sb.append("chain_id = \"" + chainId + "\"\n");
		sb.append("\n");
		sb.append("# the maximal length of the error message kept in the store of the node\n");
		sb.append("max_error_length = " + maxErrorLength + "\n");
		sb.append("\n");
		sb.append("# the maximal number of dependencies in the classpath of a transaction\n");
		sb.append("max_dependencies = " + maxDependencies + "\n");
		sb.append("\n");
		sb.append("# the maximal cumulative size (in bytes) of the instrumented jars\n");
		sb.append("# of the dependencies of a transaction\n");
		sb.append("max_cumulative_size_of_dependencies = " + maxCumulativeSizeOfDependencies + "\n");
		sb.append("\n");
		sb.append("# true if and only if the use of the @SelfCharged annotation is allowed\n");
		sb.append("allows_self_charged = " + allowsSelfCharged + "\n");
		sb.append("\n");
		sb.append("# true if and only if the use of the faucet of the gamete is allowed without a valid signature\n");
		sb.append("allows_unsigned_faucet = " + allowsUnsignedFaucet + "\n");
		sb.append("\n");
		sb.append("# true if and only if the gamete of the node can call, for free, the add method of the accounts ledger\n");
		sb.append("# and the mint/burn methods of the accounts, without paying gas and without paying for the minted coins\n");
		sb.append("allows_mint_burn_from_gamete = " + allowsMintBurnFromGamete + "\n");
		sb.append("\n");
		sb.append("# true if and only if the static verification of the classes of the jars installed in the node must be skipped\n");
		sb.append("skips_verification = " + skipsVerification + "\n");
		sb.append("\n");
		sb.append("# the Base64-encoded public key of the gamete account\n");
		sb.append("public_key_of_gamete = \"" + publicKeyOfGamete + "\"\n");
		sb.append("\n");
		sb.append("# the initial gas price\n");
		sb.append("initial_gas_price = \"" + initialGasPrice + "\"\n");
		sb.append("\n");
		sb.append("# the maximal amount of gas that a non-view transaction can consume\n");
		sb.append("max_gas_per_transaction = \"" + maxGasPerTransaction + "\"\n");
		sb.append("\n");
		sb.append("# true if and only if the node ignores the minimum gas price;\n");
		sb.append("# hence requests that specify a lower gas price than the current gas price of the node are executed anyway;\n");
		sb.append("# this is mainly useful for testing\n");
		sb.append("ignores_gas_price = " + ignoresGasPrice + "\n");
		sb.append("\n");
		sb.append("# the units of gas that are aimed to be rewarded at each reward;\n");
		sb.append("# if the actual reward is smaller, the price of gas must decrease;\n");
		sb.append("# if it is larger, the price of gas must increase\n");
		sb.append("target_gas_at_reward = \"" + targetGasAtReward + "\"\n");
		sb.append("\n");
		sb.append("# how quick the gas consumed at previous rewards is forgotten:\n");
		sb.append("# 0 means never, 1000000 means immediately;\n");
		sb.append("# hence a smaller level means that the latest rewards are heavier\n");
		sb.append("# in the determination of the gas price;\n");
		sb.append("# a value of 0 means that the gas price is constant\n");
		sb.append("oblivion = " + oblivion + "\n");
		sb.append("\n");
		sb.append("# the initial inflation applied to the gas consumed by transactions\n");
		sb.append("# before it gets sent as reward to the validators. 1000000 means 1%;\n");
		sb.append("# inflation can be negative. For instance, -300,000 means -0.3%\n");
		sb.append("initial_inflation = " + initialInflation + "\n");
		sb.append("\n");
		sb.append("# the version of the verification module to use\n");
		sb.append("verification_version = " + verificationVersion + "\n");
		sb.append("\n");
		sb.append("# the initial supply of coins in the node\n");
		sb.append("initial_supply = \"" + initialSupply + "\"\n");
		sb.append("\n");
		sb.append("# the final supply of coins in the node; once the current supply reaches\n");
		sb.append("# this final amount, it remains constant\n");
		sb.append("final_supply = \"" + finalSupply + "\"\n");
		sb.append("\n");
		sb.append("# the initial supply of red coins in the node\n");
		sb.append("initial_red_supply = \"" + initialRedSupply + "\"\n");
		sb.append("\n");
		sb.append("# the amount of coin to pay to start a new poll amount the validators,\n");
		sb.append("# for instance in order to change a consensus parameter\n");
		sb.append("ticket_for_new_poll = \"" + ticketForNewPoll + "\"\n");
		sb.append("\n");
		sb.append("# the name of the signature algorithm for signing requests\n");
		sb.append("signature = \"" + signature + "\"\n");

		return sb.toString();
	}

	@Override
	public LocalDateTime getGenesisTime() {
		return genesisTime;
	}

	@Override
	public String getChainId() {
		return chainId;
	}

	@Override
	public long getMaxErrorLength() {
		return maxErrorLength;
	}

	@Override
	public long getMaxDependencies() {
		return maxDependencies;
	}

	@Override
	public long getMaxCumulativeSizeOfDependencies() {
		return maxCumulativeSizeOfDependencies;
	}

	@Override
	public boolean allowsSelfCharged() {
		return allowsSelfCharged;
	}

	@Override
	public boolean allowsUnsignedFaucet() {
		return allowsUnsignedFaucet;
	}

	@Override
	public boolean allowsMintBurnFromGamete() {
		return allowsMintBurnFromGamete;
	}

	@Override
	public boolean skipsVerification() {
		return skipsVerification;
	}

	@Override
	public String getPublicKeyOfGamete() {
		return publicKeyOfGamete;
	}

	@Override
	public BigInteger getInitialGasPrice() {
		return initialGasPrice;
	}

	@Override
	public BigInteger getMaxGasPerTransaction() {
		return maxGasPerTransaction;
	}

	@Override
	public boolean ignoresGasPrice() {
		return ignoresGasPrice;
	}

	@Override
	public BigInteger getTargetGasAtReward() {
		return targetGasAtReward;
	}

	@Override
	public long getOblivion() {
		return oblivion;
	}

	@Override
	public long getInitialInflation() {
		return initialInflation;
	}

	@Override
	public long getVerificationVersion() {
		return verificationVersion;
	}

	@Override
	public BigInteger getInitialSupply() {
		return initialSupply;
	}

	@Override
	public BigInteger getFinalSupply() {
		return finalSupply;
	}

	@Override
	public BigInteger getInitialRedSupply() {
		return initialRedSupply;
	}

	@Override
	public BigInteger getTicketForNewPoll() {
		return ticketForNewPoll;
	}

	@Override
	public SignatureAlgorithm getSignature() {
		return signature;
	}

	/**
	 * The builder of a configuration object.
	 * 
	 * @param <T> the concrete type of the builder
	 */
	public abstract static class ConsensusConfigBuilderImpl<C extends ConsensusConfig<C,B>, B extends ConsensusConfigBuilder<C,B>> implements ConsensusConfigBuilder<C,B> {
		private String chainId = "";
		private LocalDateTime genesisTime = LocalDateTime.now(ZoneId.of("UTC"));
		private long maxErrorLength = 300L;
		private boolean allowsSelfCharged = false;
		private boolean allowsUnsignedFaucet = false;
		private boolean allowsMintBurnFromGamete = false;
		private SignatureAlgorithm signature;
		private BigInteger maxGasPerTransaction = BigInteger.valueOf(1_000_000_000L);
		private long maxDependencies = 20;
		private long maxCumulativeSizeOfDependencies = 10_000_000L;
		private BigInteger initialGasPrice = BigInteger.valueOf(100L);
		private boolean ignoresGasPrice = false;
		private boolean skipsVerification = false;
		private BigInteger targetGasAtReward = BigInteger.valueOf(1_000_000L);
		private long oblivion = 250_000L;
		private long initialInflation = 100_000L; // 0.1%
		private long verificationVersion = 0L;
		private BigInteger initialSupply = BigInteger.ZERO;
		private BigInteger finalSupply = BigInteger.ZERO;
		private BigInteger initialRedSupply = BigInteger.ZERO;
		private String publicKeyOfGamete = "";
		private BigInteger ticketForNewPoll = BigInteger.valueOf(100);

		/**
		 * Creates a builder with default values for the properties.
		 * 
		 * @throws NoSuchAlgorithmException if some signature algorithm is not available
		 */
		protected ConsensusConfigBuilderImpl() throws NoSuchAlgorithmException {
			this(SignatureAlgorithms.ed25519());
		}

		/**
		 * Creates a builder with default values for the properties, except for the signature algorithm.
		 * 
		 * @param signature the signature algorithm to store in the builder
		 */
		protected ConsensusConfigBuilderImpl(SignatureAlgorithm signature) {
			this.signature = signature;
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		protected ConsensusConfigBuilderImpl(ConsensusConfig<C,B> config) {
			setChainId(config.getChainId());
			setGenesisTime(config.getGenesisTime());
			setMaxErrorLength(config.getMaxErrorLength());
			setMaxDependencies(config.getMaxDependencies());
			setMaxCumulativeSizeOfDependencies(config.getMaxCumulativeSizeOfDependencies());
			allowSelfCharged(config.allowsSelfCharged());
			allowUnsignedFaucet(config.allowsUnsignedFaucet());
			allowMintBurnFromGamete(config.allowsMintBurnFromGamete());
			signRequestsWith(config.getSignature());
			setMaxGasPerTransaction(config.getMaxGasPerTransaction());
			setInitialGasPrice(config.getInitialGasPrice());
			ignoreGasPrice(config.ignoresGasPrice());
			skipVerification(config.skipsVerification());
			setTargetGasAtReward(config.getTargetGasAtReward());
			setOblivion(config.getOblivion());
			setInitialInflation(config.getInitialInflation());
			setVerificationVersion(config.getVerificationVersion());
			setInitialSupply(config.getInitialSupply());
			setFinalSupply(config.getFinalSupply());
			setInitialRedSupply(config.getInitialRedSupply());
			setPublicKeyOfGamete(config.getPublicKeyOfGamete());
			setTicketForNewPoll(config.getTicketForNewPoll());
		}

		/**
		 * Reads the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws NoSuchAlgorithmException if some signature algorithm in the TOML file is not available
		 */
		protected ConsensusConfigBuilderImpl(Toml toml) throws NoSuchAlgorithmException {
			var genesisTime = toml.getString("genesis_time");
			if (genesisTime != null)
				setGenesisTime(LocalDateTime.parse(genesisTime, DateTimeFormatter.ISO_DATE_TIME));

			var chainId = toml.getString("chain_id");
			if (chainId != null)
				setChainId(chainId);

			var maxErrorLength = toml.getLong("max_error_length");
			if (maxErrorLength != null)
				setMaxErrorLength(maxErrorLength);

			var maxDependencies = toml.getLong("max_dependencies");
			if (maxDependencies != null)
				setMaxDependencies(maxDependencies);

			var maxCumulativeSizeOfDependencies = toml.getLong("max_cumulative_size_of_dependencies");
			if (maxCumulativeSizeOfDependencies != null)
				setMaxCumulativeSizeOfDependencies(maxCumulativeSizeOfDependencies);

			var allowsSelfCharged = toml.getBoolean("allows_self_charged");
			if (allowsSelfCharged != null)
				allowSelfCharged(allowsSelfCharged);

			var allowsUnsignedFaucet = toml.getBoolean("allows_unsigned_faucet");
			if (allowsUnsignedFaucet != null)
				allowUnsignedFaucet(allowsUnsignedFaucet);

			var allowsMintBurnFromGamete = toml.getBoolean("allows_mint_burn_from_gamete");
			if (allowsMintBurnFromGamete != null)
				allowMintBurnFromGamete(allowsMintBurnFromGamete);

			var signature = toml.getString("signature");
			if (signature != null)
				signRequestsWith(SignatureAlgorithms.of(signature));

			var maxGasPerTransaction = toml.getString("max_gas_per_transaction");
			if (maxGasPerTransaction != null)
				setMaxGasPerTransaction(new BigInteger(maxGasPerTransaction));

			var initialGasPrice = toml.getString("initial_gas_price");
			if (initialGasPrice != null)
				setInitialGasPrice(new BigInteger(initialGasPrice));

			var ignoresGasPrice = toml.getBoolean("ignores_gas_price");
			if (ignoresGasPrice != null)
				ignoreGasPrice(ignoresGasPrice);

			var skipsVerification = toml.getBoolean("skips_verification");
			if (skipsVerification != null)
				skipVerification(skipsVerification);

			var targetGasAtReward = toml.getString("target_gas_at_reward");
			if (targetGasAtReward != null)
				setTargetGasAtReward(new BigInteger(targetGasAtReward));

			var oblivion = toml.getLong("oblivion");
			if (oblivion != null)
				setOblivion(oblivion);

			var initialInflation = toml.getLong("initial_inflation");
			if (initialInflation != null)
				setInitialInflation(initialInflation);

			var verificationVersion = toml.getLong("verification_version");
			if (verificationVersion != null)
				setVerificationVersion(verificationVersion);

			var initialSupply = toml.getString("initial_supply");
			if (initialSupply != null)
				setInitialSupply(new BigInteger(initialSupply));

			var finalSupply = toml.getString("final_supply");
			if (finalSupply != null)
				setFinalSupply(new BigInteger(finalSupply));

			var initialRedSupply = toml.getString("initial_red_supply");
			if (initialRedSupply != null)
				setInitialRedSupply(new BigInteger(initialRedSupply));

			var ticketForNewPoll = toml.getString("ticket_for_new_poll");
			if (ticketForNewPoll != null)
				setTicketForNewPoll(new BigInteger(ticketForNewPoll));

			var publicKeyOfGamete = toml.getString("public_key_of_gamete");
			if (publicKeyOfGamete != null)
				setPublicKeyOfGamete(publicKeyOfGamete);
		}

		@Override
		public B setGenesisTime(LocalDateTime genesisTime) {
			this.genesisTime = genesisTime;
			return getThis();
		}

		@Override
		public B setChainId(String chainId) {
			this.chainId = chainId;
			return getThis();
		}

		@Override
		public B setMaxErrorLength(long maxErrorLength) {
			this.maxErrorLength = maxErrorLength;
			return getThis();
		}

		@Override
		public B setMaxDependencies(long maxDependencies) {
			this.maxDependencies = maxDependencies;
			return getThis();
		}

		@Override
		public B setMaxCumulativeSizeOfDependencies(long maxCumulativeSizeOfDependencies) {
			this.maxCumulativeSizeOfDependencies = maxCumulativeSizeOfDependencies;
			return getThis();
		}

		@Override
		public B allowSelfCharged(boolean allowsSelfCharged) {
			this.allowsSelfCharged = allowsSelfCharged;
			return getThis();
		}

		@Override
		public B allowUnsignedFaucet(boolean allowsUnsignedFaucet) {
			this.allowsUnsignedFaucet = allowsUnsignedFaucet;
			return getThis();
		}

		@Override
		public B allowMintBurnFromGamete(boolean allowsMintBurnFromGamete) {
			this.allowsMintBurnFromGamete = allowsMintBurnFromGamete;
			return getThis();
		}

		@Override
		public B signRequestsWith(SignatureAlgorithm signature) {
			Objects.requireNonNull(signature, "The signature algorithm cannot be null");
			this.signature = signature;
			return getThis();
		}

		@Override
		public B setInitialGasPrice(BigInteger initialGasPrice) {
			Objects.requireNonNull(initialGasPrice, "The initial gas price cannot be null");

			if (initialGasPrice.signum() <= 0)
				throw new IllegalArgumentException("The initial gas price must be positive");

			this.initialGasPrice = initialGasPrice;
			return getThis();
		}

		@Override
		public B setMaxGasPerTransaction(BigInteger maxGasPerTransaction) {
			Objects.requireNonNull(maxGasPerTransaction, "The maximal amount of gas per transaction cannot be null");

			if (maxGasPerTransaction.signum() <= 0)
				throw new IllegalArgumentException("The maximal amount of gas per transaction must be positive");

			this.maxGasPerTransaction = maxGasPerTransaction;
			return getThis();
		}

		@Override
		public B setTargetGasAtReward(BigInteger targetGasAtReward) {
			Objects.requireNonNull(targetGasAtReward, "The target gas at reward cannot be null");

			if (targetGasAtReward.signum() <= 0)
				throw new IllegalArgumentException("The target gas at reward must be positive");

			this.targetGasAtReward = targetGasAtReward;
			return getThis();
		}

		@Override
		public B setOblivion(long oblivion) {
			if (oblivion < 0 || oblivion > 1_000_000L)
				throw new IllegalArgumentException("Oblivion must be between 0 and 1_000_000");

			this.oblivion = oblivion;
			return getThis();
		}

		@Override
		public B setInitialInflation(long initialInflation) {
			this.initialInflation = initialInflation;
			return getThis();
		}

		@Override
		public B ignoreGasPrice(boolean ignoresGasPrice) {
			this.ignoresGasPrice = ignoresGasPrice;
			return getThis();
		}

		@Override
		public B skipVerification(boolean skipsVerification) {
			this.skipsVerification = skipsVerification;
			return getThis();
		}

		@Override
		public B setVerificationVersion(long verificationVersion) {
			if (verificationVersion < 0L)
				throw new IllegalArgumentException("The verification version must be non-negative");

			this.verificationVersion = verificationVersion;
			return getThis();
		}

		@Override
		public B setInitialSupply(BigInteger initialSupply) {
			Objects.requireNonNull(initialSupply, "The initial supply cannot be null");

			if (initialSupply.signum() < 0)
				throw new IllegalArgumentException("The initial supply must be non-negative");

			this.initialSupply = initialSupply;
			return getThis();
		}

		@Override
		public B setInitialRedSupply(BigInteger initialRedSupply) {
			Objects.requireNonNull(initialRedSupply, "The initial red supply cannot be null");

			if (initialRedSupply.signum() < 0)
				throw new IllegalArgumentException("The initial red supply must be non-negative");

			this.initialRedSupply = initialRedSupply;
			return getThis();
		}

		@Override
		public B setPublicKeyOfGamete(String publicKeyOfGamete) {
			Objects.requireNonNull(publicKeyOfGamete, "The public key of the gamete cannot be null");
			this.publicKeyOfGamete = publicKeyOfGamete;
			return getThis();
		}

		@Override
		public B setFinalSupply(BigInteger finalSupply) {
			Objects.requireNonNull(finalSupply, "The final supply cannot be null");

			if (finalSupply.signum() < 0)
				throw new IllegalArgumentException("The final supply must be non-negative");

			this.finalSupply = finalSupply;
			return getThis();
		}

		@Override
		public B setTicketForNewPoll(BigInteger ticketForNewPoll) {
			Objects.requireNonNull(ticketForNewPoll, "The ticket for a new poll cannot be null");

			if (ticketForNewPoll.signum() < 0)
				throw new IllegalArgumentException("The ticket for new poll must be non-negative");

			this.ticketForNewPoll = ticketForNewPoll;
			return getThis();
		}
	
		/**
		 * Standard design pattern. See http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ205
		 * 
		 * @return this same builder
		 */
		protected abstract B getThis();
	
		/**
		 * Loads the TOML file at the given path.
		 * 
		 * @param path the path
		 * @return the file
		 * @throws FileNotFoundException if {@code path} cannot be found
		 */
		protected static Toml readToml(Path path) throws FileNotFoundException {
			try {
				return new Toml().read(path.toFile());
			}
			catch (RuntimeException e) {
				// the toml4j library wraps the FileNotFoundException inside a RuntimeException...
				Throwable cause = e.getCause();
				if (cause instanceof FileNotFoundException)
					throw (FileNotFoundException) cause;
				else
					throw e;
			}
		}
	}
}