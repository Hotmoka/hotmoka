package io.hotmoka.runs;

import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;

import java.math.BigInteger;

/**
 * An example that shows how to create a brand new empty memory blockchain and publish a server bound to it.
 *
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 *
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartNetworkServiceWithEmptyMemoryNodeAndEmptySignature
 */
public class StartNetworkServiceWithEmptyMemoryNodeAndEmptySignature {

    public static void main(String[] args) throws Exception {
        MemoryBlockchainConfig config = new MemoryBlockchainConfig.Builder()
                .setMaxGasPerViewTransaction(BigInteger.valueOf(10_000_000))
                .build();

        ConsensusParams consensus = new ConsensusParams.Builder()
                .signRequestsWith("empty") // good for testing
                .allowUnsignedFaucet(true) // good for testing
                .setChainId("test")
                .ignoreGasPrice(true) // good for testing
                .build();

        NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();
        try (Node original = MemoryBlockchain.init(config, consensus);
             NodeService service = NodeService.of(networkConfig, original)) {

            System.out.println("\nPress enter to turn off the server and exit this program");
            System.console().readLine();
        }
    }
}
