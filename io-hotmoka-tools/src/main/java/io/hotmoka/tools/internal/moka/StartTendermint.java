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
import java.util.Comparator;

import io.hotmoka.nodes.ConsensusConfigs;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "start-tendermint",
	description = "Starts a new node based on Tendermint",
	showDefaultValues = true)
public class StartTendermint extends AbstractCommand {

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8080")
	private int port;

	@Option(names = { "--dir" }, description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--tendermint-config" }, description = "the directory of the Tendermint configuration of the node", defaultValue = "io-hotmoka-tools/tendermint_configs/v1n0/node0")
	private Path tendermintConfig;

	@Option(names = { "--delete-tendermint-config" }, description = "delete the directory of the Tendermint configuration after starting the node")
	private boolean deleteTendermintConfig;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;

		private Run() throws Exception {
			askForConfirmation();

			var nodeConfig = new TendermintBlockchainConfig.Builder()
				.setTendermintConfigurationToClone(tendermintConfig)
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			networkConfig = new NodeServiceConfig.Builder()
				.setPort(port)
				.build();

			var consensus = ConsensusConfigs.defaults()
				.build();

			try (var node = TendermintBlockchain.init(nodeConfig, consensus);
				var service = NodeService.of(networkConfig, node)) {

				cleanUp();
				printBanner();
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
	}
}