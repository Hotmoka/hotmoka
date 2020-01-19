/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.BasicTypes;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.DeserializationError;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.memory.InitializedMemoryBlockchain;

/**
 * A test for the creation of classes with the same name but from different jars.
 */
class ClassSwap extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ConstructorSignature CONSTRUCTOR_C = new ConstructorSignature("C");

	private static final MethodSignature GET = new NonVoidMethodSignature("C", "get", BasicTypes.INT);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	/**
	 * The only account of the blockchain.
	 */
	private StorageReference gamete;

	/**
	 * The classpath for the class C whose method get() yields 13.
	 */
	private Classpath classpathC13;

	/**
	 * The classpath for the class C whose method get() yields 17.
	 */
	private Classpath classpathC17;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedMemoryBlockchain(Paths.get("../distribution/dist/io-takamaka-code-1.0.jar"), ALL_FUNDS);
		gamete = blockchain.account(0);

		TransactionReference c13 = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("jars/c13.jar")), blockchain.takamakaBase));

		TransactionReference c17 = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("jars/c17.jar")), blockchain.takamakaBase));

		classpathC13 = new Classpath(c13, true);
		classpathC17 = new Classpath(c17, true);
	}

	@Test @DisplayName("c13 new/get works in its classpath")
	void testC13() throws TransactionException, CodeExecutionException {
		StorageReference c13 = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpathC13, CONSTRUCTOR_C));

		IntValue get = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpathC13, GET, c13));

		assertSame(13, get.value);
	}

	@Test @DisplayName("c17 new/get works in its classpath")
	void testC17() throws TransactionException, CodeExecutionException {
		StorageReference c17 = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpathC17, CONSTRUCTOR_C));

		IntValue get = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpathC17, GET, c17));

		assertSame(17, get.value);
	}

	@Test @DisplayName("c13 new/get fails if classpath changed")
	void testC13SwapC17() throws TransactionException, CodeExecutionException {
		StorageReference c13 = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpathC13, CONSTRUCTOR_C));

		// the following call should fail since c13 was created from another jar
		throwsTransactionExceptionWithCause(DeserializationError.class, () ->
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _10_000, classpathC17, GET, c13))
		);
	}
}