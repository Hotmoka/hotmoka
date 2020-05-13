package io.hotmoka.tendermint.runs;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.hotmoka.nodes.InitializedNode;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;

public class Node_1_2 extends Node {

	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().setDelete(false).build();

		// we delete the blockchain directory
		deleteRecursively(config.dir);

		// we replace the blockchain directory with the initialized data for the node
		Files.createDirectories(config.dir);
		copyRecursively(Paths.get("2-nodes").resolve("node0"), config.dir.resolve("blocks"));

		// the first node installs the basic classes and creates some initial accounts
		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"));
			 InitializedNode node = InitializedNode.of(blockchain, BigInteger.valueOf(200_000), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000))) {

			System.out.println(node.takamakaCode());
			System.out.println(node.account(0));

			Thread.sleep(100_000);
		}
	}
}