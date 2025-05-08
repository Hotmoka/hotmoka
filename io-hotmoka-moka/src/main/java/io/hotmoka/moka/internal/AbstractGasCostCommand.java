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
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.GasHelpers;
import io.hotmoka.moka.api.GasCost;
import io.hotmoka.moka.api.GasCostOutput;
import io.hotmoka.moka.internal.json.GasCostOutputJson;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Option;

/**
 * Shared implementation of the commands that report their gas cost in their output.
 */
public abstract class AbstractGasCostCommand extends AbstractMokaRpcCommand {

	@Option(names = "--gas-limit", description = "the gas limit used for the transaction; if missing, it will be determined through a heuristic") 
	private BigInteger gasLimit;

	@Option(names = "--gas-price", description = "the gas price used for the transaction; if missing, the current gas price of the network will be used") 
	private BigInteger gasPrice;

	/**
	 * Yields the gas limit to use for the transaction.
	 * 
	 * @param defaultGasLimit the default gas limit to use if the {@code --gas-limit} optin is not specified
	 * @return the value specified through {@code --gas-limit}, or that provided by the {@code defaultGasLimit} otherwise
	 * @throws CommandException if the gas limit cannot be determined
	 */
	protected BigInteger determineGasLimit(GasLimitHeuristic heuristic) throws CommandException {
		return gasLimit != null ? gasLimit : heuristic.apply();
	}

	/**
	 * A heuristic about the gas limit to use for the transaction of this command.
	 */
	protected interface GasLimitHeuristic {

		/**
		 * Yields the gas limit to use as heuristic for the transaction of the command.
		 * 
		 * @return the gas limit to use as heuristic for the transaction of the command
		 * @throws CommandException if the heuristic cannot be computed
		 */
		BigInteger apply() throws CommandException;
	}

	/**
	 * Yields the gas price to offer for the transaction.
	 * 
	 * @param remote the node where the transaction will be executed
	 * @return the gas price to offer for the transaction
	 * @throws CommandException if the gas price cannot be determined
	 * @throws NodeException if the node is misbehaving
	 * @throws TimeoutException if the operation times out
	 * @throws InterruptedException if the operation is interrupted while waiting for a result
	 */
	protected BigInteger determineGasPrice(RemoteNode remote) throws CommandException, NodeException, TimeoutException, InterruptedException {
		if (gasPrice != null)
			return gasPrice;

		try {
			return GasHelpers.of(remote).getGasPrice();
		}
		catch (CodeExecutionException | TransactionRejectedException | TransactionException e) {
			throw new CommandException("Cannot determine the current gas price!", e);
		}
	}

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
			this.gasConsumedForCPU = gasCounter.getForCPU();
			this.gasConsumedForRAM = gasCounter.getForRAM();
			this.gasConsumedForStorage = gasCounter.getForStorage();
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

		/**
		 * Reports the gas cost information inside the given string builder.
		 * 
		 * @param sb the string builder
		 */
		protected void toStringGasCost(StringBuilder sb) {
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