package io.hotmoka.runs;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.nodes.views.NodeWithAccounts;

/**
 * An example that shows how to create a brand new blockchain in memory.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --module io.hotmoka.memory/io.hotmoka.memory.runs.Main
 */
public class StartMemoryNode {

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
		MemoryBlockchainConfig config = new MemoryBlockchainConfig.Builder().build();

		try (Node blockchain = MemoryBlockchain.of(config)) {
			// update version number when needed
			InitializedNode initializedView = InitializedNode.of
				(blockchain, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"),
				StartMemoryNode.class.getName(), GREEN, RED);
			NodeWithAccounts viewWithAccounts = NodeWithAccounts.of(initializedView, initializedView.gamete(), initializedView.keysOfGamete().getPrivate(), _200_000, _200_000, _200_000);
			System.out.println("takamakaCode: " + viewWithAccounts.getTakamakaCode());
			System.out.println("account #0: " + viewWithAccounts.account(0));
		}
	}
}