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

import io.hotmoka.exceptions.Objects;
import io.hotmoka.moka.api.NodeResumeOutput;
import io.hotmoka.moka.internal.json.NodeResumeOutputJson;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Option;

/**
 * Shared code for the moka commands that resume an already initiallized Hotmoka node.
 */
public abstract class AbstractNodeResume extends AbstractMokaCommand {

	@Option(names = "--max-gas-per-view", description = "the maximal gas limit accepted for calls to @View methods") 
	private BigInteger maxGasPerView;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Option(names = "--port", description = "the network port where the service must be published", defaultValue="8001")
	private int port;

	@Option(names = "--chain-dir", description = "the directory that contains blocks and state of the node", defaultValue = "chain")
	private Path chainDir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

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
	protected abstract static class AbstractNodeResumeOutput implements NodeResumeOutput {
		private final URI uri;

		protected AbstractNodeResumeOutput(URI uri) {
			this.uri = uri;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public AbstractNodeResumeOutput(NodeResumeOutputJson json) throws InconsistentJsonException {
			this.uri = Objects.requireNonNull(json.getURI(), "uri cannot be null", InconsistentJsonException::new);
		}

		@Override
		public final URI getURI() {
			return uri;
		}

		/**
		 * Reports information on the resumed node in the given string builder.
		 * 
		 * @param sb the string builder that gets enriched with the information
		 */
		protected void toStringNodeResume(StringBuilder sb) {
			sb.append(asInteraction("Press the enter key to stop this process and close this node: "));
		}
	}
}