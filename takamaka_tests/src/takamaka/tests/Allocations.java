/**
 * 
 */
package takamaka.tests;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.memory.InitializedMemoryBlockchain;

/**
 * A test for the remote purchase contract.
 */
class Allocations extends TakamakaTest {

	private static final ClassType ALLOCATIONS = new ClassType("takamaka.tests.allocations.Allocations");

	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"),
			_1_000_000_000, BigInteger.valueOf(100_000L));

		TransactionReference allocations = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _200_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/allocations.jar")), blockchain.takamakaBase));

		classpath = new Classpath(allocations, true);
	}

	@Test @DisplayName("new Allocations()")
	void createAllocations() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _200_000, classpath, new ConstructorSignature(ALLOCATIONS)));
	}
}