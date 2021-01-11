package io.takamaka.code.engine;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.hotmoka.beans.references.TransactionReference;

/**
 * The configuration of a node.
 */
public class Config {

	/**
	 * The directory where the node's data will be persisted.
	 * It defaults to {@code chain} in the current directory.
	 */
	public final Path dir;

	/**
	 * True if and only if {@link #dir} must be deleted when
	 * the node starts. It defaults to true.
	 */
	public final boolean delete;

	/**
	 * The maximal number of polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * It defaults to 60.
	 */
	public final int maxPollingAttempts;

	/**
	 * The delay of two subsequent polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * This delay is then increased by 10% at each subsequent attempt.
	 * It defaults to 10.
	 */
	public final int pollingDelay;

	/**
	 * The size of the cache for the {@link io.hotmoka.nodes.Node#getRequest(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	public final int requestCacheSize;

	/**
	 * The size of the cache for the {@link io.hotmoka.nodes.Node#getResponse(TransactionReference)} method.
	 * It defaults to 1,000.
	 */
	public final int responseCacheSize;

	/**
	 * The maximal length of the error message kept in the store of the node.
	 * Beyond this threshold, the message gets truncated.
	 * It defaults to 300 characters.
	 */
	public final int maxErrorLength;

	/**
	 * The name of the signature algorithm that must be used to sign the requests
	 * sent to the node. It defaults to "ed25519".
	 */
	public final String signature;

	/**
	 * True if and only if the use of the {@code @@SelfCharged} annotation is allowed.
	 * It defaults to false.
	 */
	public final boolean allowSelfCharged;

	/**
	 * The maximal amount of gas that a view transaction can consume.
	 * It defaults to 1_000_000_000.
	 */
	public final BigInteger maxGasPerViewTransaction;

	/**
	 * Full constructor for the builder pattern.
	 */
	private Config(Path dir, boolean delete, int maxPollingAttempts,
			       int pollingDelay, int requestCacheSize,
			       int responseCacheSize, int maxErrorLength,
			       String signature,
			       boolean allowSelfCharged,
			       BigInteger maxGasPerViewTransaction) {

		this.dir = dir;
		this.delete = delete;
		this.maxPollingAttempts = maxPollingAttempts;
		this.pollingDelay = pollingDelay;
		this.requestCacheSize = requestCacheSize;
		this.responseCacheSize = responseCacheSize;
		this.maxErrorLength = maxErrorLength;
		this.signature = signature;
		this.allowSelfCharged = allowSelfCharged;
		this.maxGasPerViewTransaction = maxGasPerViewTransaction;
	}

	/**
	 * Copy-constructor for subclassing.
	 */
	protected Config(Config parent) {
		this.dir = parent.dir;
		this.delete = parent.delete;
		this.maxPollingAttempts = parent.maxPollingAttempts;
		this.pollingDelay = parent.pollingDelay;
		this.requestCacheSize = parent.requestCacheSize;
		this.responseCacheSize = parent.responseCacheSize;
		this.maxErrorLength = parent.maxErrorLength;
		this.signature = parent.signature;
		this.allowSelfCharged = parent.allowSelfCharged;
		this.maxGasPerViewTransaction = parent.maxGasPerViewTransaction;
	}

	/**
	 * The builder of a configuration object.
	 */
	public abstract static class Builder<T extends Builder<T>> {
		private Path dir = Paths.get("chain");
		private boolean delete = true;
		private int maxPollingAttempts = 60;
		private int pollingDelay = 10;
		private int requestCacheSize = 1_000;
		private int responseCacheSize = 1_000;
		private int maxErrorLength = 300;
		private String signature = "ed25519";
		private boolean allowsSelfCharged = false;
		private BigInteger maxGasPerViewTransaction = BigInteger.valueOf(1_000_000_000);

		/**
		 * Standard design pattern. See http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ205
		 */
		protected abstract T getThis();

		/**
		 * Sets the maximal amount of gas that a view transaction can consume.
		 * It defaults to 1_000_000_000.
		 */
		public T setMaxGasPerViewTransaction(BigInteger maxGasPerViewTransaction) {
			if (maxGasPerViewTransaction == null)
				throw new NullPointerException("the maximal amount of gas per transaction cannot be null");

			this.maxGasPerViewTransaction = maxGasPerViewTransaction;
	
			return getThis();
		}

		/**
		 * Specifies to signature algorithm to use to sign the requests sent to the node.
		 * It defaults to "ed25519";
		 * 
		 * @return this builder
		 */
		public T signRequestsWith(String signature) {
			this.signature = signature;

			return getThis();
		}

		/**
		 * Specifies to allows the {@code @@SelfCharged} annotation in the Takamaka
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
		 * Sets the directory where the node's data will be persisted.
		 * It defaults to {@code chain} in the current directory.
		 * 
		 * @param dir the directory
		 * @return this builder
		 */
		public T setDir(Path dir) {
			this.dir = dir;
			return getThis();
		}

		/**
		 * Sets the flag that determines if the directory where
		 * the node stores its data must be deleted at start-up.
		 * It defaults to true.
		 * 
		 * @param delete the new value of the flag
		 * @return this builder
		 */
		public T setDelete(boolean delete) {
			this.delete = delete;
			return getThis();
		}

		/**
		 * Sets the maximal number of polling attempts, in milliseconds,
		 * while waiting for the result of a posted transaction.
		 * It defaults to 60.
		 * 
		 * @param maxPollingAttempts the the maximal number of polling attempts
		 * @return this builder
		 */
		public T setMaxPollingAttempts(int maxPollingAttempts) {
			this.maxPollingAttempts = maxPollingAttempts;
			return getThis();
		}

		/**
		 * Sets the delay of two subsequent polling attempts, in milliseconds,
		 * while waiting for the result of a posted transaction.
		 * This delay is then increased by 10% at each subsequent attempt.
		 * It defaults to 10.
		 * 
		 * @param pollingDelay the delay
		 * @return this builder
		 */
		public T setPollingDelay(int pollingDelay) {
			this.pollingDelay = pollingDelay;
			return getThis();
		}

		/**
		 * Sets size of the cache for the {@link io.hotmoka.nodes.Node#getRequest(TransactionReference)} method.
		 * It defaults to 1,000.
		 * 
		 * @param requestCacheSize the cache size
		 * @return this builder
		 */
		public T setRequestCacheSize(int requestCacheSize) {
			this.requestCacheSize = requestCacheSize;
			return getThis();
		}

		/**
		 * Sets size of the cache for the {@link io.hotmoka.nodes.Node#getResponse(TransactionReference)} method.
		 * It defaults to 1,000.
		 * 
		 * @param responseCacheSize the cache size
		 * @return this builder
		 */
		public T setResponseCacheSize(int responseCacheSize) {
			this.responseCacheSize = responseCacheSize;
			return getThis();
		}

		/**
		 * Sets the maximal length of the error message kept in the store of the node.
		 * Beyond this threshold, the message gets truncated.
		 * It defaults to 300 characters.
		 * 
		 * @param maxErrorLength the maximal error length
		 * @return this builder
		 */
		public T setMaxErrorLength(int maxErrorLength) {
			this.maxErrorLength = maxErrorLength;
			return getThis();
		}

		/**
		 * Builds the configuration.
		 * 
		 * @return the configuration
		 */
		public Config build() {
			return new Config(dir, delete, maxPollingAttempts, pollingDelay,
				requestCacheSize, responseCacheSize, maxErrorLength, signature, allowsSelfCharged,
				maxGasPerViewTransaction);
		}
	}
}