/**
 * 
 */
package takamaka.tests.errors;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.memory.InitializedMemoryBlockchain;
import takamaka.verifier.VerificationException;

class CallerOutsideEntry {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"), _1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		try {
			blockchain.addJarStoreTransaction
				(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				Files.readAllBytes(Paths.get("../takamaka_examples/dist/calleroutsideentry.jar")), blockchain.takamakaBase));		
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof VerificationException)
				return;

			fail("wrong exception");
		}

		fail("no exception");
	}
}