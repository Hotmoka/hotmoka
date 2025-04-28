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
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.cli.CommandException;
import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Base58ConversionException;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.api.SignatureAlgorithm;
import io.hotmoka.exceptions.Objects;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.moka.NodesMokamintInitOutputs;
import io.hotmoka.moka.api.nodes.mokamint.NodesMokamintInitOutput;
import io.hotmoka.moka.internal.AbstractMokaCommand;
import io.hotmoka.moka.internal.converters.ConsensusConfigOptionConverter;
import io.hotmoka.moka.internal.converters.MokamintLocalNodeConfigOptionConverter;
import io.hotmoka.moka.internal.converters.MokamintNodeConfigOptionConverter;
import io.hotmoka.moka.internal.converters.SignatureOptionConverter;
import io.hotmoka.moka.internal.json.NodesMokamintInitOutputJson;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.nodes.ConsensusConfig;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.mokamint.MokamintNodeConfigBuilders;
import io.hotmoka.node.mokamint.MokamintNodes;
import io.hotmoka.node.mokamint.api.MokamintNodeConfig;
import io.hotmoka.node.service.NodeServices;
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
	description = "Initialize a new Mokamint node and publish a service to it. This spawns both a Mokamint engine and a Mokamint node on top of that engine. The configurations of both can be provided through the --mokamint-config and (--node-local-config and --node-consensus-config), respectively, which, when missing, rely to defaults. In any case, such configurations can be updated with explicit values, such as --initial-supply.",
	showDefaultValues = true)
public class Init extends AbstractMokaCommand {

	@Parameters(description = "the path of the jar with the basic Takamaka classes that will be installed in the node")
	private Path takamakaCode;

	@Parameters(description = "the path of the plot file that the Mokamint miner will use for mining deadlines")
	private Path plot;

	@Parameters(description = "the path of the key pair of the Mokamint node, used to sign the blocks that it creates")
	private Path keysOfMokamintNode;

	@Parameters(description = "the path of the key pair of the plot file, used to sign the deadlines that the miner creates with that plot file")
	private Path keysOfPlot;

	@Option(names = "--mokamint-config", description = "the configuration of the underlying Mokamint engine", converter = MokamintLocalNodeConfigOptionConverter.class)
	private LocalNodeConfig mokamintConfig;

	@Option(names = "--node-local-config", description = "the local configuration of the Hotmoka node", converter = MokamintNodeConfigOptionConverter.class)
	private MokamintNodeConfig nodeLocalConfig;

	@Option(names = "--node-consensus-config", description = "the local configuration of the Hotmoka node", converter = ConsensusConfigOptionConverter.class)
	private ConsensusConfig<?, ?> nodeConsensusConfig;

	@Option(names = "--initial-supply", description = "the initial supply of coins of the node, which goes to the gamete")
	private BigInteger initialSupply;

	@Option(names = "--final-supply", description = "the final supply of coins of the node, after which inflation becomes 0")
	private BigInteger finalSupply;

	@Option(names = "--public-key-of-gamete", description = "the Base58-encoded ed25519 public key to use for the gamete account")
	private String publicKeyOfGamete;

	@Option(names = "--chain-id", description = "the chain identifier of the Hotmoka network")
	private String chainId;

	@Option(names = "--signature", description = "the default signature algorithm to use for signing the requests to the node (ed25519, sha256dsa, qtesla1, qtesla3)", converter = SignatureOptionConverter.class)
	private SignatureAlgorithm signature;
	
	@Option(names = "--open-unsigned-faucet", description = "open the unsigned faucet of the gamete") 
	private Boolean openUnsignedFaucet;

	@Option(names = "--initial-gas-price", description = "the initial price of a unit of gas") 
	private BigInteger initialGasPrice;

	@Option(names = "--oblivion", description = "how quick the gas consumed at previous rewards is forgotten (0 = never, 1000000 = immediately); use 0 to keep the gas price constant") 
	private Long oblivion;

	@Option(names = "--ignore-gas-price", description = "accept transactions regardless of their gas price") 
	private Boolean ignoreGasPrice;

