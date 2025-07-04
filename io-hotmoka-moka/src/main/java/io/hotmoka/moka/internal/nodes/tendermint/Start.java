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

package io.hotmoka.moka.internal.nodes.tendermint;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.NodesTendermintStartOutputs;
import io.hotmoka.moka.api.nodes.tendermint.NodesTendermintStartOutput;
import io.hotmoka.moka.internal.AbstractNodeStart;
import io.hotmoka.moka.internal.converters.TendermintNodeConfigOptionConverter;
import io.hotmoka.moka.internal.json.NodesTendermintStartOutputJson;
import io.hotmoka.node.local.NodeCreationException;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.websockets.api.FailedDeploymentException;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "start",
	header = "Start a new Tendermint node and publish a service to it.",
	description = "This command spawns both a Tendermint engine and a Tendermint node on top of that engine. The configurations of both can be provided through the --tendermint-config and --local-config, respectively, which, when missing, rely on defaults. In any case, such configurations can be updated with explicit values.",
	showDefaultValues = true)
public class Start extends AbstractNodeStart {

	@Option(names = "--tendermint-config", paramLabel = "<path>", description = "the directory containing the configuration of the underlying Tendermint engine; this is a directory containing config/ and data/ that can be generated, for instance, by the tendermint init command of Tendermint; if missing, a default configuration for a one-validator network will be used; this will be copied inside the directory specified by --chain-dir")
	private Path tendermintConfig;

	@Option(names = "--local-config", paramLabel = "<path>", description = "the local configuration of the Hotmoka node, in TOML format", converter = TendermintNodeConfigOptionConverter.class)
	private TendermintNodeConfig localConfig;

	@Option(names = "--delete-tendermint-config", description = "delete the directory specified by --tendermint-config after starting the node and copying it inside the directory specified by --chain-dir")
	private boolean deleteTendermintConfig;

	@Override
	protected void execute() throws CommandException {
		TendermintNodeConfig localNodeConfig = mkLocalConfig();
		askForConfirmation(localNodeConfig.getDir());

		try (var node = TendermintNodes.init(localNodeConfig);
			 var service = NodeServices.of(node, getPort())) {

			cleanUp();
			var output = new Output(URI.create("ws://localhost:" + getPort()));
			report(json(), output, NodesTendermintStartOutputs.Encoder::new);

			waitForEnterKey();
		}
		catch (NodeCreationException e) {
			throw new CommandException("The node could not be created", e);
		}
		catch (FailedDeploymentException e) {
			throw new CommandException("Cannot deploy the service at port " + getPort());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CommandException("The operation has been interrupted", e);
		}
	}

	/**
	 * Yields the local configuration of the Hotmoka node.
	 * 
	 * @return the local configuration of the Hotmoka node
	 * @throws CommandException if the configuration cannot be built
	 */
	private TendermintNodeConfig mkLocalConfig() throws CommandException {
		var builder = localConfig != null ? localConfig.toBuilder() : TendermintNodeConfigBuilders.defaults();

		if (getMaxGasPerView() != null)
			builder = builder.setMaxGasPerViewTransaction(getMaxGasPerView());

		if (getChainDir() != null)
			builder = builder.setDir(getChainDir());

		if (tendermintConfig != null)
			builder.setTendermintConfigurationToClone(tendermintConfig);

		return builder.build();
	}

	private void cleanUp() throws CommandException {
		try {
			if (deleteTendermintConfig)
				Files.walk(tendermintConfig)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
		}
		catch (IOException e) {
			throw new CommandException("Cannot delete the Tendermint configuration", e);
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractNodeStartOutput implements NodesTendermintStartOutput {

		private Output(URI uri) {
			super(uri);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesTendermintStartOutputJson json) throws InconsistentJsonException {
			super(json);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append("The following service has been published:\n");
			sb.append(" * " + asUri(getURI()) + ": the API of this Hotmoka node\n");
			sb.append("\n");

			toStringNodeStart(sb);

			return sb.toString();
		}
	}
}