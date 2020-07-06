package io.hotmoka.memory.runs;

import java.math.BigInteger;
import java.nio.file.Paths;

import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.nodes.views.NodeWithAccounts;
import io.takamaka.code.constants.Constants;
import io.hotmoka.memory.Config;
import io.hotmoka.memory.MemoryBlockchain;

/**
 * An example that shows how to create a brand new blockchain in memory.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --module io.hotmoka.memory/io.hotmoka.memory.runs.Main
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

		try (Node blockchain = MemoryBlockchain.of(config)) {
			// update version number when needed
			InitializedNode initializedView = InitializedNode.of
				(blockchain, Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"),
				Constants.MANIFEST_NAME, Main.class.getName(), GREEN, RED);
			NodeWithAccounts viewWithAccounts = NodeWithAccounts.of(initializedView, initializedView.keysOfGamete().getPrivate(), _200_000, _200_000, _200_000);
			System.out.println("takamakaCode: " + viewWithAccounts.getTakamakaCode());
			System.out.println("account #0: " + viewWithAccounts.account(0));
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}
}