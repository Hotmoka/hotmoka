package io.hotmoka.tendermint.runs;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.InitializedNode;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;

/**
 * Creates a brand new blockchain.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"))) {
			StorageReference gamete = blockchain.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(blockchain.takamakaCode(), BigInteger.valueOf(999_999_999), BigInteger.valueOf(999_999_999)));

			try (InitializedNode node = InitializedNode.of(blockchain, gamete, BigInteger.valueOf(200_000), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000))) {
				System.out.println(node.takamakaCode());
				System.out.println(node.account(0));
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}
}