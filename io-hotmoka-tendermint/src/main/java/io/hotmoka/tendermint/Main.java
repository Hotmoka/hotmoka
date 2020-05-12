package io.hotmoka.tendermint;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.nodes.InitializedNode;

/**
 * Creates a brand new blockchain.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"));
			 InitializedNode node = InitializedNode.of(blockchain, BigInteger.valueOf(200_000), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000))) {

			System.out.println(node.takamakaCode());
			System.out.println(node.account(0));
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}
}