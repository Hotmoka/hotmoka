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
import io.hotmoka.moka.internal.converters.MokamintLocalNodeConfigOptionConverter;
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
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.websockets.beans.api.InconsistentJsonException;
import io.mokamint.miner.local.LocalMiners;
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
	description = "Initialize a new disk node and publish a service to it.",
	showDefaultValues = true)
public class Init extends AbstractMokaCommand {

	@Parameters(description = "the initial supply of coins of the node, which goes to the gamete")
    private BigInteger initialSupply;

	@Parameters(description = "the Base58-encoded ed25519 public key to use for the gamete account")
    private String publicKeyOfGamete;

	@Parameters(description = "the path of the jar with the basic Takamaka classes that will be installed in the node")
	private Path takamakaCode;

	@Parameters(description = "the path of the plot file that the Mokamint miner will use for mining deadlines")
	private Path plot;

	@Parameters(description = "the path of the key pair of the Mokamint node, used to sign the blocks that it creates")
	private Path keysOfMokamintNode;

	@Parameters(description = "the path of the key pair of the plot file, used to sign the deadlines that the miner creates with that plot file")
	private Path keysOfPlot;

	@Parameters(description = "the path to the Mokamint configuration of the node", converter = MokamintLocalNodeConfigOptionConverter.class)
	private LocalNodeConfig mokamintConfig;

	@Option(names = "--delta-supply", description = "the amount of coins that can be minted during the life of the node, after which inflation becomes 0", defaultValue = "0")
    private BigInteger deltaSupply;

	@Option(names = "--chain-id", description = "the chain identifier of the network; if missing, the chain identifier from the underlying Mokamint engine will be used, as reported with --mokamint-config")
	private String chainId;

	@Option(names = "--signature", description = "the default signature algorithm to use for signing the requests to the node (ed25519, sha256dsa, qtesla1, qtesla3)",
			converter = SignatureOptionConverter.class, defaultValue = "ed25519")
	private SignatureAlgorithm signature;
	
	@Option(names = "--open-unsigned-faucet", description = "open the unsigned faucet of the gamete") 
	private boolean openUnsignedFaucet;

	@Option(names = "--initial-gas-price", description = "the initial price of a unit of gas", defaultValue = "100") 
	private BigInteger initialGasPrice;

	@Option(names = "--oblivion", description = "how quick the gas consumed at previous rewards is forgotten (0 = never, 1000000 = immediately); use 0 to keep the gas price constant", defaultValue = "250000") 
	private long oblivion;

	@Option(names = "--ignore-gas-price", description = "accept transactions regardless of their gas price") 
	private boolean ignoreGasPrice;

	@Option(names = "--max-gas-per-view", description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = "--yes", description = "assume yes when asked for confirmation; this is implied if --json is used")
	private boolean yes;

	@Option(names = "--port", description = "the network port where the service must be published", defaultValue="8001")
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
		String passwordOfKeysOfMokamintNodeAsString, passwordOfKeysOfPlotAsString;

		try {
			passwordOfKeysOfMokamintNodeAsString = new String(passwordOfKeysOfMokamintNode);
			passwordOfKeysOfPlotAsString = new String(passwordOfKeysOfPlot);

			if (json)
				yes = true;

			askForConfirmation();

			// the local configuration of the Hotmoka Mokamint node
			var mokamintNodeConfig = MokamintNodeConfigBuilders.defaults()
					.setMaxGasPerViewTransaction(maxGasPerView)
					.setDir(dir)
					.build();

			PublicKey publicKey;

			try {
				publicKey = signature.publicKeyFromEncoding(Base58.fromBase58String(publicKeyOfGamete));
			}
			catch (Base58ConversionException e) {
				throw new CommandException("The public key of the gamete is not in Base58 format", e);
			}
			catch (InvalidKeySpecException e) {
				throw new CommandException("The public key of the gamete is not valid for the " + signature + " signature algorithm");
			}

			// the configuration of the underlying Mokamint engine
			LocalNodeConfig mokamintConfig = this.mokamintConfig.toBuilder()
				.setDir(dir.resolve("mokamint")) // we replace the directory configuration
				.build();

			ConsensusConfig<?, ?> consensus;

			try {
				// the consensus configuration of the Hotmoka Mokamint node
				consensus = ConsensusConfigBuilders.defaults(signature)
						.allowUnsignedFaucet(openUnsignedFaucet)
						.setInitialGasPrice(initialGasPrice)
						.setSignatureForRequests(signature)
						.setOblivion(oblivion)
						.ignoreGasPrice(ignoreGasPrice)
						.setChainId(chainId != null ? chainId : mokamintConfig.getChainId())
						.setInitialSupply(initialSupply)
						.setFinalSupply(initialSupply.add(deltaSupply))
						.setPublicKeyOfGamete(publicKey)
						.build();
			}
			catch (InvalidKeyException e) {
				// this should not happen since we have created the public key with the same signature algorithm
				throw new RuntimeException(e);
			}

			KeyPair keysOfNode;
			try {
				keysOfNode = Entropies.load(keysOfMokamintNode).keys(passwordOfKeysOfMokamintNodeAsString, mokamintConfig.getSignatureForBlocks());
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + keysOfMokamintNode + "\"!", e);
			}

			KeyPair keysOfPlot;
			try {
				keysOfPlot = Entropies.load(this.keysOfPlot).keys(passwordOfKeysOfPlotAsString, mokamintConfig.getSignatureForDeadlines());
			}
			catch (IOException e) {
				throw new CommandException("Cannot access file \"" + this.keysOfPlot + "\"!", e);
			}

			try (var node = MokamintNodes.init(mokamintNodeConfig, mokamintConfig, keysOfNode, true)) {
				try (var plot = Plots.load(this.plot)) {
				var mokamintNode = node.getMokamintNode();
				var pakp = PlotAndKeyPairs.of(plot, keysOfPlot);

				try (var miner = LocalMiners.of(new PlotAndKeyPair[] { pakp })) {
					mokamintNode.add(miner).orElseThrow(() -> new CommandException("Could not add a miner to the Mokamint node"));

					// the next services will be closed when the node will be closed
					PublicNodeServices.open(mokamintNode, mokamintPort, 1800000, 1000, Optional.of(URI.create("ws://localhost:" + mokamintPort)));
					RestrictedNodeServices.open(mokamintNode, mokamintPortRestricted);

					try (var initialized = InitializedNodes.of(node, consensus, takamakaCode); var service = NodeServices.of(node, port)) {
						var output = new Output(initialized.gamete(),
								URI.create("ws://localhost:" + port),
								URI.create("ws://localhost:" + mokamintPort),
								URI.create("ws://localhost:" + mokamintPortRestricted));

						report(json, output, NodesMokamintInitOutputs.Encoder::new);
						waitForEnterKey();
					}
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
			passwordOfKeysOfMokamintNodeAsString = null;
			passwordOfKeysOfPlotAsString = null;
			Arrays.fill(passwordOfKeysOfMokamintNode, ' ');
			Arrays.fill(passwordOfKeysOfPlot, ' ');
		}
	}

	private void askForConfirmation() throws CommandException {
		if (!yes && !answerIsYes("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] "))
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