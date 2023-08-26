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

package io.hotmoka.nodes.internal;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.nio.file.Path;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.nodes.api.ConsensusConfig;

/**
 * Implementation of the consensus parameters of a Hotmoka node. This information
 * is typically contained in the manifest of the node.
 */
@Immutable
public class ConsensusConfigImpl implements ConsensusConfig {

	/**
	 * The genesis time, UTC, in ISO8601 pattern.
	 */
	public final String genesisTime;

	/**
	 * The chain identifier of the node.
	 */
	public final String chainId;

	/**
	 * The maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 */
	public final int maxErrorLength;

	/**
	 * The maximal number of dependencies in the classpath of a transaction.
	 */
	public final int maxDependencies;

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
	public final int verificationVersion;

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
	 * The name of the signature algorithm for signing requests. It defaults to "ed25519".
	 */
	public final String signature;

	/**
	 * The amount of validators' rewards that gets staked. The rest is sent to the validators immediately.
	 * 1000000 = 1%. It defaults to 75%.
	 */
	public final int percentStaked;

	/**
	 * Extra tax paid when a validator acquires the shares of another validator
	 * (in percent of the offer cost). 1000000 = 1%. It defaults to 50%.
	 */
	public final int buyerSurcharge;

	/**
	 * The percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%.
	 * It defaults to 1%.
	 */
	public final int slashingForMisbehaving;

	/**
	 * The percent of stake that gets slashed for validators that do not behave
	 * (or do not vote). 1000000 means 1%. It defaults to 0.5%.
	 */
	public final int slashingForNotBehaving;

	/**
	 * Full constructor for the builder pattern.
	 * 
	 * @param builder the builder where information is extracted from
	 */
	public ConsensusConfigImpl(AbstractBuilder<?> builder) {
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
		this.percentStaked = builder.percentStaked;
		this.buyerSurcharge = builder.buyerSurcharge;
		this.slashingForMisbehaving = builder.slashingForMisbehaving;
		this.slashingForNotBehaving = builder.slashingForNotBehaving;
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && getClass() == other.getClass()) {
			var otherConfig = (ConsensusConfigImpl) other;
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
				signature.equals(otherConfig.signature) &&
				percentStaked == otherConfig.percentStaked &&
				buyerSurcharge == otherConfig.buyerSurcharge &&
				slashingForMisbehaving == otherConfig.slashingForMisbehaving &&
				slashingForNotBehaving == otherConfig.slashingForNotBehaving;
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
		StringBuilder sb = new StringBuilder();
		sb.append("# This is a TOML config file for Hotmoka nodes.\n");
		sb.append("# For more information about TOML, see https://github.com/toml-lang/toml\n");
		sb.append("# For more information about Hotmoka, see https://www.hotmoka.io\n");
		sb.append("\n");
		sb.append("## Consensus parameters\n");
		sb.append("\n");
		sb.append("# the genesis time, UTC, in ISO8601 pattern\n");
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
		sb.append("\n");
		sb.append("# the amount of validators' rewards that gets staked. The rest is sent to the validators\n");
		sb.append("# immediately. 1000000 means 1%\n");
		sb.append("percent_staked = " + percentStaked + "\n");
		sb.append("\n");
		sb.append("# extra tax paid when a validator acquires the shares of another validator\n");
		sb.append("# (in percent of the offer cost). 1000000 means 1%\n");
		sb.append("buyer_surcharge = " + buyerSurcharge + "\n");
		sb.append("\n");
		sb.append("# the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%\n");
		sb.append("slashing_for_misbehaving = " + slashingForMisbehaving + "\n");
		sb.append("\n");
		sb.append("# the percent of stake that gets slashed for each validator that does not behave\n");
		sb.append("# (or does not vote). 1000000 means 1%\n");
		sb.append("slashing_for_not_behaving = " + slashingForNotBehaving + "\n");

		return sb.toString();
	}

	@Override
	public String getGenesisTime() {
		return genesisTime;
	}

	@Override
	public String getChainId() {
		return chainId;
	}

	@Override
	public int getMaxErrorLength() {
		return maxErrorLength;
	}

