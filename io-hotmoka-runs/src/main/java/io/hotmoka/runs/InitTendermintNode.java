package io.hotmoka.runs;

import java.nio.file.Paths;

import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.views.TendermintInitializedNode;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.InitTendermintNode
 */
public class InitTendermintNode extends Run {

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().build();
		ConsensusParams consensus = new ConsensusParams.Builder().build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.init(config, consensus)) {
			TendermintInitializedNode.of(blockchain, consensus, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED);
			printManifest(blockchain);
		}
	}
}