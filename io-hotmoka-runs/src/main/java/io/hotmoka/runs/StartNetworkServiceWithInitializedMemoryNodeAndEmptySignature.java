package io.hotmoka.runs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.network.NodeService;
import io.hotmoka.network.NodeServiceConfig;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.views.InitializedNode;
import io.hotmoka.nodes.views.NodeWithJars;

/**
 * An example that shows how to create a brand new memory blockchain and publish a server bound to it.
 *
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 *
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature
 */
public class StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature {

    /**
     * Initial stakes.
     */
    private final static BigInteger GREEN = BigInteger.valueOf(1_000_000);
    private final static BigInteger RED = GREEN;

    public static void main(String[] args) throws Exception {
        MemoryBlockchainConfig nodeConfig = new MemoryBlockchainConfig.Builder().signRequestsWith("EMPTY").build();
        NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();
        Path takamakaCodeJar = Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar");
        Path basicJar = Paths.get("io-takamaka-examples/target/io-takamaka-examples-1.0.0-basic.jar");
        Path basicdependency = Paths.get("io-takamaka-examples/target/io-takamaka-examples-1.0.0-basicdependency.jar");

        try (Node original = MemoryBlockchain.of(nodeConfig);
             InitializedNode initialized = InitializedNode.of(original, takamakaCodeJar, StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature.class.getName(), GREEN, RED);
             NodeService service = NodeService.of(networkConfig, initialized)) {

            NodeWithJars nodeWithJars = NodeWithJars.of(initialized, initialized.gamete(), initialized.keysOfGamete().getPrivate(), basicdependency);


            // we install a jar to test some methods of it
            TransactionReference jarTransaction = initialized.addJarStoreTransaction(new JarStoreTransactionRequest(
                    Signer.with(initialized.getSignatureAlgorithmForRequests(), initialized.keysOfGamete()),
                    initialized.gamete(),
                    BigInteger.TWO,
                    StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature.class.getName(),
                    BigInteger.valueOf(10000),
                    BigInteger.ONE,
                    initialized.getTakamakaCode(),
                    Files.readAllBytes(basicJar),
                    nodeWithJars.jar(0))
            );

            System.out.println("\nio-takamaka-code-1.0.0.jar installed at " + curl(new URL("http://localhost:8080/get/takamakaCode")));
            System.out.println("\ngamete storage reference " + initialized.gamete().transaction.getHash());
            System.out.println("\nbasic jar storage reference " + jarTransaction.getHash());
            System.out.println("\nPress enter to turn off the server and exit this program");
            System.console().readLine();
        }
    }

    private static String curl(URL url) throws IOException {
        try (InputStream is = url.openStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }
}
