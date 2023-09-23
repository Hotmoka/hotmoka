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

package io.hotmoka.tools.internal.moka;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.constants.Constants;
import io.hotmoka.crypto.Base58;
import io.hotmoka.helpers.InitializedNodes;
import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.helpers.api.InitializedNode;
import io.hotmoka.node.SimpleConsensusConfigBuilders;
import io.hotmoka.node.disk.DiskNodeConfigBuilders;
import io.hotmoka.node.disk.DiskNodes;
import io.hotmoka.node.disk.api.DiskNode;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init-disk",
	description = "Initializes a new node in disk memory",
	showDefaultValues = true)
public class InitDiskNode extends AbstractCommand {

	@Parameters(description = "the initial supply of coins of the node, which goes to the gamete")
    private BigInteger initialSupply;

	private final static String DELTA_SUPPLY_DEFAULT = "equals to the initial supply";
	@Option(names = { "--delta-supply" }, description = "the amount of coins that can be minted during the life of the node, after which inflation becomes 0", defaultValue = DELTA_SUPPLY_DEFAULT)
    private String deltaSupply;

	@Option(names = { "--chain-id" }, description = "the chain identifier of the network", defaultValue = "")
	private String chainId;

	@Option(names = { "--initial-red-supply" }, description = "the initial supply of red coins of the node, which goes to the gamete", defaultValue = "0")
    private BigInteger initialRedSupply;

	@Option(names = { "--key-of-gamete" }, description = "the Base58-encoded public key of the gamete account")
    private String keyOfGamete;

	@Option(names = { "--open-unsigned-faucet" }, description = "opens the unsigned faucet of the gamete") 
	private boolean openUnsignedFaucet;

	@Option(names = { "--allow-mint-burn-from-gamete" }, description = "allows the gamete to mint and burn coins for free") 
	private boolean allowMintBurnFromGamete;

	@Option(names = { "--initial-gas-price" }, description = "the initial price of a unit of gas", defaultValue = "100") 
	private BigInteger initialGasPrice;

	@Option(names = { "--oblivion" }, description = "how quick the gas consumed at previous rewards is forgotten (0 = never, 1000000 = immediately). Use 0 to keep the gas price constant", defaultValue = "250000") 
	private long oblivion;

	@Option(names = { "--ignore-gas-price" }, description = "accepts transactions regardless of their gas price") 
	private boolean ignoreGasPrice;

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8080")
	private int port;

	@Option(names = { "--dir" }, description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--takamaka-code" }, description = "the jar with the basic Takamaka classes that will be installed in the node",
			defaultValue = "modules/explicit/io-takamaka-code-TAKAMAKA-VERSION.jar")
	private String takamakaCode;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;
		private final DiskNode node;
		private final InitializedNode initialized;

		private Run() throws Exception {
			checkPublicKey(keyOfGamete);
			askForConfirmation();

			var nodeConfig = DiskNodeConfigBuilders.defaults()
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			networkConfig = new NodeServiceConfig.Builder()
				.setPort(port)
				.build();

			BigInteger deltaSupply;
			if (DELTA_SUPPLY_DEFAULT.equals(InitDiskNode.this.deltaSupply))
				deltaSupply = initialSupply;
			else
				deltaSupply = new BigInteger(InitDiskNode.this.deltaSupply);

			var consensus = SimpleConsensusConfigBuilders.defaults()
				.allowUnsignedFaucet(openUnsignedFaucet)
				.allowMintBurnFromGamete(allowMintBurnFromGamete)
				.setInitialGasPrice(initialGasPrice)
				.setOblivion(oblivion)
				.ignoreGasPrice(ignoreGasPrice)
				.setChainId(chainId)
				.setInitialSupply(initialSupply)
				.setFinalSupply(initialSupply.add(deltaSupply))
				.setInitialRedSupply(initialRedSupply)
				.setPublicKeyOfGamete(Base64.getEncoder().encodeToString(Base58.decode(keyOfGamete)))
				.build();

			try (var node = this.node = DiskNodes.init(nodeConfig, consensus);
				var initialized = this.initialized = InitializedNodes.of(node, consensus,
						Paths.get(takamakaCode.replace("TAKAMAKA-VERSION", Constants.TAKAMAKA_VERSION)));
				var service = NodeService.of(networkConfig, node)) {

				printManifest();
				printBanner();
				dumpInstructionsToBindGamete();
				waitForEnterKey();
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
			System.out.println("The node has been published at localhost:" + networkConfig.port);
			System.out.println("Try for instance in a browser: http://localhost:" + networkConfig.port + "/get/manifest");
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			System.out.println("\nThe following node has been initialized:\n" + ManifestHelpers.of(node));
		}

		private void dumpInstructionsToBindGamete() {
			System.out.println("\nThe owner of the key of the gamete can bind it to its address now:");
			System.out.println("  moka bind-key " + keyOfGamete + " --url url_of_this_node");
			System.out.println("or");
			System.out.println("  moka bind-key " + keyOfGamete + " --reference " + initialized.gamete() + "\n");
		}
	}
}