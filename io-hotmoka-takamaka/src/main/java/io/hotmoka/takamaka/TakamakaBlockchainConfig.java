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

package io.hotmoka.takamaka;

import io.hotmoka.beans.annotations.Immutable;
import io.hotmoka.local.Config;

/**
 * The configuration of a Takamaka blockchain.
 */
@Immutable
public class TakamakaBlockchainConfig extends Config {

	/**
	 * The maximal number of connection attempts to the Takamaka process during ping.
	 * Defaults to 20.
	 */
	public final int maxPingAttempts;

	/**
	 * The delay between two successive ping attempts, in milliseconds. Defaults to 200.
	 */
	public final int pingDelay;

	/**
	 * The number of last commits that can be checked out, in order to
	 * change the world-view of the store.
	 * This entails that such commits are not garbage-collected, until
	 * new commits get created on top and they end up being deeper.
	 * This is useful if we expect an old state to be checked out, for
	 * instance because the blockchain swaps to another history, but we can
	 * assume a reasonable depth for that to happen. Use -1 if all commits
	 * could ever be checked out, which effectively disable garbage-collection
	 * of the store. Defaults to 20.
	 */
	public final int checkableDepth;

	/**
	 * Full constructor for the builder pattern.
	 */
	protected TakamakaBlockchainConfig(io.hotmoka.local.Config superConfig, int maxPingAttemps, int pingDelay, int checkableDepth) {
		super(superConfig);

		this.maxPingAttempts = maxPingAttemps;
		this.pingDelay = pingDelay;
		this.checkableDepth = checkableDepth;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.hotmoka.local.Config.Builder<Builder> {
		private int maxPingAttempts = 20;
		private int pingDelay = 200;
		private int checkableDepth = 20;

		/**
		 * Sets the maximal number of connection attempts to the Tendermint process during ping.
		 * Defaults to 20.
		 * 
		 * @param maxPingAttempts the max number of attempts
		 * @return this builder
		 */
		public Builder setMaxPingAttempts(int maxPingAttempts) {
			this.maxPingAttempts = maxPingAttempts;
			return this;
		}

		/**
		 * Sets the delay between two successive ping attempts, in milliseconds. Defaults to 200.
		 * 
		 * @param pingDelay the delay
		 * @return this builder
		 */
		public Builder setPingDelay(int pingDelay) {
			this.pingDelay = pingDelay;
			return this;
		}

		/**
		 * Sets the maximal checbale depth for the blockain.
		 * This is the number of last commits that can be checked out, in order to
		 * change the world-view of the store.
		 * This entails that such commits are not garbage-collected, until
		 * new commits get created on top and they end up being deeper.
		 * This is useful if we expect an old state to be checked out, for
		 * instance because the blockchain swaps to another history, but we can
		 * assume a reasonable depth for that to happen. Use -1 if all commits
		 * could ever be checked out, which effectively disable garbage-collection
		 * of the store. Defaults to 20.
		 * 
		 * @param checkableDepth the checkable depth
		 * @return this builder
		 */
		public Builder setCheckaleDepth(int checkableDepth) {
			this.checkableDepth = checkableDepth;
			return this;
		}

		@Override
		public TakamakaBlockchainConfig build() {
			return new TakamakaBlockchainConfig(super.build(), maxPingAttempts, pingDelay, checkableDepth);
		}

		@Override
		protected Builder getThis() {
			return this;
		}
	}
}