package takamaka.tests.errors;

import java.math.BigInteger;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.whitelisting.MustRedefineHashCode;
import io.takamaka.whitelisting.WhiteListed;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.VoidMethodSignature;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.StaticMethodCallTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.StorageReference;
import takamaka.lang.NonWhiteListedCallException;
import takamaka.memory.InitializedMemoryBlockchain;
import takamaka.tests.TakamakaTest;

public class IllegalCallToNonWhiteListedMethod13 extends TakamakaTest {
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

	@Test @DisplayName("call with argument that does not redefine hashCode")
	void testNonWhiteListedCall() throws TransactionException, CodeExecutionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				new ConstructorSignature(ClassType.EOA)));

		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			blockchain.addStaticMethodCallTransaction
				(new StaticMethodCallTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				new VoidMethodSignature(IllegalCallToNonWhiteListedMethod13.class.getName(), "callee", ClassType.OBJECT),
				eoa))
		);
	}

	public static @WhiteListed void callee(@MustRedefineHashCode Object o) {}
}