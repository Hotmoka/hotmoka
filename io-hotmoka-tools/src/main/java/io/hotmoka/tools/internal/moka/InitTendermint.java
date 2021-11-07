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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.crypto.Account;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;
import io.hotmoka.views.InitializedNode;
import io.hotmoka.views.ManifestHelper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "init-tendermint",
	description = "Initializes a new Hotmoka node based on Tendermint",
	showDefaultValues = true)
public class InitTendermint extends AbstractCommand {

	@Parameters(description = "the initial balance of the gamete")
    private BigInteger balance;

	@Option(names = { "--balance-red" }, description = "the initial red balance of the gamete", defaultValue = "0")
    private BigInteger balanceRed;

	@Option(names = { "--password-of-gamete" }, description = "the password of the gamete account; if not specified, it will be asked interactively")
    private String passwordOfGamete;

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

	@Option(names = { "--dir" }, description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--takamaka-code" }, description = "the jar with the basic Takamaka classes that will be installed in the node", defaultValue = "modules/explicit/io-takamaka-code-1.0.5.jar")
	private Path takamakaCode;

	@Option(names = { "--tendermint-config" }, description = "the directory of the Tendermint configuration of the node", defaultValue = "io-hotmoka-tools/tendermint_configs/v1n0/node0")
	private Path tendermintConfig;

	@Option(names = { "--delete-tendermint-config" }, description = "deletes the directory of the Tendermint configuration after starting the node")
	private boolean deleteTendermintConfig;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;
		private final TendermintBlockchain node;
		private final InitializedNode initialized;

		private Run() throws Exception {
			passwordOfGamete = ensurePassword(passwordOfGamete, "the gamete account", nonInteractive, false);
			askForConfirmation();

			TendermintBlockchainConfig nodeConfig = new TendermintBlockchainConfig.Builder()
				.setTendermintConfigurationToClone(tendermintConfig)
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			networkConfig = new NodeServiceConfig.Builder()
				.setPort(port)
				.build();

			ConsensusParams consensus = new ConsensusParams.Builder()
				.allowUnsignedFaucet(openUnsignedFaucet)
				.ignoreGasPrice(ignoreGasPrice)
				.build();

			try (TendermintBlockchain node = this.node = TendermintBlockchain.init(nodeConfig, consensus);
				InitializedNode initialized = this.initialized = TendermintInitializedNode.of(node, consensus, passwordOfGamete, takamakaCode, balance, balanceRed);
				NodeService service = NodeService.of(networkConfig, node)) {

				cleanUp();
				printManifest();
				printBanner();
				dumpKeysOfGamete(dir);
				waitForEnterKey();
			}
		}

		private void cleanUp() throws IOException {
			if (deleteTendermintConfig)
				Files.walk(tendermintConfig)
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}

		private void askForConfirmation() {
			if (!nonInteractive)
				yesNo("Do you really want to start a new node at \"" + dir + "\" (old blocks and store will be lost) [Y/N] ");
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

		private void dumpKeysOfGamete(Path where) throws IOException, NoSuchAlgorithmException, ClassNotFoundException, TransactionRejectedException, TransactionException, CodeExecutionException {
			Account gamete = initialized.gamete();
			String fileName = gamete.dump(where);
			System.out.println("\nThe entropy of the gamete has been saved into the file " + fileName);
			Path bip39Path = where.resolve("gamete.bip39");
			gamete.bip39Words().dump(bip39Path);
			System.out.println("The BIP39 representation of the gamete has been saved into the file " + bip39Path + "\n");
		}
	}
}