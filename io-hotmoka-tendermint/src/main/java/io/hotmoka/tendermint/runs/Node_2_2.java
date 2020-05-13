package io.hotmoka.tendermint.runs;

import java.nio.file.Files;
import java.nio.file.Paths;

import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;

public class Node_2_2 extends Node {

	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().setDelete(false).build();

		// we delete the blockchain directory
		deleteRecursively(config.dir);

		// we replace the blockchain directory with the initialized data for the peer
		Files.createDirectories(config.dir);
		copyRecursively(Paths.get("2-nodes").resolve("node1"), config.dir.resolve("blocks"));

		// nodes different from the first just listen to what the other do
		try (TendermintBlockchain node = TendermintBlockchain.of(config)) {
			while (true) {
				System.out.println(node.takamakaCode());
				Thread.sleep(1000);
			}
		}
	}
}