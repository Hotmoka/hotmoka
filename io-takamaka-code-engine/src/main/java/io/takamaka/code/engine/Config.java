package io.takamaka.code.engine;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The configuration of a node.
 */
public class Config {

	/**
	 * The directory where the node's data will be persisted.
	 * Defaults to {@code chain} in the current directory.
	 */
	public final Path dir;

	/**
	 * The maximal number of polling attempts, in milliseconds,
	 * while waiting for the result of a posted transaction.
	 * It defaults to 100.
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
	 * Full constructor for the builder pattern.
	 */
	private Config(Path dir, int maxPollingAttempts, int pollingDelay) {
		this.dir = dir;
		this.maxPollingAttempts = maxPollingAttempts;
		this.pollingDelay = pollingDelay;
	}

	/**
	 * Copy-constructor for subclassing.
	 */
	protected Config(Config parent) {
		this.dir = parent.dir;
		this.maxPollingAttempts = parent.maxPollingAttempts;
		this.pollingDelay = parent.pollingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder {
		private Path dir = Paths.get("chain");
		private int maxPollingAttempts = 100;
		private int pollingDelay = 10;

		/**
		 * Sets the directory where the node's data will be persisted.
		 * It defaults to {@code chain} in the current directory.
		 * 
		 * @param dir the directory
		 * @return this builder
		 */
		public Builder setDir(Path dir) {
			this.dir = dir;
			return this;
		}

		/**
		 * Sets the maximal number of polling attempts, in milliseconds,
		 * while waiting for the result of a posted transaction.
		 * It defaults to 100.
		 * 
		 * @param maxPollingAttempts the the maximal number of polling attempts
		 * @return this builder
		 */
		public Builder setMaxPollingAttempts(int maxPollingAttempts) {
			this.maxPollingAttempts = maxPollingAttempts;
			return this;
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
		public Builder setPollingDelay(int pollingDelay) {
			this.pollingDelay = pollingDelay;
			return this;
		}

		/**
		 * Builds the configuration.
		 * 
		 * @return the configuration
		 */
		public Config build() {
			return new Config(dir, maxPollingAttempts, pollingDelay);
		}
	}
}