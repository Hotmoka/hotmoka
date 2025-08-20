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

package io.hotmoka.moka.internal.nodes.mokamint;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.moka.NodesMokamintInitOutputs;
import io.hotmoka.moka.api.nodes.mokamint.NodesMokamintInitOutput;
import io.hotmoka.moka.internal.AbstractNodeInit;
import io.hotmoka.moka.internal.converters.ConsensusConfigOptionConverter;
import io.hotmoka.moka.internal.converters.MokamintLocalNodeConfigOptionConverter;
import io.hotmoka.moka.internal.converters.MokamintNodeConfigOptionConverter;
import io.hotmoka.moka.internal.json.NodesMokamintInitOutputJson;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.mokamint.MokamintInitializedNodes;
import io.hotmoka.node.mokamint.MokamintNodeConfigBuilders;
import io.hotmoka.node.mokamint.MokamintNodes;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.websockets.api.FailedDeploymentException;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.mokamint.miner.local.LocalMiners;
import io.mokamint.node.local.LocalNodeConfigBuilders;
import io.mokamint.node.local.api.LocalNodeConfig;
import io.mokamint.node.service.PublicNodeServices;
import io.mokamint.node.service.RestrictedNodeServices;
import io.mokamint.plotter.PlotAndKeyPairs;
import io.mokamint.plotter.Plots;
import io.mokamint.plotter.api.PlotAndKeyPair;
import io.mokamint.plotter.api.WrongKeyException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init",
	header = "Initialize a new Mokamint node and publish a service to it.",
	description = "This command spawns and initializes both a Mokamint engine and a Mokamint node on top of that engine. The configurations of both can be provided through the --mokamint-config and (--local-config and --consensus-config), respectively, which, when missing, rely on defaults. In any case, such configurations can be updated with explicit values.",
	showDefaultValues = true)
public class Init extends AbstractNodeInit {

	@Parameters(description = "the path of the plot file that the Mokamint miner will use for mining deadlines")
	private Path plot;

	@Parameters(description = "the path of the key pair of the Mokamint node, used to sign the blocks that it creates")
	private Path keysOfMokamintNode;

	@Parameters(description = "the path of the key pair of the plot file, used to sign the deadlines that the miner creates with that plot file")
	private Path keysOfPlot;

	@Option(names = "--mokamint-config", paramLabel = "<path>", description = "the configuration of the underlying Mokamint engine, in TOML format", converter = MokamintLocalNodeConfigOptionConverter.class)
	private LocalNodeConfig mokamintConfig;

	@Option(names = "--local-config", paramLabel = "<path>", description = "the local configuration of the Hotmoka node, in TOML format", converter = MokamintNodeConfigOptionConverter.class)
	private MokamintNodeConfig localConfig;

	@Option(names = "--consensus-config", paramLabel = "<path>", description = "the consensus configuration of the Hotmoka network, in TOML format", converter = ConsensusConfigOptionConverter.class)
	private ConsensusConfig<?, ?> consensusConfig;

	@Option(names = { "--mokamint-port", "--mokamint-port-public" }, description = "the network port where the public Mokamint service must be published", defaultValue="8030")
	private int mokamintPort;

	@Option(names = "--mokamint-port-restricted", description = "the network port where the restricted Mokamint service must be published", defaultValue="8031")
	private int mokamintPortRestricted;

	@Option(names = "--visible-as", paramLabel = "<URI>", description = "the URI that can be used to contact the public Mokamint service from outside; if missing, Mokamint will try to guess its URI, which might fail, especially from inside a Docker container")
	private URI visibleAs;

	@Option(names = "--password-of-mokamint-node", description = "the password of the key pair of the Mokamint node", interactive = true, defaultValue = "")
    private char[] passwordOfKeysOfMokamintNode;

	@Option(names = "--password-of-plot", description = "the password of the key pair of the plot file", interactive = true, defaultValue = "")
    private char[] passwordOfKeysOfPlot;

