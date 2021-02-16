package io.hotmoka.runs;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.network.RemoteNode;
import io.hotmoka.network.RemoteNodeConfig;
import io.hotmoka.nodes.ConsensusParams;
import io.hotmoka.nodes.GasHelper;
import io.hotmoka.nodes.Node;
import io.hotmoka.nodes.NonceHelper;
import io.hotmoka.nodes.views.InitializedNode;

/**
 * An example that shows how to connect to a Hotmoka node published online and send a code installation request.
 * 
 * This class is meant to be run from the parent directory, after building the project, with this command-line:
 * 
 * java --module-path modules/explicit:modules/automatic --class-path "modules/unnamed/*" --module io.hotmoka.runs/io.hotmoka.runs.InstallC1
 */
public class InstallC3 {

	public static void main(String[] args) throws Exception {
		RemoteNodeConfig config = new RemoteNodeConfig.Builder()
			.setURL("localhost:8080")
			.build();

		ConsensusParams consensus = new ConsensusParams.Builder()
			.setChainId("test")
			.build();

		System.out.print("Connecting to the blockchain node at localhost:8080... ");
		try (Node node = RemoteNode.of(config)) {
			System.out.println("done");
			Path takamakaCodePath = Paths.get("modules/explicit/io-takamaka-code-1.0.0.jar");
			Path c1Path = Paths.get("io-hotmoka-examples/target/io-hotmoka-examples-1.0.0-wtsc2021_c3.jar");

			BigInteger GREEN_AMOUNT = BigInteger.valueOf(100_000_000);
			BigInteger RED_AMOUNT = BigInteger.ZERO;

			System.out.print("Installing the Takamaka runtime in the node... ");
			InitializedNode initialized = InitializedNode.of(node, consensus, takamakaCodePath, GREEN_AMOUNT, RED_AMOUNT);
			System.out.println("done");

			System.out.print("Installing C3 in the node... ");
			GasHelper gasHelper = new GasHelper(node);
			NonceHelper nonceHelper = new NonceHelper(node);

			// we let the gamete pay, for simplicity, so that we do not need to create a new account
			TransactionReference ref = node.addJarStoreTransaction(new JarStoreTransactionRequest
				(Signer.with(node.getSignatureAlgorithmForRequests(), initialized.keysOfGamete()), // signer of the gamete
				initialized.gamete(), // caller (the gamete)
				nonceHelper.getNonceOf(initialized.gamete()), // nonce
				"test", // chain id
				BigInteger.valueOf(100_000L), // gas limit
				gasHelper.getGasPrice(), // gas price
				node.getTakamakaCode(), // classpath (the Takamaka runtime)
				Files.readAllBytes(c1Path), // bytes of the jar to install
				node.getTakamakaCode())); // dependencies of the jar to install: only the Takamaka runtime
			System.out.println("done (on-chain verification succeeded)");
			System.out.println("C1.jar installed at address " + ref);
			System.exit(0);
		}
	}
}