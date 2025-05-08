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
import java.util.Optional;
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
import io.hotmoka.node.api.transactions.TransactionReference;
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

	@Option(names = "--post", description = "just post the transaction, without waiting and reporting its outcome")
	private boolean post;

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
	 * Determine if the user has just requested to post the transaction that consumes the gas, without waiting
	 * and reporting its output.
	 * 
	 * @return true if and only if the user has just requested to post the transaction
	 */
	protected boolean post() {
		return post;
	}

	/**
	 * The output of this command.
	 */
	protected abstract static class AbstractGasCostCommandOutput implements GasCostOutput {

		/**
		 * The transaction that consumed the gas.
		 */
		private final TransactionReference transaction;

		/**
		 * The gas cost of the transaction, if any.
		 */
		private final Optional<GasCost> gasCost;

		/**
		 * The error message of the transaction, if any.
		 */
		private final Optional<String> errorMessage;

		/**
		 * Builds the output of the command.
		 */
		protected AbstractGasCostCommandOutput(TransactionReference transaction, Optional<GasCost> gasCost, Optional<String> errorMessage) {
			this.transaction = transaction;
			this.gasCost = gasCost;
			this.errorMessage = errorMessage;
		}
	
		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public AbstractGasCostCommandOutput(GasCostOutputJson json) throws InconsistentJsonException {
			this.transaction = Objects.requireNonNull(json.getTransaction(), "transaction cannot be null", InconsistentJsonException::new).unmap();

			var gasCost = json.getGasCost();
			if (gasCost.isEmpty())
				this.gasCost = Optional.empty();
			else
				this.gasCost = Optional.of(gasCost.get().unmap());

			this.errorMessage = json.getErrorMessage();
		}

		@Override
		public TransactionReference getTransaction() {
			return transaction;
		}

		@Override
		public Optional<GasCost> getGasCost() {
			return gasCost;
		}

		@Override
		public Optional<String> getErrorMessage() {
			return errorMessage;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			errorMessage.ifPresent(m -> sb.append("The transaction failed with message " + m + "\n"));

			toString(sb);

			gasCost.ifPresent(g -> {
				sb.append("\n");
				g.toString(sb);
			});

			return sb.toString();
		}

		/**
		 * Adds, to the given string builder, specific information about this output.
		 * 
		 * @param sb the string builder
		 */
		protected abstract void toString(StringBuilder sb);
	}
}