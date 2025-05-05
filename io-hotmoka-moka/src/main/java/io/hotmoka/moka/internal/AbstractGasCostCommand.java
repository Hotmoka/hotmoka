/*
Copyright 2025 Fausto Spoto

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

package io.hotmoka.moka.internal;

import java.math.BigInteger;

import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.api.GasCost;
import io.hotmoka.moka.api.GasCostOutput;
import io.hotmoka.moka.internal.json.GasCostOutputJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;

/**
 * Shared implementation of the commands that report their gas cost in their output.
 */
public abstract class AbstractGasCostCommand extends AbstractMokaRpcCommand {

	/**
	 * The output of this command.
	 */
	protected abstract static class AbstractGasCostCommandOutput implements GasCostOutput {

		/**
		 * The amount of gas consumed for the CPU cost for installing the jar.
		 */
		private final BigInteger gasConsumedForCPU;

		/**
		 * The amount of gas consumed for the RAM cost for installing the jar.
		 */
		private final BigInteger gasConsumedForRAM;

		/**
		 * The amount of gas consumed for the storage cost for installing the jar.
		 */
		private final BigInteger gasConsumedForStorage;

		/**
		 * The gas price used for the transaction.
		 */
		private final BigInteger gasPrice;

		/**
		 * Builds the output of the command.
		 */
		protected AbstractGasCostCommandOutput(GasCost gasCounter, BigInteger gasPrice) {
			this.gasConsumedForCPU = gasCounter.forCPU();
			this.gasConsumedForRAM = gasCounter.forRAM();
			this.gasConsumedForStorage = gasCounter.forStorage();
			this.gasPrice = gasPrice;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		protected AbstractGasCostCommandOutput(GasCostOutputJson json) throws InconsistentJsonException {
			this.gasConsumedForCPU = Objects.requireNonNull(json.getGasConsumedForCPU(), "gasConsumedForCPU cannot be null", InconsistentJsonException::new);
			this.gasConsumedForRAM = Objects.requireNonNull(json.getGasConsumedForRAM(), "gasConsumedForRAM cannot be null", InconsistentJsonException::new);
			this.gasConsumedForStorage = Objects.requireNonNull(json.getGasConsumedForStorage(), "gasConsumedForStorage cannot be null", InconsistentJsonException::new);
			this.gasPrice = Objects.requireNonNull(json.getGasPrice(), "gasPrice cannot be null", InconsistentJsonException::new);
		}

		@Override
		public BigInteger getGasConsumedForCPU() {
			return gasConsumedForCPU;
		}

		@Override
		public BigInteger getGasConsumedForRAM() {
			return gasConsumedForRAM;
		}

		@Override
		public BigInteger getGasConsumedForStorage() {
			return gasConsumedForStorage;
		}

		@Override
		public BigInteger getGasPrice() {
			return gasPrice;
		}

		@Override
		public void toStringGasCost(StringBuilder sb) {
			sb.append("Gas consumption:\n");
			BigInteger totalGasConsumed = gasConsumedForCPU.add(gasConsumedForRAM).add(gasConsumedForStorage);
			sb.append(" * total: " + totalGasConsumed + "\n");
			sb.append("   * for CPU: " + gasConsumedForCPU + "\n");
			sb.append("   * for RAM: " + gasConsumedForRAM + "\n");
			sb.append("   * for storage: " + gasConsumedForStorage + "\n");
			sb.append(" * price per unit: " + panas(gasPrice) + "\n");
			sb.append(" * total price: " + panas(gasPrice.multiply(totalGasConsumed)) + "\n");
		}
	}
}