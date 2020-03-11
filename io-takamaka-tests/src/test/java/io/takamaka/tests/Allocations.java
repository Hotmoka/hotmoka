/**
 * 
 */
package io.takamaka.tests;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.nodes.CodeExecutionException;

/**
 * A test for the remote purchase contract.
 */
class Allocations extends TakamakaTest {

	private static final ClassType ALLOCATIONS = new ClassType("io.takamaka.tests.allocations.Allocations");

	private static final BigInteger _20_000_000 = BigInteger.valueOf(20_000_000);

	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private MemoryBlockchain blockchain;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = mkMemoryBlockchain(_1_000_000_000, BigInteger.valueOf(100_000L));

		TransactionReference allocations = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("allocations.jar"), blockchain.takamakaCode()));

		classpath = new Classpath(allocations, true);
	}

	@Test @DisplayName("new Allocations()")
	void createAllocations() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000_000, BigInteger.ONE, classpath, new ConstructorSignature(ALLOCATIONS)));
	}
}