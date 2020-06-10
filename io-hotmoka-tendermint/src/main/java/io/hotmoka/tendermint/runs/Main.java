package io.hotmoka.tendermint.runs;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;

/**
 * Creates a brand new blockchain.
 */
public class Main {

	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = BigInteger.valueOf(999_999_999).pow(5);

	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().build();

		try (Node blockchain = TendermintBlockchain.of(config)) {
			InitializedNode initializedView = InitializedNode.of(blockchain, Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.0.jar"), GREEN, RED);
			NodeWithAccounts viewWithAccounts = NodeWithAccounts.of(initializedView, initializedView.keysOfGamete().getPrivate(), _200_000, _200_000, _200_000);
			System.out.println("takamakaCode: " + viewWithAccounts.getTakamakaCode());
			System.out.println("account #0: " + viewWithAccounts.account(0));
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}
}