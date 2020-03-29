package io.takamaka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.tests.TakamakaTest;

class Loop3 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private MemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = mkMemoryBlockchain(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws TransactionException, IOException {
		blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("loop3.jar"), blockchain.takamakaCode()));
	}

	@Test @DisplayName("install jar then call to Loop.loop() fails")
	void callLoop() throws TransactionException, IOException, CodeExecutionException {
		TransactionReference loop = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("loop3.jar"), blockchain.takamakaCode()));

		TakamakaTest.throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () -> 
			blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
				(blockchain.account(0), _20_000, BigInteger.ONE, new Classpath(loop, true), new VoidMethodSignature("io.takamaka.tests.errors.loop3.Loop", "loop"))));
	}
}