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

import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.annotations.Immutable;
import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.ExceptionSupplier;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.NodesConfigShowOutputs;
import io.hotmoka.moka.api.nodes.config.NodesConfigShowOutput;
import io.hotmoka.moka.internal.AbstractMokaRpcCommand;
import io.hotmoka.moka.internal.json.NodesConfigShowOutputJson;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.remote.api.RemoteNode;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import jakarta.websocket.EncodeException;
import picocli.CommandLine.Command;

@Command(name = "show", description = "Show the configuration of a node.")
public class Show extends AbstractMokaRpcCommand {

	private void body(RemoteNode remote) throws TimeoutException, InterruptedException, NodeException {
		NodesConfigShowOutputs.of(remote.getConfig()).println(System.out, json());
	}

	@Override
	protected void execute() throws CommandException {
		execute(this::body);
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
		public Output(ConsensusConfig<?, ?> config) {
			this(config, IllegalArgumentException::new);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 * @throws NoSuchAlgorithmException if {@code json} refers to a non-available cryptographic algorithm
		 */
		public Output(NodesConfigShowOutputJson json) throws InconsistentJsonException, NoSuchAlgorithmException {
			this(
				Objects.requireNonNull(json.getConfig(), "config cannot be null", InconsistentJsonException::new).unmap(),
				InconsistentJsonException::new
			);
		}

		/**
		 * Builds the output of the command.
		 * 
		 * @param <E> the type of the exception thrown if some arguments is illegal
		 * @param config the configuration in the output
		 * @param onIllegalArgs the generator of the exception thrown if some argument is illegal
		 * @throws E if some argument is illegal
		 */
		private <E extends Exception> Output(ConsensusConfig<?, ?> config, ExceptionSupplier<? extends E> onIllegalArgs) throws E {
			this.config = Objects.requireNonNull(config, "config cannot be null", onIllegalArgs);
		}

		@Override
		public ConsensusConfig<?, ?> getConfig() {
			return config;
		}

		@Override
		public void println(PrintStream out, boolean json) {
			if (json) {
				try {
					out.println(new NodesConfigShowOutputs.Encoder().encode(this));
				}
				catch (EncodeException e) {
					// this should not happen, since the constructor of the JSON representation never throws exceptions
					throw new RuntimeException("Cannot encode the configuration of the node in JSON format", e);
				}
			}
			else
				out.println(config);
		}
	}
}