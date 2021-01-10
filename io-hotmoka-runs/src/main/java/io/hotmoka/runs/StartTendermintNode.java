package io.hotmoka.runs;

import java.math.BigInteger;
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
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartTendermintNode
 */
public class StartTendermintNode extends Start {

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = GREEN;

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder().build();
		ConsensusParams consensus = new ConsensusParams.Builder().build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
			TendermintInitializedNode.of(blockchain, consensus, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"), GREEN, RED);

			printManifest(blockchain);
		}
	}
}