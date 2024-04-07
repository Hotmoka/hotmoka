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

package io.hotmoka.moka.internal;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import io.hotmoka.helpers.ManifestHelpers;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.service.NodeServiceConfigBuilders;
import io.hotmoka.node.service.NodeServices;
import io.hotmoka.node.service.api.NodeServiceConfig;
import io.hotmoka.node.tendermint.TendermintNodeConfigBuilders;
import io.hotmoka.node.tendermint.TendermintNodes;
import io.hotmoka.node.tendermint.api.TendermintNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "resume-tendermint",
	description = "Resume an existing node based on Tendermint",
	showDefaultValues = true)
public class ResumeTendermint extends AbstractCommand {

	@Option(names = { "--tendermint-config" }, description = "the directory of the Tendermint configuration of the node", defaultValue = "io-hotmoka-tools/tendermint_configs/v1n0/node0")
	private Path tendermintConfig;

	@Option(names = { "--dir" }, description = "the directory that contains blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8080")
	private int port;

	@Override
	protected void execute() throws Exception {
		new Run();
	}

	private class Run {
		private final NodeServiceConfig networkConfig;
		private final TendermintNode node;

		private Run() throws Exception {
			var nodeConfig = TendermintNodeConfigBuilders.defaults()
				.setTendermintConfigurationToClone(tendermintConfig)
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			networkConfig = NodeServiceConfigBuilders.defaults()
				.setPort(port)
				.build();

			try (var node = this.node = TendermintNodes.resume(nodeConfig); var service = NodeServices.of(networkConfig, node)) {
				printManifest();
				printBanner();
				waitForEnterKey();
			}
		}

		private void waitForEnterKey() {
			System.out.println("Press enter to exit this program and turn off the node");
			System.console().readLine();
		}

		private void printBanner() {
			System.out.println("The node has been published at localhost:" + networkConfig.getPort());
			System.out.println("Try for instance in a browser: http://localhost:" + networkConfig.getPort() + "/get/manifest");
		}

		private void printManifest() throws TransactionRejectedException, TransactionException, CodeExecutionException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
			System.out.println("\nThe following node has been restarted:\n" + ManifestHelpers.of(node));
		}
	}
}