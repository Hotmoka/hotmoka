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

import java.net.URI;

import io.hotmoka.cli.CommandException;
import io.hotmoka.moka.NodesTendermintResumeOutputs;
import io.hotmoka.moka.api.nodes.tendermint.NodesTendermintResumeOutput;
import io.hotmoka.moka.internal.AbstractNodeResume;
import io.hotmoka.moka.internal.converters.TendermintNodeConfigOptionConverter;
import io.hotmoka.moka.internal.json.NodesTendermintResumeOutputJson;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.hotmoka.node.tendermint.api.TendermintNodeConfig;
import io.hotmoka.websockets.api.FailedDeploymentException;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "resume",
	header = "Resume a new Tendermint node and publish a service to it.",
	description = "This command spawns both a Tendermint engine and a Tendermint node on top of that engine. The configurations of both can be provided through the --tendermint-config and --local-config, respectively, which, when missing, rely on defaults. In any case, such configurations can be updated with explicit values.",
	showDefaultValues = true)
public class Resume extends AbstractNodeResume {

	@Option(names = "--local-config", paramLabel = "<path>", description = "the local configuration of the Hotmoka node, in TOML format", converter = TendermintNodeConfigOptionConverter.class)
	private TendermintNodeConfig localConfig;

	@Override
	protected void execute() throws CommandException {
		TendermintNodeConfig localNodeConfig = mkLocalConfig();

		try (var node = TendermintNodes.resume(localNodeConfig); var service = NodeServices.of(node, getPort())) {
			//cleanUp();
			var output = new Output(URI.create("ws://localhost:" + getPort()));
			report(output, NodesTendermintResumeOutputs.Encoder::new);

			waitForEnterKey();
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

		return builder.build();
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractNodeResumeOutput implements NodesTendermintResumeOutput {

		private Output(URI uri) {
			super(uri);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesTendermintResumeOutputJson json) throws InconsistentJsonException {
			super(json);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append("The following service has been published:\n");
			sb.append(" * " + asUri(getURI()) + ": the API of this Hotmoka node\n");
			sb.append("\n");

			toStringNodeResume(sb);

			return sb.toString();
		}
	}
}