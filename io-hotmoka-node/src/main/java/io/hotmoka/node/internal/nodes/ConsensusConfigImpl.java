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

package io.hotmoka.node.internal.nodes;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.moandjiezana.toml.Toml;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.Base64ConversionException;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.instrumentation.GasCostModels;
import io.hotmoka.instrumentation.api.GasCostModel;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.nodes.ConsensusConfigBuilder;

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
	 * The public key of the gamete account.
	 */
	public final PublicKey publicKeyOfGamete;

	/**
	 * The Base64 encoding of {@link #publicKeyOfGamete}.
	 */
	private final String publicKeyOfGameteBase64;

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
	 * The amount of coin to pay to start a new poll amount the validators,
	 * for instance in order to change a consensus parameter.
	 */
	public final BigInteger ticketForNewPoll;

	/**
	 * The signature algorithm for signing requests. It defaults to "ed25519".
	 */
	public final SignatureAlgorithm signatureForRequests;

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
		this.allowsUnsignedFaucet = builder.allowsUnsignedFaucet;
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
		this.publicKeyOfGamete = builder.publicKeyOfGamete;
		this.publicKeyOfGameteBase64 = builder.publicKeyOfGameteBase64;
		this.signatureForRequests = builder.signatureForRequests;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof ConsensusConfigImpl<?,?> occi && getClass() == other.getClass() &&
			genesisTime.equals(occi.genesisTime) &&
			chainId.equals(occi.chainId) &&
			maxErrorLength == occi.maxErrorLength &&
			maxDependencies == occi.maxDependencies &&
			maxCumulativeSizeOfDependencies == occi.maxCumulativeSizeOfDependencies &&
			allowsUnsignedFaucet == occi.allowsUnsignedFaucet &&
			initialGasPrice.equals(occi.initialGasPrice) &&
			maxGasPerTransaction.equals(occi.maxGasPerTransaction) &&
			ignoresGasPrice == occi.ignoresGasPrice &&
			skipsVerification == occi.skipsVerification &&
			targetGasAtReward.equals(occi.targetGasAtReward) &&
			oblivion == occi.oblivion &&
			initialInflation == occi.initialInflation &&
			verificationVersion == occi.verificationVersion &&
			ticketForNewPoll.equals(occi.ticketForNewPoll) &&
			initialSupply.equals(occi.initialSupply) &&
			finalSupply.equals(occi.finalSupply) &&
			publicKeyOfGamete.equals(occi.publicKeyOfGamete) &&
			signatureForRequests.equals(occi.signatureForRequests);
	}

	@Override
	public int hashCode() {
		return genesisTime.hashCode() ^ chainId.hashCode() ^ Long.hashCode(maxErrorLength) ^ Long.hashCode(maxDependencies)
			^ Long.hashCode(maxCumulativeSizeOfDependencies) ^ publicKeyOfGameteBase64.hashCode() ^ initialGasPrice.hashCode()
			^ maxGasPerTransaction.hashCode() ^ targetGasAtReward.hashCode() ^ Long.hashCode(oblivion)
			^ Long.hashCode(initialInflation) ^ Long.hashCode(verificationVersion) ^ initialSupply.hashCode();
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
		sb.append("# true if and only if the use of the faucet of the gamete is allowed without a valid signature\n");
		sb.append("allows_unsigned_faucet = " + allowsUnsignedFaucet + "\n");
		sb.append("\n");
		sb.append("# true if and only if the static verification of the classes of the jars installed in the node must be skipped\n");
		sb.append("skips_verification = " + skipsVerification + "\n");
		sb.append("\n");
		sb.append("# the Base64-encoded public key of the gamete account\n");
		sb.append("public_key_of_gamete = \"" + publicKeyOfGameteBase64 + "\"\n");
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
		sb.append("# the amount of coin to pay to start a new poll amount the validators,\n");
		sb.append("# for instance in order to change a consensus parameter\n");
		sb.append("ticket_for_new_poll = \"" + ticketForNewPoll + "\"\n");
		sb.append("\n");
		sb.append("# the name of the signature algorithm for signing requests\n");
		sb.append("signature = \"" + signatureForRequests + "\"\n");

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
	public boolean allowsUnsignedFaucet() {
		return allowsUnsignedFaucet;
	}

	@Override
	public boolean skipsVerification() {
		return skipsVerification;
	}

	@Override
	public PublicKey getPublicKeyOfGamete() {
		return publicKeyOfGamete;
	}

	@Override
	public String getPublicKeyOfGameteBase64() {
		return publicKeyOfGameteBase64;
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
	public BigInteger getTicketForNewPoll() {
		return ticketForNewPoll;
	}

	@Override
	public SignatureAlgorithm getSignatureForRequests() {
		return signatureForRequests;
	}

	public GasCostModel getGasCostModel() {
		return GasCostModels.standard();
	}

	/**
	 * The builder of a configuration object.
	 * 
	 * @param <T> the concrete type of the builder
	 */
	public abstract static class ConsensusConfigBuilderImpl<C extends ConsensusConfig<C,B>, B extends ConsensusConfigBuilder<C,B>> implements ConsensusConfigBuilder<C,B> {
		private String chainId = "";
		private LocalDateTime genesisTime = LocalDateTime.now(ZoneId.of("UTC"));
		private int maxErrorLength = 300;
		private boolean allowsUnsignedFaucet = false;
		private SignatureAlgorithm signatureForRequests;
		private BigInteger maxGasPerTransaction = BigInteger.valueOf(1_000_000_000L);
		private int maxDependencies = 20;
		private long maxCumulativeSizeOfDependencies = 10_000_000L;
		private BigInteger initialGasPrice = BigInteger.valueOf(100L);
		private boolean ignoresGasPrice = false;
		private boolean skipsVerification = false;
		private BigInteger targetGasAtReward = BigInteger.valueOf(1_000_000L);
		private long oblivion = 250_000L;
		private long initialInflation = 100_000L; // 0.1%
		private long verificationVersion = 0L;
		private BigInteger initialSupply = new BigInteger("1000000000000000000000000000000000000000000");
		private BigInteger finalSupply = initialSupply.multiply(BigInteger.valueOf(2L)); // BigInteger.TWO crashes the Android client
		private PublicKey publicKeyOfGamete;
		private String publicKeyOfGameteBase64;
		private BigInteger ticketForNewPoll = BigInteger.valueOf(100);

		/**
		 * Creates a builder with default values for the properties.
		 * 
		 * @throws NoSuchAlgorithmException if some cryptographic algorithm is not available
		 */
		protected ConsensusConfigBuilderImpl() throws NoSuchAlgorithmException {
			this(SignatureAlgorithms.ed25519());
		}

		/**
		 * Creates a builder containing default data. but for the given signature.
		 * 
		 * @param signatureForRequests the signature algorithm to use for signing the requests
		 * @return the builder
		 */
		protected ConsensusConfigBuilderImpl(SignatureAlgorithm signatureForRequests) {
			setSignatureForRequests(signatureForRequests);

			try {
				setPublicKeyOfGamete(Entropies.of(new byte[16]).keys("", signatureForRequests).getPublic());
			}
			catch (InvalidKeyException e) {
				// we have generated the key ourselves, how could it be invalid?
				throw new RuntimeException("Unexpected exception", e);
			}
		}

		/**
		 * Creates a builder with properties initialized to those of the given configuration object.
		 * 
		 * @param config the configuration object
		 */
		protected ConsensusConfigBuilderImpl(C config) {
			setChainId(config.getChainId());
			setGenesisTime(config.getGenesisTime());
			setMaxErrorLength(config.getMaxErrorLength());
			setMaxDependencies(config.getMaxDependencies());
			setMaxCumulativeSizeOfDependencies(config.getMaxCumulativeSizeOfDependencies());
			allowUnsignedFaucet(config.allowsUnsignedFaucet());
			setSignatureForRequests(config.getSignatureForRequests());
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
			this.publicKeyOfGamete = config.getPublicKeyOfGamete();
			this.publicKeyOfGameteBase64 = config.getPublicKeyOfGameteBase64();
			setTicketForNewPoll(config.getTicketForNewPoll());
		}

		/**
		 * Reads the properties of the given TOML file and sets them for
		 * the corresponding fields of this builder.
		 * 
		 * @param toml the file
		 * @throws NoSuchAlgorithmException if some cryptographic algorithm in the TOML file is not available
		 * @throws Base64ConversionException if some public key in the TOML file is not correctly Base64-encoded
		 * @throws InvalidKeySpecException if the specification of some public key in the TOML file is illegal
		 * @throws InvalidKeyException if some public key in the TOML file is invalid
		 */
		protected ConsensusConfigBuilderImpl(Toml toml) throws NoSuchAlgorithmException, InvalidKeySpecException, Base64ConversionException, InvalidKeyException {
			this();

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

			var allowsUnsignedFaucet = toml.getBoolean("allows_unsigned_faucet");
			if (allowsUnsignedFaucet != null)
				allowUnsignedFaucet(allowsUnsignedFaucet);

			var signature = toml.getString("signature");
			if (signature != null)
				setSignatureForRequests(SignatureAlgorithms.of(signature));

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

			var ticketForNewPoll = toml.getString("ticket_for_new_poll");
			if (ticketForNewPoll != null)
				setTicketForNewPoll(new BigInteger(ticketForNewPoll));

			var publicKeyOfGamete = toml.getString("public_key_of_gamete");
			if (publicKeyOfGamete != null)
				setPublicKeyOfGamete(this.signatureForRequests.publicKeyFromEncoding(Base64.fromBase64String(publicKeyOfGamete)));
		}

		@Override
		public B setGenesisTime(LocalDateTime genesisTime) {
			this.genesisTime = Objects.requireNonNull(genesisTime, "The genesis time cannot be null");
			return getThis();
		}

		@Override
		public B setChainId(String chainId) {
			this.chainId = Objects.requireNonNull(chainId, "The chain id cannot be null");
			return getThis();
		}

		@Override
		public B setMaxErrorLength(int maxErrorLength) {
			if (maxErrorLength < 0)
				throw new IllegalArgumentException("The max error length cannot be negative");

			this.maxErrorLength = maxErrorLength;
			return getThis();
		}

		private B setMaxErrorLength(long maxErrorLength) {
			if (maxErrorLength < 0 || maxErrorLength > Integer.MAX_VALUE)
				throw new IllegalArgumentException("maxErrorLength must be between 0 and " + Integer.MAX_VALUE + " inclusive");

			this.maxErrorLength = (int) maxErrorLength;
			return getThis();
		}

		@Override
		public B setMaxDependencies(int maxDependencies) {
			if (maxDependencies < 0)
				throw new IllegalArgumentException("The max number of dependencies cannot be negative");

			this.maxDependencies = maxDependencies;
			return getThis();
		}

		private B setMaxDependencies(long maxDependencies) {
			if (maxDependencies < 0 || maxDependencies > Integer.MAX_VALUE)
				throw new IllegalArgumentException("maxDependencies must be between 0 and " + Integer.MAX_VALUE + " inclusive");

			this.maxDependencies = (int) maxDependencies;
			return getThis();
		}

		@Override
		public B setMaxCumulativeSizeOfDependencies(long maxCumulativeSizeOfDependencies) {
			if (maxCumulativeSizeOfDependencies < 0L)
				throw new IllegalArgumentException("The max cumulative size opf the dependencies cannot be negative");

			this.maxCumulativeSizeOfDependencies = maxCumulativeSizeOfDependencies;
			return getThis();
		}

		@Override
		public B allowUnsignedFaucet(boolean allowsUnsignedFaucet) {
			this.allowsUnsignedFaucet = allowsUnsignedFaucet;
			return getThis();
		}

		@Override
		public B setSignatureForRequests(SignatureAlgorithm signature) {
			this.signatureForRequests = Objects.requireNonNull(signature, "The signature algorithm cannot be null");
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
		public B setPublicKeyOfGamete(PublicKey publicKeyOfGamete) throws InvalidKeyException {
			this.publicKeyOfGamete = Objects.requireNonNull(publicKeyOfGamete, "The public key of the gamete cannot be null");
			this.publicKeyOfGameteBase64 = Base64.toBase64String(signatureForRequests.encodingOf(publicKeyOfGamete));
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
				if (cause instanceof FileNotFoundException fne)
					throw fne;
				else
					throw e;
			}
		}
	}
}