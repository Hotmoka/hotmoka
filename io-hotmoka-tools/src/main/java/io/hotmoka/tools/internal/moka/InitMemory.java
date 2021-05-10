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

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.views.InitializedNode;
import io.hotmoka.views.ManifestHelper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init-memory",
	description = "Initializes a new Hotmoka node in memory",
	showDefaultValues = true)
public class InitMemory extends AbstractCommand {

	@Parameters(description = "the initial balance of the gamete")
    private BigInteger balance;

	@Option(names = { "--balance-red" }, description = "the initial red balance of the gamete", defaultValue = "0")
    private BigInteger balanceRed;

	@Option(names = { "--open-unsigned-faucet" }, description = "opens the unsigned faucet of the gamete") 
	private boolean openUnsignedFaucet;

	@Option(names = { "--ignore-gas-price" }, description = "accepts transactions regardless of their gas price") 
	private boolean ignoreGasPrice;

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--non-interactive" }, description = "runs in non-interactive mode")
	private boolean nonInteractive;

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8080")
	private int port;

	@Option(names = { "--takamaka-code" }, description = "the jar with the basic Takamaka classes that will be installed in the node", defaultValue = "modules/explicit/io-takamaka-code-1.0.0.jar")
	private Path takamakaCode;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;
		private final MemoryBlockchain node;
		private final InitializedNode initialized;

		private Run() throws Exception {
			askForConfirmation();

			MemoryBlockchainConfig nodeConfig = new MemoryBlockchainConfig.Builder()
				.setMaxGasPerViewTransaction(maxGasPerView)
				.build();

			networkConfig = new NodeServiceConfig.Builder()
				.setPort(port)
				.build();

			ConsensusParams consensus = new ConsensusParams.Builder()
				.allowUnsignedFaucet(openUnsignedFaucet)
				.ignoreGasPrice(ignoreGasPrice)
				.build();

			try (MemoryBlockchain node = this.node = MemoryBlockchain.init(nodeConfig, consensus);
				InitializedNode initialized = this.initialized = InitializedNode.of(node, consensus, takamakaCode, balance, balanceRed);
				NodeService service = NodeService.of(networkConfig, node)) {

				printManifest();
				printBanner();
				dumpKeysOfGamete();
				waitForEnterKey();
			}
		}

		private void askForConfirmation() {
			if (!nonInteractive)
				yesNo("Do you really want to start a new node at this place (old blocks and store will be lost) [Y/N] ");
		}

		private void waitForEnterKey() throws IOException {
			System.out.println("Press enter to exit this program and turn off the node");
			System.in.read();
		}

		private void printBanner() {
			System.out.println("The Hotmoka node has been published at localhost:" + networkConfig.port);
			System.out.println("Try for instance in a browser: http://localhost:" + networkConfig.port + "/get/manifest");
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException {
			System.out.println("\nThe following node has been initialized:\n" + new ManifestHelper(node));
		}

		private void dumpKeysOfGamete() throws IOException {
			String fileName = dumpKeys(initialized.gamete(), initialized.keysOfGamete());
			System.out.println("\nThe keys of the gamete have been saved into the file " + fileName + "\n");
		}
	}
}