	@Override
	public int getMaxDependencies() {
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
	public int getVerificationVersion() {
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
	public String getSignature() {
		return signature;
	}

	@Override
	public int getPercentStaked() {
		return percentStaked;
	}

	@Override
	public int getBuyerSurcharge() {
		return buyerSurcharge;
	}

	@Override
	public int getSlashingForMisbehaving() {
		return slashingForMisbehaving;
	}

	@Override
	public int getSlashingForNotBehaving() {
		return slashingForNotBehaving;
	}

	/**
	 * The builder of a configuration object.
	 * 
	 * @param <T> the concrete type of the builder
	 */
	public abstract static class AbstractBuilder<T extends AbstractBuilder<T>> {
		private String chainId = "";
		private String genesisTime = "";
		private int maxErrorLength = 300;
		private boolean allowsSelfCharged = false;
		private boolean allowsUnsignedFaucet = false;
		private boolean allowsMintBurnFromGamete = false;
		private String signature = "ed25519";
		private BigInteger maxGasPerTransaction = BigInteger.valueOf(1_000_000_000L);
		private int maxDependencies = 20;
		private long maxCumulativeSizeOfDependencies = 10_000_000L;
		private BigInteger initialGasPrice = BigInteger.valueOf(100L);
		private boolean ignoresGasPrice = false;
		private boolean skipsVerification = false;
		private BigInteger targetGasAtReward = BigInteger.valueOf(1_000_000L);
		private long oblivion = 250_000L;
		private long initialInflation = 100_000L; // 0.1%
		private int verificationVersion = 0;
		private BigInteger initialSupply = BigInteger.ZERO;
		private BigInteger finalSupply = BigInteger.ZERO;
		private BigInteger initialRedSupply = BigInteger.ZERO;
		private String publicKeyOfGamete = "";
		private BigInteger ticketForNewPoll = BigInteger.valueOf(100);
		private int percentStaked = 75_000_000;
		private int buyerSurcharge = 50_000_000;
		private int slashingForMisbehaving = 1_000_000;
		private int slashingForNotBehaving = 500_000;
	
		protected AbstractBuilder() {}
	
		/**
		 * Reads the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 */
		protected AbstractBuilder(Toml toml) {
			var genesisTime = toml.getString("genesis_time");
			if (genesisTime != null)
				setGenesisTime(genesisTime);

			var chainId = toml.getString("chain_id");
			if (chainId != null)
				setChainId(chainId);

			// TODO: remove all type conversions below
			var maxErrorLength = toml.getLong("max_error_length");
			if (maxErrorLength != null)
				setMaxErrorLength((int) (long) maxErrorLength);

			var maxDependencies = toml.getLong("max_dependencies");
			if (maxDependencies != null)
				setMaxDependencies((int) (long) maxDependencies);

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
				signRequestsWith(signature);

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
				setVerificationVersion((int) (long) verificationVersion);

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

			var percentStaked = toml.getLong("percent_staked");
			if (percentStaked != null)
				setPercentStaked((int) (long) percentStaked);

			var buyerSurcharge = toml.getLong("buyer_surcharge");
			if (buyerSurcharge != null)
				setBuyerSurcharge((int) (long) buyerSurcharge);

			var slashingForMisbehaving = toml.getLong("slashing_for_misbehaving");
			if (slashingForMisbehaving != null)
				setSlashingForMisbehaving((int) (long) slashingForMisbehaving);

			var slashingForNotBehaving = toml.getLong("slashing_for_not_behaving");
			if (slashingForNotBehaving != null)
				setSlashingForNotBehaving((int) (long) slashingForNotBehaving);
		}
	
		/**
		 * Sets the genesis time, UTC, in ISO8601 pattern.
		 * 
		 * @param genesisTime the genesis time, UTC, in ISO08601 pattern
		 * @return this builder
		 */
		public T setGenesisTime(String genesisTime) {
			this.genesisTime = genesisTime;
			return getThis();
		}

		/**
		 * Sets the chain identifier of the node.
		 * 
		 * @param chainId the chain identifier
		 * @return this builder
		 */
		public T setChainId(String chainId) {
			this.chainId = chainId;
			return getThis();
		}

		/**
		 * Sets the maximal length of the error message kept in the store of the node.
		 * Beyond this threshold, the message gets truncated.
		 * 
		 * @param maxErrorLength the maximal length of the error message kept in the store of the node
		 * @return this builder
		 */
		public T setMaxErrorLength(int maxErrorLength) {
			this.maxErrorLength = maxErrorLength;
			return getThis();
		}

		/**
		 * Sets the maximal number of dependencies in the classpath of a transaction.
		 * 
		 * @param maxDependencies the maximal number of dependencies in the classpath of a transaction
		 * @return this builder
		 */
		public T setMaxDependencies(int maxDependencies) {
			this.maxDependencies = maxDependencies;
			return getThis();
		}

		/**
		 * Sets the maximal cumulative size (in bytes) of the instrumented jars of the dependencies
		 * of a transaction.
		 * 
		 * @param maxCumulativeSizeOfDependencies the maximal cumulative size (in bytes) of the instrumented
		 *                                        jars of the dependencies of a transaction
		 * @return this builder
		 */
		public T setMaxCumulativeSizeOfDependencies(long maxCumulativeSizeOfDependencies) {
			this.maxCumulativeSizeOfDependencies = maxCumulativeSizeOfDependencies;
			return getThis();
		}

		/**
		 * Specifies to allow the {@code @@SelfCharged} annotation in the Takamaka
		 * code that runs in the node.
		 * 
		 * @param allowsSelfCharged true if and only if the annotation is allowed
		 * @return this builder
		 */
		public T allowSelfCharged(boolean allowsSelfCharged) {
			this.allowsSelfCharged = allowsSelfCharged;
			return getThis();
		}

		/**
		 * Specifies to allow the {@code faucet()} methods of the gametes without a valid signature.
		 * This is only useful for testing networks, where users can freely fill their accounts at the faucet.
		 * 
		 * @param allowsUnsignedFaucet true if and only if the faucet of the gametes can be used without a valid signature
		 * @return this builder
		 */
		public T allowUnsignedFaucet(boolean allowsUnsignedFaucet) {
			this.allowsUnsignedFaucet = allowsUnsignedFaucet;
			return getThis();
		}

		/**
		 * Specifies to allow the gamete of the node to call, for free, the add method of the accounts ledger
		 * and the mint/burn methods of the accounts, without paying gas and without paying for the minted coins.
		 * 
		 * @param allowsMintBurnFromGamete true if and only if the gamete is allowed
		 * @return this builder
		 */
		public T allowMintBurnFromGamete(boolean allowsMintBurnFromGamete) {
			this.allowsMintBurnFromGamete = allowsMintBurnFromGamete;
			return getThis();
		}

		/**
		 * Specifies to signature algorithm to use to sign the requests sent to the node.
		 * It defaults to "ed25519";
		 * 
		 * @param signature the name of the signature algorithm. Currently, this includes
		 *                  "ed25519", "ed25519det", "sha256dsa", "empty", "qtesla1" and "qtesla3"
		 * @return this builder
		 */
		public T signRequestsWith(String signature) {
			if (signature == null)
				throw new NullPointerException("the signature algorithm name cannot be null");

			this.signature = signature;
			return getThis();
		}

		/**
		 * Sets the initial gas price. It defaults to 100.
		 * 
		 * @param initialGasPrice the initial gas price to set.
		 * @return this builder
		 */
		public T setInitialGasPrice(BigInteger initialGasPrice) {
			if (initialGasPrice == null)
				throw new NullPointerException("the initial gas price cannot be null");

			if (initialGasPrice.signum() <= 0)
				throw new IllegalArgumentException("the initial gas price must be positive");

			this.initialGasPrice = initialGasPrice;
			return getThis();
		}

		/**
		 * Sets the maximal amount of gas that a non-view transaction can consume.
		 * It defaults to 1_000_000_000.
		 * 
		 * @return this builder
		 */
		public T setMaxGasPerTransaction(BigInteger maxGasPerTransaction) {
			if (maxGasPerTransaction == null)
				throw new NullPointerException("the maximal amount of gas per transaction cannot be null");

			if (maxGasPerTransaction.signum() <= 0)
				throw new IllegalArgumentException("the maximal amount of gas per transaction must be positive");

			this.maxGasPerTransaction = maxGasPerTransaction;
			return getThis();
		}

		/**
		 * Sets the units of gas that are aimed to be rewarded at each reward.
		 * If the actual reward is smaller, the price of gas must decrease.
		 * If it is larger, the price of gas must increase.
		 * It defaults to 1_000_000.
		 * 
		 * @return this builder
		 */
		public T setTargetGasAtReward(BigInteger targetGasAtReward) {
			if (targetGasAtReward == null)
				throw new NullPointerException("the target gas at reward cannot be null");

			if (targetGasAtReward.signum() <= 0)
				throw new IllegalArgumentException("the target gas at reward must be positive");

			this.targetGasAtReward = targetGasAtReward;
			return getThis();
		}

		/**
		 * Sets how quick the gas consumed at previous rewards is forgotten:
		 * 0 means never, 1_000_000 means immediately.
		 * Hence a smaller level means that the latest rewards are heavier
		 * in the determination of the gas price.
		 * Use 0 to keep the gas price constant.
		 * It defaults to 250_000L.
		 * 
		 * @return this builder
		 */
		public T setOblivion(long oblivion) {
			if (oblivion < 0 || oblivion > 1_000_000L)
				throw new IllegalArgumentException("oblivion must be between 0 and 1_000_000");

			this.oblivion = oblivion;
			return getThis();
		}

		/**
		 * Sets the initial inflation applied to the gas consumed by transactions before it gets sent
		 * as reward to the validators. 1,000,000 means 1%.
		 * Inflation can be negative. For instance, -300,000 means -0.3%.
		 * It defaults to 100,000 (that is, inflation is 0.1% by default).
		 * 
		 * @return this builder
		 */
		public T setInitialInflation(long initialInflation) {
			this.initialInflation = initialInflation;
			return getThis();
		}

		/**
		 * Sets the amount of validators' rewards that gets staked. The rest is sent to the validators immediately.
		 * 1000000 = 1%. It defaults to 75%.
		 * 
		 * @param percentStaked the buyer surcharge to set
		 * @return this builder
		 */
		public T setPercentStaked(int percentStaked) {
			if (percentStaked < 0 || percentStaked > 100_000_000L)
				throw new IllegalArgumentException("percentStaked must be between 0 and 100_000_000");

			this.percentStaked = percentStaked;
			return getThis();
		}

		/**
		 * Sets the extra tax paid when a validator acquires the shares of another validator
		 * (in percent of the offer cost). 1000000 = 1%. It defaults to 50%.
		 * 
		 * @param buyerSurcharge the buyer surcharge to set
		 * @return this builder
		 */
		public T setBuyerSurcharge(int buyerSurcharge) {
			if (buyerSurcharge < 0 || buyerSurcharge > 100_000_000L)
				throw new IllegalArgumentException("buyerSurcharge must be between 0 and 100_000_000");

			this.buyerSurcharge = buyerSurcharge;
			return getThis();
		}

		/**
		 * Sets the percent of stake that gets slashed for each misbehaving validator. 1000000 means 1%.
		 * It defaults to 1%.
		 * 
		 * @param slashingForMisbehaving the slashing for misbehaving validators
		 * @return this builder
		 */
		public T setSlashingForMisbehaving(int slashingForMisbehaving) {
			if (slashingForMisbehaving < 0 || slashingForMisbehaving > 100_000_000L)
				throw new IllegalArgumentException("slashingForMisbehaving must be between 0 and 100_000_000");

			this.slashingForMisbehaving = slashingForMisbehaving;
			return getThis();
		}

		/**
		 * Sets the percent of stake that gets slashed for each not behaving (not voting) validator.
		 * 1000000 means 1%. It defaults to 1%.
		 * 
		 * @param slashingForNotBehaving the slashing for not behaving validators
		 * @return this builder
		 */
		public T setSlashingForNotBehaving(int slashingForNotBehaving) {
			if (slashingForNotBehaving < 0 || slashingForNotBehaving > 100_000_000L)
				throw new IllegalArgumentException("slashingForNotBehaving must be between 0 and 100_000_000");

			this.slashingForNotBehaving = slashingForNotBehaving;
			return getThis();
		}

		/**
		 * Specifies that the minimum gas price for transactions is 0, so that the current
		 * gas price is not relevant for the execution of the transactions. It defaults to false.
		 * 
		 * @param ignoresGasPrice true if and only if the minimum gas price must be ignored
		 * @return this builder
		 */
		public T ignoreGasPrice(boolean ignoresGasPrice) {
			this.ignoresGasPrice = ignoresGasPrice;
			return getThis();
		}

		/**
		 * Requires to skip the verification of the classes of the jars installed in the node.
		 * It defaults to false.
		 * 
		 * @param skipsVerification true if and only if the verification must be disabled
		 * @return this builder
		 */
		public T skipVerification(boolean skipsVerification) {
			this.skipsVerification = skipsVerification;
			return getThis();
		}

		/**
		 * Sets the version of the verification module to use.
		 * It defaults to 0.
		 * 
		 * @param verificationVersion the version of the verification module
		 * @return this builder
		 */
		public T setVerificationVersion(int verificationVersion) {
			if (verificationVersion < 0)
				throw new IllegalArgumentException("the verification version must be non-negative");

			this.verificationVersion = verificationVersion;
			return getThis();
		}

		/**
		 * Sets the initial supply of coins of the node.
		 * It defaults to 0.
		 * 
		 * @param initialSupply the initial supply of coins of the node
		 * @return this builder
		 */
		public T setInitialSupply(BigInteger initialSupply) {
			if (initialSupply == null)
				throw new NullPointerException("the initial supply cannot be null");

			if (initialSupply.signum() < 0)
				throw new IllegalArgumentException("the initial supply must be non-negative");

			this.initialSupply = initialSupply;
			return getThis();
		}

		/**
		 * Sets the initial supply of red coins of the node.
		 * It defaults to 0.
		 * 
		 * @param initialRedSupply the initial supply of red coins of the node
		 * @return this builder
		 */
		public T setInitialRedSupply(BigInteger initialRedSupply) {
			if (initialRedSupply == null)
				throw new NullPointerException("the initial red supply cannot be null");

			if (initialRedSupply.signum() < 0)
				throw new IllegalArgumentException("the initial red supply must be non-negative");

			this.initialRedSupply = initialRedSupply;
			return getThis();
		}

		/**
		 * Sets the public key for the gamete account.
		 * It defaults to "" (hence a non-existent key).
		 * 
		 * @param publicKeyOfGamete the Base64-encoded public key of the gamete account
		 * @return this builder
		 */
		public T setPublicKeyOfGamete(String publicKeyOfGamete) {
			if (publicKeyOfGamete == null)
				throw new NullPointerException("the public key of the gamete cannot be null");

			this.publicKeyOfGamete = publicKeyOfGamete;
			return getThis();
		}

		/**
		 * Sets the final supply of coins of the node.
		 * It defaults to 0.
		 * 
		 * @param finalSupply the final supply of coins of the node
		 * @return this builder
		 */
		public T setFinalSupply(BigInteger finalSupply) {
			if (finalSupply == null)
				throw new NullPointerException("the final supply cannot be null");

			if (finalSupply.signum() < 0)
				throw new IllegalArgumentException("the final supply must be non-negative");

			this.finalSupply = finalSupply;
			return getThis();
		}

		/**
		 * Sets the amount of coins that must be payed to start a new poll amount
		 * to validators, for instance to change a consensus parameter.
		 * It defaults to 100.
		 */
		public T setTicketForNewPoll(BigInteger ticketForNewPoll) {
			if (ticketForNewPoll == null)
				throw new NullPointerException("the ticket for a new poll cannot be null");

			if (ticketForNewPoll.signum() < 0)
				throw new IllegalArgumentException("the ticket for new poll must be non-negative");

			this.ticketForNewPoll = ticketForNewPoll;
			return getThis();
		}

		/**
		 * Builds the configuration.
		 * 
		 * @return the configuration
		 */
		public abstract ConsensusConfig build();
	
		/**
		 * Standard design pattern. See http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ205
		 * 
		 * @return this same builder
		 */
		protected abstract T getThis();
	
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