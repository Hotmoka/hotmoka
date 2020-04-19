package io.hotmoka.tendermint;

import java.nio.file.Paths;

/**
 * Reopens an already created blockchain.
 */
public class MainRecycle {
	public static void main(String[] args) throws Exception {
		Config config = new Config(Paths.get("chain"), 26657, 26658);

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
			System.out.println(blockchain.takamakaCode());
			System.out.println(blockchain.account(0));
		}
	}
}