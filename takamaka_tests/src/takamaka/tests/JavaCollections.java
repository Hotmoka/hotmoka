/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.NonVoidMethodSignature;
import io.takamaka.code.blockchain.NonWhiteListedCallException;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.request.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.request.StaticMethodCallTransactionRequest;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.StringValue;
import io.takamaka.code.memory.InitializedMemoryBlockchain;

/**
 * A test for the Java HashMap class.
 */
class JavaCollections extends TakamakaTest {

	private static final ClassType HASH_MAP_TESTS = new ClassType("io.takamaka.tests.javacollections.HashMapTests");
	private static final ClassType HASH_SET_TESTS = new ClassType("io.takamaka.tests.javacollections.HashSetTests");

	private static final BigInteger _200_000 = BigInteger.valueOf(200_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

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
		blockchain = new InitializedMemoryBlockchain(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"), ALL_FUNDS);
		TransactionReference collections = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _200_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/javacollections.jar")), blockchain.takamakaBase));

		classpath = new Classpath(collections, true);
	}

	@Test @DisplayName("HashMapTests.testToString1() == [how, are, hello, you, ?]")
	void toString1OnHashMap() throws TransactionException, CodeExecutionException {
		StringValue toString = (StringValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(blockchain.account(0), _200_000, classpath, new NonVoidMethodSignature(HASH_MAP_TESTS, "testToString1", ClassType.STRING)));
		assertEquals("[how, are, hello, you, ?]", toString.value);
	}

	@Test @DisplayName("HashMapTests.testToString2() == [how, are, hello, you, ?]")
	void toString2OnHashMap() throws TransactionException, CodeExecutionException {
		StringValue toString = (StringValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(blockchain.account(0), _200_000, classpath, new NonVoidMethodSignature(HASH_MAP_TESTS, "testToString2", ClassType.STRING)));
		assertEquals("[how, are, hello, you, ?]", toString.value);
	}

	@Test @DisplayName("HashMapTests.testToString3() fails with a run-time white-listing violation")
	void toString3OnHashMap() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
					(blockchain.account(0), _200_000, classpath, new NonVoidMethodSignature(HASH_MAP_TESTS, "testToString3", ClassType.STRING)))
		);
	}

	@Test @DisplayName("HashMapTests.testToString4() == [how, are, hello, you, ?]")
	void toString4OnHashMap() throws TransactionException, CodeExecutionException {
		StringValue toString = (StringValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(blockchain.account(0), _200_000, classpath, new NonVoidMethodSignature(HASH_MAP_TESTS, "testToString4", ClassType.STRING)));
		assertEquals("[are, io.takamaka.tests.javacollections.C@2a, hello, you, ?]", toString.value);
	}

	@Test @DisplayName("HashSetTests.testToString1() == [how, are, hello, you, ?]")
	void toString1OnHashSet() throws TransactionException, CodeExecutionException {
		StringValue toString = (StringValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(blockchain.account(0), _200_000, classpath, new NonVoidMethodSignature(HASH_SET_TESTS, "testToString1", ClassType.STRING)));
		assertEquals("[how, are, hello, you, ?]", toString.value);
	}

	@Test @DisplayName("HashSetTests.testToString2() == [how, are, hello, you, ?]")
	void toString2OnHashSet() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
				(blockchain.account(0), _200_000, classpath, new NonVoidMethodSignature(HASH_SET_TESTS, "testToString2", ClassType.STRING)))
		);
	}

	@Test @DisplayName("HashSetTests.testToString3() == [how, are, hello, you, ?]")
	void toString3OnHashSet() throws TransactionException, CodeExecutionException {
		StringValue toString = (StringValue) blockchain.addStaticMethodCallTransaction(new StaticMethodCallTransactionRequest
			(blockchain.account(0), _200_000, classpath, new NonVoidMethodSignature(HASH_SET_TESTS, "testToString3", ClassType.STRING)));
		assertEquals("[how, are, io.takamaka.tests.javacollections.C@2a, hello, you, ?]", toString.value);
	}
}