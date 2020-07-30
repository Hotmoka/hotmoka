package io.hotmoka.network.runs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.network.NodeService;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;
import io.takamaka.code.constants.Constants;

/**
 * An example that shows how to create a brand new Tendermint Hotmoka blockchain
 * and publish a server bound to the node.
 * 
 * This class is meant to be run from the parent directory, after building the project,
 * with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.network/io.hotmoka.network.runs.Main
 */
public class Main {

	/**
	 * Initial green stake.
	 */
	private final static BigInteger GREEN = BigInteger.valueOf(999_999_999).pow(5);

	/**
	 * Initial red stake.
	 */
	private final static BigInteger RED = BigInteger.valueOf(999_999_999).pow(5);

	public static void main(String[] args) throws Exception {
		MemoryBlockchainConfig tendermintConfig = new MemoryBlockchainConfig.Builder().build();
		io.hotmoka.network.NodeServiceConfig networkConfig = new io.hotmoka.network.NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();

		// update version number when needed
		try (Node node = InitializedNode.of
			(MemoryBlockchain.of(tendermintConfig), Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar"),
			Constants.MANIFEST_NAME, Main.class.getName(), GREEN, RED)) {

			try (NodeService nodeRestService = NodeService.of(networkConfig, node)) {
				System.out.println("takamakaCode: " + curl(new URL("http://localhost:8080/get/takamakaCode")));
			}
		}

		System.exit(0);
	}

	private static String curl(URL url) throws IOException {
	    try (InputStream is = url.openStream();
	    	 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
	    	return br.lines().collect(Collectors.joining("\n"));
	    }
	}
}