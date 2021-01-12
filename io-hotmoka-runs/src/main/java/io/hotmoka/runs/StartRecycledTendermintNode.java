package io.hotmoka.runs;

import io.hotmoka.tendermint.TendermintBlockchainConfig;
import io.hotmoka.tendermint.TendermintBlockchain;

/**
 * Reopens an already created blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartRecycledTendermintNode
 */
public class StartRecycledTendermintNode extends Start {

	public static void main(String[] args) throws Exception {
		TendermintBlockchainConfig config = new TendermintBlockchainConfig.Builder()
			.build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.restart(config)) {
			System.out.println("takamakaCode: " + blockchain.getTakamakaCode());
			printManifest(blockchain);
		}
	}
}