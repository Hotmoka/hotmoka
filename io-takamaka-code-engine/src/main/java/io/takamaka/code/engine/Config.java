package io.takamaka.code.engine;

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
	 * True if and only if the SHA256DSA algorithm must be used to sign the
	 * transactions. The default is ED25519 instead.
	 */
	public final boolean signWithSHA256DSA;

	/**
	 * True if and only if the ED25519 algorithm must be used to sign the
	 * transactions. The is the default.
	 */
	public final boolean signWithED25519;

	/**
	 * True if and only if the qTesla algorithm must be used to sign the
	 * transactions. The default is ED25519 instead.
	 */
	public final boolean signWithQTesla;

	/**
	 * Full constructor for the builder pattern.
	 */
	private Config(Path dir, boolean delete, int maxPollingAttempts,
			       int pollingDelay, int requestCacheSize,
			       int responseCacheSize, int maxErrorLength,
			       boolean signWithSHA256DSA,
			       boolean signWithED25519,
			       boolean signWithQTesla) {

		this.dir = dir;
		this.delete = delete;
		this.maxPollingAttempts = maxPollingAttempts;
		this.pollingDelay = pollingDelay;
		this.requestCacheSize = requestCacheSize;
		this.responseCacheSize = responseCacheSize;
		this.maxErrorLength = maxErrorLength;
		this.signWithSHA256DSA = signWithSHA256DSA;
		this.signWithED25519 = signWithED25519;
		this.signWithQTesla = signWithQTesla;
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
		this.signWithSHA256DSA = parent.signWithSHA256DSA;
		this.signWithED25519 = parent.signWithED25519;
		this.signWithQTesla = parent.signWithQTesla;
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
		private boolean signWithSHA256DSA = false;
		private boolean signWithED25519 = true;
		private boolean signWithQTesla = false;

		/**
		 * Standard design pattern. See http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ205
		 */
		protected abstract T getThis();

		/**
		 * Specifies to use the SHA256DSA signing algorithm for transactions.
		 * The default is ED25519 instead.
		 * 
		 * @return this builder
		 */
		public T signWithSHA256DSA() {
			signWithSHA256DSA = true;
			signWithED25519 = false;
			signWithQTesla = false;

			return getThis();
		}

		/**
		 * Specifies to use the ED25519 signing algorithm for transactions.
		 * This is the default.
		 * 
		 * @return this builder
		 */
		public T signWithED25519() {
			signWithSHA256DSA = false;
			signWithED25519 = true;
			signWithQTesla = false;

			return getThis();
		}

		/**
		 * Specifies to use the qTesla signing algorithm for transactions.
		 * That algorithm is quantum-resistant but has rather large
		 * signatures and keys: disk requirements and gas costs will be higher.
		 * The default is ED25519 instead.
		 * 
		 * @return this builder
		 */
		public T signWithQTesla() {
			signWithSHA256DSA = false;
			signWithED25519 = false;
			signWithQTesla = true;

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
			return new Config(dir, delete, maxPollingAttempts, pollingDelay, requestCacheSize, responseCacheSize, maxErrorLength,
				signWithSHA256DSA, signWithED25519, signWithQTesla);
		}
	}
}