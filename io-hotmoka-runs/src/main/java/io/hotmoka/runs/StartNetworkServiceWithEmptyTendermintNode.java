package io.hotmoka.runs;

import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.nodes.Node;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;

/**
 * An example that shows how to create a brand new empty Tendermint blockchain and publish a server bound to it.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.network/io.hotmoka.runs.StartNetworkServiceWithEmptyTendermintNode
 */
public class StartNetworkServiceWithEmptyTendermintNode {

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig nodeConfig = new TendermintBlockchainConfig.Builder().build();
		NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();

		try (Node original = TendermintBlockchain.of(nodeConfig);
			 NodeService service = NodeService.of(networkConfig, original)) {

			System.out.println("\nPress enter to turn off the server and exit this program");
			System.console().readLine();
		}
	}
}