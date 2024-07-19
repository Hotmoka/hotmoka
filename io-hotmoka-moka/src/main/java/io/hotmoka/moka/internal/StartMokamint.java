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
import java.util.Optional;

import io.hotmoka.crypto.Entropies;
import io.hotmoka.node.mokamint.MokamintNodeConfigBuilders;
import io.hotmoka.node.mokamint.MokamintNodes;
import io.hotmoka.node.service.NodeServices;
import io.mokamint.miner.local.LocalMiners;
import io.mokamint.node.local.LocalNodeConfigBuilders;
import io.mokamint.node.service.PublicNodeServices;
import io.mokamint.plotter.PlotAndKeyPairs;
import io.mokamint.plotter.Plots;
import io.mokamint.plotter.api.PlotAndKeyPair;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "start-mokamint",
	description = "Start a new node based on Mokamint",
	showDefaultValues = true)
public class StartMokamint extends AbstractCommand {

	@Option(names = { "--max-gas-per-view" }, description = "the maximal gas limit accepted for calls to @View methods", defaultValue = "1000000") 
	private BigInteger maxGasPerView;

	@Option(names = { "--interactive" }, description = "run in interactive mode", defaultValue = "true")
	private boolean interactive;

	@Option(names = { "--port" }, description = "the network port for the publication of the service", defaultValue="8001")
	private int port;

	@Option(names = { "--dir" }, description = "the directory that will contain blocks and state of the node", defaultValue = "chain")
	private Path dir;

	@Option(names = { "--mokamint-config" }, description = "the path to the Mokamint configuration of the node", defaultValue = "io-hotmoka-moka/mokamint_configs/fast.cfg")
	private Path mokamintConfig;

	@Option(names = { "--mokamint-port" }, description = "the network port for the publication of the Mokamint service", defaultValue="8030")
	private int mokamintPort;

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

		private Run() throws Exception {
			passwordOfKeysOfMokamintNode = ensurePassword(passwordOfKeysOfMokamintNode, "the Mokamint node", interactive, false);
			passwordOfKeysOfPlot = ensurePassword(passwordOfKeysOfPlot, "the plot", interactive, false);
			askForConfirmation();

			var nodeConfig = MokamintNodeConfigBuilders.defaults()
				.setMaxGasPerViewTransaction(maxGasPerView)
				.setDir(dir)
				.build();

			var mokamintConfig = LocalNodeConfigBuilders.load(StartMokamint.this.mokamintConfig)
				.setDir(dir.resolve("mokamint"))
				.build();

			var keysOfNode = Entropies.load(keysOfMokamintNode).keys(passwordOfKeysOfMokamintNode, mokamintConfig.getSignatureForBlocks());
			var keysOfPlot = Entropies.load(StartMokamint.this.keysOfPlot).keys(passwordOfKeysOfPlot, mokamintConfig.getSignatureForDeadlines());

			try (var node = MokamintNodes.init(nodeConfig, mokamintConfig, keysOfNode, false);
				 var service = NodeServices.of(node, port);
			     var plot = Plots.load(StartMokamint.this.plot);
				 var miner = LocalMiners.of(new PlotAndKeyPair[] { PlotAndKeyPairs.of(plot, keysOfPlot) })) {

				node.getMokamintNode().add(miner).orElseThrow(() -> new CommandException("Could not add a miner to the test node"));

				// the service below will be closed when the node will be closed
				PublicNodeServices.open(node.getMokamintNode(), mokamintPort, 1800000, 1000, Optional.of(URI.create("ws://localhost:" + mokamintPort)));

				printBanner();
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
			System.out.println("The Hotmoka node has been published at ws://localhost:" + port);
			System.out.println("The Mokamint node has been published at ws://localhost:" + mokamintPort);
		}
	}
}