	@Override
	protected void execute() throws CommandException {
		try {
			LocalNodeConfig mokamintConfig = mkMokamintConfig();
			MokamintNodeConfig localNodeConfig = mkLocalConfig();
			ConsensusConfig<?, ?> consensus = mkConsensusConfig();
			KeyPair keysOfNode = mkKeysOfMokamintNode(mokamintConfig);
			KeyPair keysOfPlot = mkKeysOfPlot(mokamintConfig);
			askForConfirmation(localNodeConfig.getDir());

			try (var node = MokamintNodes.init(localNodeConfig, mokamintConfig, keysOfNode); var plot = Plots.load(this.plot)) {
				try (var miner = LocalMiners.of(new PlotAndKeyPair[] { PlotAndKeyPairs.of(plot, keysOfPlot) })) {
					var engine = node.getMokamintEngine().get();
					engine.add(miner).orElseThrow(() -> new CommandException("Could not add a miner to the Mokamint node"));

					// the next services will be closed when the node will be closed
					var mokamintNodePublicURI = URI.create("ws://localhost:" + mokamintPort);
					
					try {
						PublicNodeServices.open(engine, mokamintPort, 1800000, 1000, Optional.ofNullable(visibleAs));
					}
					catch (FailedDeploymentException e) {
						throw new CommandException("Cannot deploy the service at port " + mokamintPort);
					}

					try {
						RestrictedNodeServices.open(engine, mokamintPortRestricted);
					}
					catch (FailedDeploymentException e) {
						throw new CommandException("Cannot deploy the service at port " + mokamintPortRestricted);
					}

					try (var initialized = MokamintInitializedNodes.of(node, consensus, getTakamakaCode()); var service = NodeServices.of(node, getPort())) {
						var output = new Output(initialized.gamete(), URI.create("ws://localhost:" + getPort()), mokamintNodePublicURI, URI.create("ws://localhost:" + mokamintPortRestricted));
						report(json(), output, NodesMokamintInitOutputs.Encoder::new);
						waitForEnterKey();
					}
					catch (FailedDeploymentException e) {
						throw new CommandException("Cannot deploy the service at port " + getPort());
					}
				}
				catch (IOException e) {
					throw new CommandException("Cannot access file \"" + getTakamakaCode() + "\"!", e);
				}
			}
			catch (ClosedNodeException e) {
				throw new CommandException("The node has been expectedly closed", e);
			}
			catch (WrongKeyException e) {
				throw new CommandException("The key pair does not contain the correct keys for the plot file");
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The plot file refers to a non-available cryptographic algorithm", e);
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + plot + "\"!", e);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Could not initialize the node", e);
			}
			catch (UnexpectedCodeException e) {
				throw new CommandException("The Takamaka runtime installed in the node contains unexpected code", e);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CommandException("The operation has been interrupted", e);
			}
			catch (io.mokamint.node.api.ClosedNodeException e) {
				throw new CommandException("The Mokamint node has been closed", e);
			}
			catch (TimeoutException e) {
				throw new CommandException("The operation has timed-out", e);
			}
		}
		finally {
			Arrays.fill(passwordOfKeysOfMokamintNode, ' ');
			Arrays.fill(passwordOfKeysOfPlot, ' ');
		}
	}

	/**
	 * Yields the configuration of the underlying Mokamint engine.
	 * 
	 * @return the configuration of the underlying Mokamint engine
	 * @throws CommandException if the configuration cannot be built
	 */
	private LocalNodeConfig mkMokamintConfig() throws CommandException {
		try {
			var builder = mokamintConfig != null ? mokamintConfig.toBuilder() : LocalNodeConfigBuilders.defaults();
			if (getChainDir() != null)
				builder = builder.setDir(getChainDir().resolve("mokamint"));

			return builder.build();
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("Some cryptographic algorithm is not available", e);
		}
	}

	/**
	 * Yields the local configuration of the Hotmoka node.
	 * 
	 * @return the local configuration of the Hotmoka node
	 * @throws CommandException if the configuration cannot be built
	 */
	private MokamintNodeConfig mkLocalConfig() throws CommandException {
		var builder = localConfig != null ? localConfig.toBuilder() : MokamintNodeConfigBuilders.defaults();

		if (getMaxGasPerView() != null)
			builder = builder.setMaxGasPerViewTransaction(getMaxGasPerView());

		if (getChainDir() != null)
			builder = builder.setDir(getChainDir());

		return builder.build();
	}

	private ConsensusConfig<?, ?> mkConsensusConfig() throws CommandException {
		try {
			var builder = consensusConfig != null ? consensusConfig.toBuilder() : ConsensusConfigBuilders.defaults();
			fillConsensusConfig(builder);
			return builder.build();
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("A cyrptographic algorithm is not available", e);
		}
	}

	private KeyPair mkKeysOfMokamintNode(LocalNodeConfig mokamintConfig) throws CommandException {
		String passwordOfKeysOfMokamintNodeAsString = new String(passwordOfKeysOfMokamintNode);

		try {
			return Entropies.load(keysOfMokamintNode).keys(passwordOfKeysOfMokamintNodeAsString, mokamintConfig.getSignatureForBlocks());
		}
		catch (IOException e) {
			throw new CommandException("Cannot access file \"" + keysOfMokamintNode + "\"!", e);
		}
		finally {
			passwordOfKeysOfMokamintNodeAsString = null;
			Arrays.fill(passwordOfKeysOfMokamintNode, ' ');
		}
	}

	private KeyPair mkKeysOfPlot(LocalNodeConfig mokamintConfig) throws CommandException {
		String passwordOfKeysOfPlotAsString = new String(passwordOfKeysOfPlot);

		try {
			return Entropies.load(keysOfPlot).keys(passwordOfKeysOfPlotAsString, mokamintConfig.getSignatureForDeadlines());
		}
		catch (IOException e) {
			throw new CommandException("Cannot access file \"" + keysOfPlot + "\"!", e);
		}
		finally {
			passwordOfKeysOfPlotAsString = null;
			Arrays.fill(passwordOfKeysOfPlot, ' ');
		}
	}

	/**
	 * The output of this command.
	 */
	public static class Output extends AbstractNodeInitOutput implements NodesMokamintInitOutput {
		private final URI uriMokamintPublic;
		private final URI uriMokamintRestricted;

		private Output(StorageReference gamete, URI uri, URI uriMokamintPublic, URI uriMokamintRestricted) {
			super(gamete, uri);

			this.uriMokamintPublic = uriMokamintPublic;
			this.uriMokamintRestricted = uriMokamintRestricted;
		}

		/**
		 * Builds the output of the command from its JSON representation.
		 * 
		 * @param json the JSON representation
		 * @throws InconsistentJsonException if {@code json} is inconsistent
		 */
		public Output(NodesMokamintInitOutputJson json) throws InconsistentJsonException {
			super(json);

			this.uriMokamintPublic = Objects.requireNonNull(json.getURIMokamintPublic(), "uriMokamintPublic cannot be null", InconsistentJsonException::new);
			this.uriMokamintRestricted = Objects.requireNonNull(json.getURIMokamintRestricted(), "uriMokamintRestricted cannot be null", InconsistentJsonException::new);
		}

		@Override
		public URI getURIMokamintPublic() {
			return uriMokamintPublic;
		}

		@Override
		public URI getURIMokamintRestricted() {
			return uriMokamintRestricted;
		}

		@Override
		public String toString() {
			var sb = new StringBuilder();

			sb.append("The following services have been published:\n");
			sb.append(" * " + asUri(getURI()) + ": the API of this Hotmoka node\n");
			sb.append(" * " + asUri(uriMokamintPublic) + ": the public API of the underlying Mokamint engine\n");
			sb.append(" * " + asUri(uriMokamintRestricted) + ": the restricted API of the underlying Mokamint engine\n");
			sb.append("\n");

			toStringNodeInit(sb);

			return sb.toString();
		}
	}
}