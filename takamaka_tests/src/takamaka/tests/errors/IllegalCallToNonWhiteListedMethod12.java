package takamaka.tests.errors;

import java.math.BigInteger;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.NonWhiteListedCallException;
import io.takamaka.code.memory.InitializedMemoryBlockchain;
import takamaka.tests.TakamakaTest;

class IllegalCallToNonWhiteListedMethod12 extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../distribution/dist/io-takamaka-code-1.0.jar"), _1_000_000_000);
	}

	@Test @DisplayName("new ExternallyOwnedAccount().hashCode()")
	void testNonWhiteListedCall() throws TransactionException, CodeExecutionException {
		StorageReference eoa = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
			new ConstructorSignature(ClassType.EOA)));

		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
				new NonVoidMethodSignature(ClassType.OBJECT, "hashCode", BasicTypes.INT), eoa))
		);
	}
}