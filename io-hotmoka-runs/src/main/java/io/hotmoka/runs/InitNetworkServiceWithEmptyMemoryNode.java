package io.hotmoka.runs;

import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;

/**
 * An example that shows how to create a brand new empty memory blockchain and publish a server bound to it.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.InitNetworkServiceWithEmptyMemoryNode
 */
public class InitNetworkServiceWithEmptyMemoryNode extends Run {

	public static void main(String[] args) throws Exception {
		MemoryBlockchainConfig nodeConfig = new MemoryBlockchainConfig.Builder().build();
		ConsensusParams consensus = new ConsensusParams.Builder().build();
		NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();

		try (Node original = MemoryBlockchain.init(nodeConfig, consensus);
			 NodeService service = NodeService.of(networkConfig, original)) {

			pressEnterToExit();
		}
	}
}