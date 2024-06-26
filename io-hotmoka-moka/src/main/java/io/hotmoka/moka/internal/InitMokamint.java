/*
Copyright 2024 Fausto Spoto

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

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.hotmoka.crypto.Base58;
import io.hotmoka.crypto.Entropies;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.ConsensusConfigBuilders;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.mokamint.MokamintNodeConfigBuilders;
import io.hotmoka.node.mokamint.MokamintNodes;
import io.hotmoka.node.service.NodeServices;
import io.mokamint.miner.local.LocalMiners;
import io.mokamint.node.local.LocalNodeConfigBuilders;
import io.mokamint.node.service.PublicNodeServices;
import io.mokamint.plotter.PlotAndKeyPairs;
import io.mokamint.plotter.Plots;
import io.mokamint.plotter.api.PlotAndKeyPair;
import io.takamaka.code.constants.Constants;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init-mokamint",
	description = "Initialize a new node based on Mokamint",
	showDefaultValues = true)
public class InitMokamint extends AbstractCommand {

	@Parameters(description = "the initial supply of coins of the node, which goes to the gamete")
    private BigInteger initialSupply;

	private final static String DELTA_SUPPLY_DEFAULT = "equal to the initial supply";
	@Option(names = { "--delta-supply" }, description = "the amount of coins that can be minted during the life of the node, after which inflation becomes 0", defaultValue = DELTA_SUPPLY_DEFAULT)
    private String deltaSupply;

	@Option(names = { "--initial-red-supply" }, description = "the initial supply of red coins of the node, which goes to the gamete", defaultValue = "0")
    private BigInteger initialRedSupply;

	@Option(names = { "--key-of-gamete" }, description = "the Base58-encoded public key of the gamete account")
    private String keyOfGamete;

	@Option(names = { "--open-unsigned-faucet" }, description = "opens the unsigned faucet of the gamete") 
	private boolean openUnsignedFaucet;

	@Option(names = { "--ignore-gas-price" }, description = "accepts transactions regardless of their gas price") 
	private boolean ignoreGasPrice;

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--initial-gas-price" }, description = "the initial price of a unit of gas", defaultValue = "100") 
	private BigInteger initialGasPrice;

	@Option(names = { "--oblivion" }, description = "how quick the gas consumed at previous rewards is forgotten (0 = never, 1000000 = immediately). Use 0 to keep the gas price constant", defaultValue = "250000") 
	private long oblivion;

	@Option(names = { "--inflation" }, description = "inflation added to the remuneration of the validators at each block (0 = 0%, 1000000 = 1%)", defaultValue = "1000000")
	private long inflation;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--port" }, description = "the network port for the publication of the Hotmoka service", defaultValue="8001")
	private int port;

	@Option(names = { "--mokamint-port" }, description = "the network port for the publication of the Mokamint service", defaultValue="8030")
	private int mokamintPort;

	@Option(names = { "--dir" }, description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--takamaka-code" }, description = "the jar with the basic Takamaka classes that will be installed in the node",
			defaultValue = "io-hotmoka-moka/modules/explicit/io-takamaka-code-TAKAMAKA-VERSION.jar")
	private String takamakaCode;

	@Option(names = { "--mokamint-config" }, description = "the path to the Mokamint configuration of the node", defaultValue = "io-hotmoka-moka/mokamint_configs/fast.cfg")
	private Path mokamintConfig;

	@Option(names = { "--keys-of-mokamint-node", "--keys" }, description = "the path to the keys of the Mokamint node", required = true)
	private Path keysOfMokamintNode;

	@Option(names = { "--password-of-keys-of-mokamint-node", "--password" }, description = "the password of the keys of the Mokamint node; if not specified, it will be asked interactively")
    private String passwordOfKeysOfMokamintNode;

	@Option(names = { "--plot" }, description = "the plot file that the Mokamint node will use for mining", required = true)
	private Path plot;

	@Option(names = { "--keys-of-plot" }, description = "the path to the keys of the plot file", required = true)
	private Path keysOfPlot;

	@Option(names = { "--password-of-keys-of-plot" }, description = "the password of the keys of the plot file; if not specified, it will be asked interactively")
    private String passwordOfKeysOfPlot;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final InitializedNode initialized;

		private Run() throws Exception {
			checkPublicKey(keyOfGamete);
			passwordOfKeysOfMokamintNode = ensurePassword(passwordOfKeysOfMokamintNode, "the Mokamint node", interactive, false);
			passwordOfKeysOfPlot = ensurePassword(passwordOfKeysOfPlot, "the plot", interactive, false);
			askForConfirmation();

			var nodeConfig = MokamintNodeConfigBuilders.defaults()
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			var mokamintConfig = LocalNodeConfigBuilders.load(InitMokamint.this.mokamintConfig)
				.setDir(dir.resolve("mokamint"))
				.build();

			BigInteger deltaSupply;
			if (DELTA_SUPPLY_DEFAULT.equals(InitMokamint.this.deltaSupply))
				deltaSupply = initialSupply;
			else
				deltaSupply = new BigInteger(InitMokamint.this.deltaSupply);

			var signature = SignatureAlgorithms.ed25519();

			var consensus = ConsensusConfigBuilders.defaults()
				.allowUnsignedFaucet(openUnsignedFaucet)
				.ignoreGasPrice(ignoreGasPrice)
				.setSignatureForRequests(signature)
				.setInitialGasPrice(initialGasPrice)
				.setOblivion(oblivion)
				.setInitialInflation(inflation)
				.setInitialSupply(initialSupply)
				.setFinalSupply(initialSupply.add(deltaSupply))
				.setInitialRedSupply(initialRedSupply)
				.setPublicKeyOfGamete(signature.publicKeyFromEncoding(Base58.decode(keyOfGamete)))
				.build();

			var keysOfNode = Entropies.load(keysOfMokamintNode).keys(passwordOfKeysOfMokamintNode, mokamintConfig.getSignatureForBlocks());
			var keysOfPlot = Entropies.load(InitMokamint.this.keysOfPlot).keys(passwordOfKeysOfPlot, mokamintConfig.getSignatureForDeadlines());

			try (var node = MokamintNodes.init(nodeConfig, mokamintConfig, keysOfNode);
			     var plot = Plots.load(InitMokamint.this.plot);
				 var miner = LocalMiners.of(new PlotAndKeyPair[] { PlotAndKeyPairs.of(plot, keysOfPlot) })) {

				node.getMokamintNode().add(miner).orElseThrow(() -> new CommandException("Could not add a miner to the test node"));

				// the next service will be closed when the node will be closed
				PublicNodeServices.open(node.getMokamintNode(), mokamintPort, 1800000, 1000, Optional.of(URI.create("ws://localhost:" + mokamintPort)));

				try (var initialized = this.initialized = InitializedNodes.of(node, consensus, Paths.get(takamakaCode.replace("TAKAMAKA-VERSION", Constants.TAKAMAKA_VERSION)));
				     var service = NodeServices.of(initialized, port)) {

					printManifest();
					printBanner();
					dumpInstructionsToBindGamete();
					waitForEnterKey();
				}
			}
		}

		private void askForConfirmation() {
			if (interactive)
				yesNo("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] ");
		}

		private void waitForEnterKey() throws IOException {
			System.out.println("Press enter to exit this program and turn off the node");
			System.in.read();
		}

		private void printBanner() {
			System.out.println("The node has been published at ws://localhost:" + port);
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
			System.out.println("\nThe following node has been initialized:\n" + ManifestHelpers.of(initialized));
		}

		private void dumpInstructionsToBindGamete() throws NodeException, TimeoutException, InterruptedException {
			System.out.println("\nThe owner of the key of the gamete can bind it to its address now:");
			System.out.println("  moka bind-key " + keyOfGamete + " --url url_of_this_node");
			System.out.println("or");
			System.out.println("  moka bind-key " + keyOfGamete + " --reference " + initialized.gamete() + "\n");
		}
	}
}