package takamaka.tests.errors;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.NonVoidMethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.types.BasicTypes;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.StorageReference;
import takamaka.lang.NonWhiteListedCallException;
import takamaka.memory.InitializedMemoryBlockchain;

class IllegalCallToNonWhiteListedMethod12 {
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

	@Test @DisplayName("new ExternallyOwnedAccount().hashCode()")
	void testNonWhiteListedCall() throws TransactionException, CodeExecutionException, IOException {
		try {
			StorageReference eoa = blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				new ConstructorSignature(ClassType.EOA)));
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				new NonVoidMethodSignature(ClassType.OBJECT, "hashCode", BasicTypes.INT), eoa));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof NonWhiteListedCallException)
				return;

			e.printStackTrace();
			fail("wrong exception");
		}

		fail("no exception");
	}
}