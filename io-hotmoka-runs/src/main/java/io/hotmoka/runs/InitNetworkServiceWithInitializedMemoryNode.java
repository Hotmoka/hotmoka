package io.hotmoka.runs;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.service.RemoteNode;
import io.hotmoka.service.RemoteNodeConfig;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;

/**
 * An example that shows how to create a brand new memory blockchain and publish a server bound to it.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.InitNetworkServiceWithInitializedMemoryNode
 */
public class InitNetworkServiceWithInitializedMemoryNode extends Run {

	public static void main(String[] args) throws Exception {
		MemoryBlockchainConfig nodeConfig = new MemoryBlockchainConfig.Builder().build();
		NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();
		RemoteNodeConfig remoteNodeConfig = new RemoteNodeConfig.Builder().build();
		ConsensusParams consensus = new ConsensusParams.Builder().build();
		Path takamakaCodeJar = Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar");

		try (Node original = MemoryBlockchain.init(nodeConfig, consensus);
			 Node initialized = InitializedNode.of(original, consensus, takamakaCodeJar, GREEN, RED);
			 NodeService service = NodeService.of(networkConfig, initialized)) {

			try (RemoteNode remote = RemoteNode.of(remoteNodeConfig)) {
				printManifest(remote);
			}

			pressEnterToExit();
		}
	}
}