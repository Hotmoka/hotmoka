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

package io.hotmoka.moka.internal.nodes.disk;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.moka.NodesDiskInitOutputs;
import io.hotmoka.moka.api.nodes.disk.NodesDiskInitOutput;
import io.hotmoka.moka.internal.converters.ConsensusConfigOptionConverter;
import io.hotmoka.moka.internal.converters.DiskNodeConfigOptionConverter;
import io.hotmoka.moka.internal.json.NodesDiskInitOutputJson;
import io.hotmoka.moka.internal.nodes.AbstractInit;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.hotmoka.node.disk.api.DiskNodeConfig;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "init",
	description = "Initialize a new disk node and publish a service to it. The configurations of the node can be provided through --node-local-config and --node-consensus-config, which, when missing, rely to defaults. In any case, such configuration can be updated with explicit values, such as --initial-supply.",
	showDefaultValues = true)
public class Init extends AbstractInit {

	@Option(names = "--node-local-config", description = "the local configuration of the Hotmoka node, in TOML format", converter = DiskNodeConfigOptionConverter.class)
	private DiskNodeConfig nodeLocalConfig;

	@Option(names = "--node-consensus-config", description = "the local configuration of the Hotmoka node, in TOML format", converter = ConsensusConfigOptionConverter.class)
	private ConsensusConfig<?, ?> nodeConsensusConfig;

	@Override
	protected void execute() throws CommandException {
		DiskNodeConfig localNodeConfig = mkLocalNodeConfig();
		ConsensusConfig<?, ?> consensus = mkConsensusNodeConfig();
		askForConfirmation(localNodeConfig.getDir());
			
		try (var node = DiskNodes.init(localNodeConfig);
			var initialized = InitializedNodes.of(node, consensus, getTakamakaCode());
			var service = NodeServices.of(node, getPort())) {

			report(json(), new Output(initialized.gamete(), URI.create("ws://localhost:" + getPort())), NodesDiskInitOutputs.Encoder::new);
			waitForEnterKey();
		}
		catch (IOException e) {
			throw new CommandException("Cannot access file \"" + getTakamakaCode() + "\"!", e);
		}
		catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
			throw new CommandException("Could not initialize the node", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CommandException("The operation has been interrupted", e);
		}
		catch (NodeException e) {
			throw new RuntimeException(e);
		}
		catch (TimeoutException e) {
			throw new CommandException("The operation has timed-out", e);
		}
	}

	/**
	 * Yields the local configuration of the Hotmoka node.
	 * 
	 * @return the local configuration of the Hotmoka node
	 * @throws CommandException if the configuration cannot be built
	 */
	private DiskNodeConfig mkLocalNodeConfig() throws CommandException {
		var builder = nodeLocalConfig != null ? nodeLocalConfig.toBuilder() : DiskNodeConfigBuilders.defaults();

		if (getMaxGasPerView() != null)
			builder = builder.setMaxGasPerViewTransaction(getMaxGasPerView());

		if (getDir() != null)
			builder = builder.setDir(getDir());

		return builder.build();
	}

	private ConsensusConfig<?, ?> mkConsensusNodeConfig() throws CommandException {
		try {
			var builder = nodeConsensusConfig != null ? nodeConsensusConfig.toBuilder() : ConsensusConfigBuilders.defaults();
			fillConsensusNodeConfig(builder);
			return builder.build();
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("A cyrptographic algorithm is not available", e);
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractInitOutput implements NodesDiskInitOutput {

		private Output(StorageReference gamete, URI uri) {
			super(gamete, uri);
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesDiskInitOutputJson json) throws InconsistentJsonException {
			super(json);
		}

		@Override
		protected void toString(StringBuilder sb) {
			sb.append("The following service has been published:\n");
			sb.append(" * " + asUri(getURI()) + ": the API of this Hotmoka node\n");
			sb.append("\n");
		}
	}
}