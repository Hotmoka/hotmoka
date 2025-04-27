/*
Copyright 2023 Fausto Spoto

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

package io.hotmoka.moka.internal.nodes.config;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.NodesConfigShowOutputs;
import io.hotmoka.moka.api.nodes.config.NodesConfigShowOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.json.NodesConfigShowOutputJson;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;

@Command(name = "show", description = "Show the configuration of a node.")
public class Show extends AbstractMokaRpcCommand {

	@Override
	protected void body(RemoteNode remote) throws TimeoutException, InterruptedException, CommandException, NodeException {
		report(json(), new Output(remote.getConfig()), NodesConfigShowOutputs.Encoder::new);
	}

	/**
	 * Implementation of the output of this command.
	 */
	@Immutable
	public static class Output implements NodesConfigShowOutput {

		/**
		 * The configuration in the output.
		 */
		private final ConsensusConfig<?, ?> config;

		/**
		 * Builds the output of the command.
		 * 
		 * @param config the configuration in the output
		 */
		private Output(ConsensusConfig<?, ?> config) {
			this.config = config;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		public Output(NodesConfigShowOutputJson json) throws InconsistentJsonException, NoSuchAlgorithmException {
			this.config = Objects.requireNonNull(json.getConfig(), "config cannot be null", InconsistentJsonException::new).unmap();
		}

		@Override
		public ConsensusConfig<?, ?> getConfig() {
			return config;
		}

		@Override
		public String toString() {
			return config.toString();
		}
	}
}