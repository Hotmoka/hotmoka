package io.hotmoka.nodes;

import java.math.BigInteger;

import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * A specification of the consensus parameters of a node. This is typically provided
 * to the {@link io.hotmoka.nodes.views.InitializedNode} view and its data gets
 * stored in the manifest of the node.
 */
public class ConsensusParams {

	/**
	 * The chain identifier of the node. This defaults to the empty string.
	 */
	public final String chainId;

	/**
	 * The maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 * It defaults to 300 characters.
	 */
	public final int maxErrorLength;

	/**
	 * True if and only if the use of the {@code @@SelfCharged} annotation is allowed.
	 * It defaults to false.
	 */
	public final boolean allowsSelfCharged;

	/**
	 * The name of the signature algorithm that must be used to sign the requests
	 * sent to the node. It defaults to "ed25519".
	 */
	public final String signature;

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
	 * This defaults to 10_000.
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

	private ConsensusParams(Builder builder) {
		this.chainId = builder.chainId;
		this.maxErrorLength = builder.maxErrorLength;
		this.allowsSelfCharged = builder.allowsSelfCharged;
		this.signature = builder.signature;
		this.maxGasPerTransaction = builder.maxGasPerTransaction;
		this.ignoresGasPrice = builder.ignoresGasPrice;
		this.targetGasAtReward = builder.targetGasAtReward;
		this.oblivion = builder.oblivion;
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
			.allowSelfCharged(allowsSelfCharged)
			.signRequestsWith(signature)
			.setMaxGasPerTransaction(maxGasPerTransaction)
			.ignoreGasPrice(ignoresGasPrice)
			.setTargetGasAtReward(targetGasAtReward)
			.setOblivion(oblivion);
	}

	public static class Builder {
		private String chainId = "";
		private int maxErrorLength = 300;
		private boolean allowsSelfCharged = false;
		private String signature = SignatureAlgorithm.TYPES.ED25519.name().toLowerCase();
		private BigInteger maxGasPerTransaction = BigInteger.valueOf(1_000_000_000L);
		private boolean ignoresGasPrice = false;
		private BigInteger targetGasAtReward = BigInteger.valueOf(10_000L);
		private long oblivion = 50_000L;

		public ConsensusParams build() {
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
		 * Specifies to signature algorithm to use to sign the requests sent to the node.
		 * It defaults to "ed25519";
		 * 
		 * @param signature the name of the signature algorithm. Currently, this includes
		 *                  "ed25519", "sha256dsa", "empty", "qtesla1" and "qtesla3"
		 * @return this builder
		 */
		public Builder signRequestsWith(String signature) {
			if (signature == null)
				throw new NullPointerException("the signature algorithm name cannot be null");

			try {
				SignatureAlgorithm.TYPES.valueOf(signature.toUpperCase());
			}
			catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("unknown signature algorithm " + signature);
			}
				
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

			this.maxGasPerTransaction = maxGasPerTransaction;
			return this;
		}

		/**
		 * Sets the units of gas that are aimed to be rewarded at each reward.
		 * If the actual reward is smaller, the price of gas must decrease.
		 * If it is larger, the price of gas must increase.
		 * It defaults to 10_000.
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
	}
}