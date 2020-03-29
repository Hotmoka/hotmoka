package io.takamaka.tests.errors;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.memory.MemoryBlockchain;
import io.takamaka.tests.TakamakaTest;

class LegalCall1 extends TakamakaTest {
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
	void installJar() throws TransactionException, CodeExecutionException, IOException {
		blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
			bytesOf("legalcall1.jar"), blockchain.takamakaCode()));		
	}
}