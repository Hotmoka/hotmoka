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

package io.hotmoka.moka.internal.nodes;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.NodesInfoOutputs;
import io.hotmoka.moka.api.nodes.NodesInfoOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.json.NodesInfoOutputJson;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.nodes.NodeInfo;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;

@Command(name = "info", header = "Show node-specific information about a node.", showDefaultValues = true)
public class Info extends AbstractMokaRpcCommand {

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, ClosedNodeException {
		report(new Output(remote.getInfo()), NodesInfoOutputs.Encoder::new);
	}

	/**
	 * Implementation of the output of this command.
	 */
	@Immutable
	public static class Output implements NodesInfoOutput {

		/**
		 * The node information in the output.
		 */
		private final NodeInfo info;

		/**
		 * Builds the output of the command.
		 * 
		 * @param info the node information in the output
		 */
		private Output(NodeInfo info) {
			this.info = info;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		public Output(NodesInfoOutputJson json) throws InconsistentJsonException {
			this.info = Objects.requireNonNull(json.getInfo(), "info cannot be null", InconsistentJsonException::new).unmap();
		}

		@Override
		public NodeInfo getInfo() {
			return info;
		}

		@Override
		public String toString() {
			return info.toString() + "\n";
		}
	}
}