	@Option(names = "--max-gas-per-view", description = "the maximal gas limit accepted for calls to @View methods") 
	private BigInteger maxGasPerView;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Option(names = "--port", description = "the network port where the service must be published", defaultValue = "8001")
	private int port;

	@Option(names = "--dir", description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = "--json", description = "print the output in JSON", defaultValue = "false")
	private boolean json;

	@Option(names = { "--mokamint-port", "--mokamint-port-public" }, description = "the network port where the public Mokamint service must be published", defaultValue="8030")
	private int mokamintPort;

	@Option(names = "--mokamint-port-restricted", description = "the network port where the restricted Mokamint service must be published", defaultValue="8031")
	private int mokamintPortRestricted;

	@Option(names = "--password-of-keys-of-mokamint-node", description = "the password of the key pair of the Mokamint node", interactive = true, defaultValue = "")
    private char[] passwordOfKeysOfMokamintNode;

	@Option(names = "--password-of-keys-of-plot", description = "the password of the key pair of the plot file", interactive = true, defaultValue = "")
    private char[] passwordOfKeysOfPlot;

	@Override
	protected void execute() throws CommandException {
		try {
			LocalNodeConfig mokamintConfig = mkMokamintConfig();
			MokamintNodeConfig localNodeConfig = mkLocalNodeConfig();
			ConsensusConfig<?, ?> consensus = mkConsensusNodeConfig();
			KeyPair keysOfNode = mkKeysOfMokamintNode(mokamintConfig);
			KeyPair keysOfPlot = mkKeysOfPlot(mokamintConfig);
			askForConfirmation(localNodeConfig.getDir());

			try (var node = MokamintNodes.init(localNodeConfig, mokamintConfig, keysOfNode, true); var plot = Plots.load(this.plot)) {
				try (var miner = LocalMiners.of(new PlotAndKeyPair[] { PlotAndKeyPairs.of(plot, keysOfPlot) })) {
					var mokamintNode = node.getMokamintNode();
					mokamintNode.add(miner).orElseThrow(() -> new CommandException("Could not add a miner to the Mokamint node"));

					// the next services will be closed when the node will be closed
					var mokamintNodePublicURI = URI.create("ws://localhost:" + mokamintPort);
					PublicNodeServices.open(mokamintNode, mokamintPort, 1800000, 1000, Optional.of(mokamintNodePublicURI));
					RestrictedNodeServices.open(mokamintNode, mokamintPortRestricted);

					try (var initialized = InitializedNodes.of(node, consensus, takamakaCode); var service = NodeServices.of(node, port)) {
						var output = new Output(initialized.gamete(), URI.create("ws://localhost:" + port), mokamintNodePublicURI, URI.create("ws://localhost:" + mokamintPortRestricted));
						report(json, output, NodesMokamintInitOutputs.Encoder::new);
						waitForEnterKey();
					}
				}
			}
			catch (WrongKeyException e) {
				throw new CommandException("The key pair does not contain the correct keys for the plot file");
			}
			catch (NoSuchAlgorithmException e) {
				throw new CommandException("The plot file refers to a non-available cryptographic algorithm", e);
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + takamakaCode + "\"!", e);
			}
			catch (TransactionRejectedException | TransactionException | CodeExecutionException e) {
				throw new CommandException("Could not initialize the node", e);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CommandException("The operation has been interrupted", e);
			}
			catch (NodeException | io.mokamint.node.api.NodeException | io.mokamint.plotter.api.PlotException | io.mokamint.miner.api.MinerException e) {
				throw new RuntimeException(e);
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

	private PublicKey mkPublicKeyOfGamete(SignatureAlgorithm signature) throws CommandException {
		try {
			return signature.publicKeyFromEncoding(Base58.fromBase58String(publicKeyOfGamete));
		}
		catch (Base58ConversionException e) {
			throw new CommandException("The public key of the gamete is not in Base58 format", e);
		}
		catch (InvalidKeySpecException e) {
			throw new CommandException("The public key of the gamete is not valid for the " + signature + " signature algorithm");
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
			if (dir != null)
				builder = builder.setDir(dir.resolve("mokamint"));

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
	private MokamintNodeConfig mkLocalNodeConfig() throws CommandException {
		var builder = nodeLocalConfig != null ? nodeLocalConfig.toBuilder() : MokamintNodeConfigBuilders.defaults();

		if (maxGasPerView != null)
			builder = builder.setMaxGasPerViewTransaction(maxGasPerView);

		if (dir != null)
			builder = builder.setDir(dir);

		return builder.build();
	}

	private ConsensusConfig<?, ?> mkConsensusNodeConfig() throws CommandException {
		try {
			var defaults = nodeConsensusConfig != null ? nodeConsensusConfig : ConsensusConfigBuilders.defaults().build();
			var builder = defaults.toBuilder();

			if (openUnsignedFaucet != null)
				builder = builder.allowUnsignedFaucet(openUnsignedFaucet);

			if (signature != null)
				builder = builder.setSignatureForRequests(signature);

			if (initialGasPrice != null)
				builder = builder.setInitialGasPrice(initialGasPrice);

			if (oblivion != null)
				builder = builder.setOblivion(oblivion);

			if (ignoreGasPrice != null)
				builder = builder.ignoreGasPrice(ignoreGasPrice);

			if (chainId != null)
				builder = builder.setChainId(chainId);

			if (initialSupply != null)
				builder = builder.setInitialSupply(initialSupply);

			if (finalSupply != null)
				builder = builder.setFinalSupply(finalSupply);

			if (publicKeyOfGamete != null)
				builder = builder.setPublicKeyOfGamete(mkPublicKeyOfGamete(signature != null ? signature : defaults.getSignatureForRequests()));

			return builder.build();
		}
		catch (InvalidKeyException e) {
			throw new CommandException("The public key is invalid for the selected signature algorithm", e);
		}
		catch (NoSuchAlgorithmException e) {
			throw new CommandException("Some cryptographic algorithm is not available", e);
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

	private void askForConfirmation(Path dir) throws CommandException {
		if (!yes && !json && !answerIsYes(asInteraction("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] ")))
			throw new CommandException("Stopped");
	}

	/**
	 * The output of this command.
	 */
	public static class Output implements NodesMokamintInitOutput {
		private final StorageReference gamete;
		private final URI uri;
		private final URI uriMokamintPublic;
		private final URI uriMokamintRestricted;

		private Output(StorageReference gamete, URI uri, URI uriMokamintPublic, URI uriMokamintRestricted) {
			this.gamete = gamete;
			this.uri = uri;
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
			this.gamete = Objects.requireNonNull(json.getGamete(), "gamete cannot be null", InconsistentJsonException::new).unmap()
				.asReference(value -> new InconsistentJsonException("The reference to the gamete must be a storage reference, not a " + value.getClass().getName()));
			this.uri = Objects.requireNonNull(json.getURI(), "uri cannot be null", InconsistentJsonException::new);
			this.uriMokamintPublic = Objects.requireNonNull(json.getURIMokamintPublic(), "uriMokamintPublic cannot be null", InconsistentJsonException::new);
			this.uriMokamintRestricted = Objects.requireNonNull(json.getURIMokamintRestricted(), "uriMokamintRestricted cannot be null", InconsistentJsonException::new);
		}

		@Override
		public StorageReference getGamete() {
			return gamete;
		}

		@Override
		public URI getURI() {
			return uri;
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
			sb.append(" * " + asUri(uri) + ": the API of this Hotmoka node\n");
			sb.append(" * " + asUri(uriMokamintPublic) + ": the public API of the underlying Mokamint engine\n");
			sb.append(" * " + asUri(uriMokamintRestricted) + ": the restricted API of the underlying Mokamint engine\n");
			sb.append("\n");
			sb.append("The owner of the key of the gamete can bind it now to its address with:\n");
			sb.append(asCommand("  moka keys bind file_containing_the_key_pair_of_the_gamete --password --url url_of_this_Hotmoka_node\n"));
			sb.append("or with:\n");
			sb.append(asCommand("  moka keys bind file_containing_the_key_pair_of_the_gamete --password --reference " + gamete + "\n"));
			sb.append("\n");
			sb.append(asInteraction("Press the enter key to stop this process and close this node: "));

			return sb.toString();
		}
	}
}