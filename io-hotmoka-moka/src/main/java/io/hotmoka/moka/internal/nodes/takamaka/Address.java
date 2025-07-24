/*
Copyright 2024 Fausto Spoto

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

package io.hotmoka.moka.internal.nodes.takamaka;

import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.NodesTakamakaAddressOutputs;
import io.hotmoka.moka.api.nodes.takamaka.NodesTakamakaAddressOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.json.NodesTakamakaAddressOutputJson;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.UninitializedNodeException;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;

@Command(name = "address", header = "Show the transaction that installed the Takamaka runtime in a node.", showDefaultValues = true)
public class Address extends AbstractMokaRpcCommand {

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, UninitializedNodeException, ClosedNodeException {
		report(json(), new Output(remote.getTakamakaCode()), NodesTakamakaAddressOutputs.Encoder::new);
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements NodesTakamakaAddressOutput {
		private final TransactionReference takamakaCode;

		private Output(TransactionReference takamakaCode) {
			this.takamakaCode = takamakaCode;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesTakamakaAddressOutputJson json) throws InconsistentJsonException {
			this.takamakaCode = Objects.requireNonNull(json.getTakamakaCode(), "takamakaCode cannot be null", InconsistentJsonException::new).unmap();
		}

		@Override
		public TransactionReference getTakamakaCode() {
			return takamakaCode;
		}

		@Override
		public String toString() {
			return takamakaCode.toString() + "\n";
		}
	}
}