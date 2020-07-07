package io.hotmoka.tendermint.runs;

import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;

/**
 * Reopens an already created blockchain.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.tendermint/io.hotmoka.tendermint.runs.MainRecycle
 */
public class MainRecycle {
	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder()
			.setDelete(false) // reuse the state already created by a previous execution
			.build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
			System.out.println("takamakaCode: " + blockchain.getTakamakaCode());

			
		}
	}
}