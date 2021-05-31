package io.hotmoka.runs;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.stream.Collectors;

import io.hotmoka.beans.Coin;
import io.hotmoka.beans.SignatureAlgorithm;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.crypto.SignatureAlgorithmForTransactionRequests;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.memory.MemoryBlockchainConfig;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.Node;
import io.hotmoka.service.NodeService;
import io.hotmoka.service.NodeServiceConfig;
import io.hotmoka.views.InitializedNode;
import io.hotmoka.views.NodeWithJars;


/**
 * An example that shows how to create a brand new memory blockchain and publish a server bound to it.
 *
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 *
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature
 */
public class StartNetworkServiceWithInitializedMemoryNodeAndEmptySignature {
    protected static final BigInteger _10_000_000 = BigInteger.valueOf(10_000_000);

    public static void main(String[] args) throws Exception {
        MemoryBlockchainConfig config = new MemoryBlockchainConfig.Builder()
                .setMaxGasPerViewTransaction(_10_000_000)
                .build();

        ConsensusParams consensus = new ConsensusParams.Builder()
                .signRequestsWith("empty".toUpperCase()) // good for testing
                .allowUnsignedFaucet(true) // good for testing
                .setChainId("test")
                .ignoreGasPrice(true) // good for testing
                .build();

        BigInteger aLot = Coin.level7(10000000);
        Path takamakaCodeJar = Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar");
        Path basicJar = Paths.get("io-hotmoka-examples/target/io-hotmoka-examples-1.0.0-basic.jar");
        Path basicdependency = Paths.get("io-hotmoka-examples/target/io-hotmoka-examples-1.0.0-basicdependency.jar");


        NodeServiceConfig networkConfig = new NodeServiceConfig.Builder().setSpringBannerModeOn(true).build();
        try (Node original = MemoryBlockchain.init(config, consensus);
             InitializedNode initializedNode = InitializedNode.of(original, consensus, takamakaCodeJar, aLot, aLot);
             NodeService service = NodeService.of(networkConfig, initializedNode)) {

            NodeWithJars nodeWithJars = NodeWithJars.of(initializedNode, initializedNode.gamete(), initializedNode.keysOfGamete().getPrivate(), basicdependency);

            SignatureAlgorithm<SignedTransactionRequest> signatureAlgorithm = SignatureAlgorithmForTransactionRequests.mk(initializedNode.getNameOfSignatureAlgorithmForRequests());

            // we install a jar to test some methods of it
            TransactionReference jarTransaction = initializedNode.addJarStoreTransaction(new JarStoreTransactionRequest(
                    Signer.with(signatureAlgorithm, initializedNode.keysOfGamete()),
                    initializedNode.gamete(),
                    BigInteger.valueOf(4),
                    "test",
                    BigInteger.valueOf(1000000000),
                    BigInteger.valueOf(1),
                    initializedNode.getTakamakaCode(),
                    Files.readAllBytes(basicJar),
                    nodeWithJars.jar(0))
            );

            System.out.println("\nNetwork info:");
            System.out.println("\tio-takamaka-code-1.0.0.jar installed at: " + curl(new URL("http://localhost:8080/get/takamakaCode")));
            System.out.println("\tgamete reference: " + initializedNode.gamete().transaction.getHash());
            System.out.println("\tbasic jar reference: " + jarTransaction.getHash());
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
