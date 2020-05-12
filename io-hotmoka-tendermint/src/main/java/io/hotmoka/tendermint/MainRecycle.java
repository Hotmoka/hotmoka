package io.hotmoka.tendermint;

/**
 * Reopens an already created blockchain.
 */
public class MainRecycle {
	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder()
			.setDelete(false)
			.build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
			System.out.println(blockchain.takamakaCode());
		}
	}
}