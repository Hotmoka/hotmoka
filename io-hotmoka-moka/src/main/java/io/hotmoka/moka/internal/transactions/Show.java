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

package io.hotmoka.moka.internal.transactions;

import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.api.MisbehavingNodeException;
import io.hotmoka.moka.TransactionsShowOutputs;
import io.hotmoka.moka.api.transactions.TransactionsShowOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.converters.TransactionReferenceOptionConverter;
import io.hotmoka.moka.internal.json.TransactionsShowOutputJson;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.TransactionRequest;
import io.hotmoka.node.api.responses.TransactionResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "show", header = "Show a transaction in the store of a node.", showDefaultValues = true)
public class Show extends AbstractMokaRpcCommand {

	@Parameters(index = "0", description = "the transaction", converter = TransactionReferenceOptionConverter.class)
    private TransactionReference transaction;

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, ClosedNodeException, MisbehavingNodeException {
		report(json(), new Output(remote, transaction), TransactionsShowOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements TransactionsShowOutput {

		/**
		 * The request of the transaction.
		 */
		private final TransactionRequest<?> request;

		/**
		 * The response of the transaction.
		 */
		private final TransactionResponse response;

		private Output(RemoteNode remote, TransactionReference transaction) throws ClosedNodeException, TimeoutException, InterruptedException, CommandException, MisbehavingNodeException {
			try {
				this.request = remote.getRequest(transaction);
			}
			catch (UnknownReferenceException e) {
				throw new CommandException("Transaction " + transaction + " cannot be found in the node");
			}

			try {
				this.response = remote.getResponse(transaction);
			}
			catch (UnknownReferenceException e) {
				throw new MisbehavingNodeException("The node contains the request of transaction " + transaction + " but it does not contain its response");
			}
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(TransactionsShowOutputJson json) throws InconsistentJsonException {
			this.request = Objects.requireNonNull(json.getRequest(), "request cannot be null", InconsistentJsonException::new).unmap();
			this.response = Objects.requireNonNull(json.getResponse(), "response cannot be null", InconsistentJsonException::new).unmap();
		}

		@Override
		public TransactionRequest<?> getRequest() {
			return request;
		}

		@Override
		public TransactionResponse getResponse() {
			return response;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append(red("request:\n"));
			sb.append(request);
			sb.append("\n\n");
			sb.append(red("response:\n"));
			sb.append(response);

			return sb.toString();
		}
	}
}