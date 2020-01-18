package takamaka.tests.errors;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.takamaka.code.blockchain.NonWhiteListedCallException;
import io.takamaka.code.memory.InitializedMemoryBlockchain;
import takamaka.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod10 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"), _1_000_000_000);
	}

	@Test @DisplayName("new Random()")
	void testNonWhiteListedCall() {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				new ConstructorSignature(Random.class.getName())))
		);
	}
}