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
	 * Full constructor for the builder pattern.
	 */
	protected TakamakaBlockchainConfig(io.hotmoka.local.Config superConfig, int maxPingAttemps, int pingDelay) {
		super(superConfig);

		this.maxPingAttempts = maxPingAttemps;
		this.pingDelay = pingDelay;
	}

	/**
	 * The builder of a configuration object.
	 */
	public static class Builder extends io.hotmoka.local.Config.Builder<Builder> {
		private int maxPingAttempts = 20;
		private int pingDelay = 200;

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

		@Override
		public TakamakaBlockchainConfig build() {
			return new TakamakaBlockchainConfig(super.build(), maxPingAttempts, pingDelay);
		}

		@Override
		protected Builder getThis() {
			return this;
		}
	}
}