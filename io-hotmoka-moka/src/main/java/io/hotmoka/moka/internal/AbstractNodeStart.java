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

package io.hotmoka.moka.internal;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;

import io.hotmoka.cli.CommandException;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.api.NodeStartOutput;
import io.hotmoka.moka.internal.json.NodeStartOutputJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Option;

/**
 * Shared code for the moka commands that start a new Hotmoka node.
 */
public abstract class AbstractNodeStart extends AbstractMokaCommand {

	@Option(names = "--max-gas-per-view", description = "the maximal gas limit accepted for calls to @View methods") 
	private BigInteger maxGasPerView;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Option(names = "--port", description = "the network port where the service must be published", defaultValue="8001")
	private int port;

	@Option(names = "--chain-dir", description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path chainDir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	/**
	 * Asks the user about the real intention to proceed with the destruction of the given directory.
	 * 
	 * @param dir the directory to delete
	 * @throws CommandException if the user replies negatively
	 */
	protected final void askForConfirmation(Path dir) throws CommandException {
		if (!yes && !json && !answerIsYes(asInteraction("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] ")))
			throw new CommandException("Stopped");
	}

	protected boolean json() {
		return json;
	}

	protected int getPort() {
		return port;
	}

	protected Path getChainDir() {
		return chainDir;
	}

	protected BigInteger getMaxGasPerView() {
		return maxGasPerView;
	}

	/**
	 * The output of this command.
	 */
	protected abstract static class AbstractNodeStartOutput implements NodeStartOutput {
		private final URI uri;

		protected AbstractNodeStartOutput(URI uri) {
			this.uri = uri;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public AbstractNodeStartOutput(NodeStartOutputJson json) throws InconsistentJsonException {
			this.uri = Objects.requireNonNull(json.getURI(), "uri cannot be null", InconsistentJsonException::new);
		}

		@Override
		public final URI getURI() {
			return uri;
		}

		/**
		 * Reports information on the started node in the given string builder.
		 * 
		 * @param sb the string builder that gets enriched with the information
		 */
		protected void toStringNodeStart(StringBuilder sb) {
			sb.append(asInteraction("Press the enter key to stop this process and close this node: "));
		}
	}
}