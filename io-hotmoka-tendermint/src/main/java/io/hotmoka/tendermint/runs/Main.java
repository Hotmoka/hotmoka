package io.hotmoka.tendermint.runs;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InitializationTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.RedGreenGameteCreationTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.InitializedNode;
import io.hotmoka.tendermint.Config;
import io.hotmoka.tendermint.TendermintBlockchain;
import io.takamaka.code.constants.Constants;

/**
 * Creates a brand new blockchain.
 */
public class Main {
	public static void main(String[] args) throws Exception {
		Config config = new Config.Builder().build();

		try (TendermintBlockchain blockchain = TendermintBlockchain.of(config)) {
			TransactionReference takamakaCode = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../io-takamaka-code/target/io-takamaka-code-1.0.jar"))));
			// the gamete has both red and green coins, enough for all tests
			StorageReference gamete = blockchain.addRedGreenGameteCreationTransaction(new RedGreenGameteCreationTransactionRequest(takamakaCode, BigInteger.valueOf(999_999_999).pow(5), BigInteger.valueOf(999_999_999).pow(5)));
			StorageReference manifest = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(gamete, BigInteger.ZERO, BigInteger.valueOf(10_000), BigInteger.ZERO, takamakaCode, new ConstructorSignature(Constants.MANIFEST_NAME, ClassType.RGEOA), gamete));
			blockchain.addInitializationTransaction(new InitializationTransactionRequest(takamakaCode, manifest));

			try (InitializedNode node = InitializedNode.of(blockchain, BigInteger.valueOf(200_000), BigInteger.valueOf(200_000), BigInteger.valueOf(200_000))) {
				System.out.println(node.getTakamakaCode());
				System.out.println(node.account(0));
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}
}