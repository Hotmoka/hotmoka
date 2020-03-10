package io.takamaka.tests.errors;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.StaticMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.memory.MemoryBlockchain;
import io.hotmoka.nodes.CodeExecutionException;
import io.hotmoka.nodes.NonWhiteListedCallException;
import io.takamaka.code.whitelisting.MustRedefineHashCode;
import io.takamaka.code.whitelisting.WhiteListed;
import io.takamaka.tests.TakamakaTest;

public class IllegalCallToNonWhiteListedMethod13 extends TakamakaTest {
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

	@Test @DisplayName("call with argument that does not redefine hashCode")
	void testNonWhiteListedCall() throws TransactionException, CodeExecutionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
				new ConstructorSignature(ClassType.EOA)));

		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			blockchain.addStaticMethodCallTransaction
				(new StaticMethodCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaCode(),
				new VoidMethodSignature(IllegalCallToNonWhiteListedMethod13.class.getName(), "callee", ClassType.OBJECT),
				eoa))
		);
	}

	public static @WhiteListed void callee(@MustRedefineHashCode Object o) {}
}