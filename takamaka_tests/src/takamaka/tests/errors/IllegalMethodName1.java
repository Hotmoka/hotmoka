/**
 * 
 */
package takamaka.tests.errors;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.ConstructorSignature;
import io.takamaka.code.blockchain.NonVoidMethodSignature;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.types.BasicTypes;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.BooleanValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.memory.InitializedMemoryBlockchain;
import takamaka.tests.TakamakaTest;

class IllegalMethodName1 extends TakamakaTest {
	private static final ClassType A = new ClassType("io.takamaka.tests.errors.illegalmethodname1.A");
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
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
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"),
			_1_000_000_000, BigInteger.valueOf(100_000L), BigInteger.valueOf(1_000_000L));

		TransactionReference illegalmethod1 = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/illegalmethodname1.jar")), blockchain.takamakaBase));

		classpath = new Classpath(illegalmethod1, true);
	}

	@Test @DisplayName("new A() succeeds")
	void createA() throws TransactionException, CodeExecutionException {
		// cannot instantiate an abstract class
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, classpath, new ConstructorSignature(A)));
	}

	@Test @DisplayName("new A().foo() yields false")
	void createAThenCallsFoo() throws TransactionException, CodeExecutionException {
		// cannot instantiate an abstract class
		StorageReference a = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, classpath, new ConstructorSignature(A)));

		BooleanValue result = (BooleanValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(blockchain.account(0), _20_000, classpath,
			new NonVoidMethodSignature(A, "foo", BasicTypes.BOOLEAN),
			a));

		assertFalse(result.value);
	}
}