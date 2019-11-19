/**
 * 
 */
package takamaka.tests;

import static io.takamaka.code.blockchain.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.takamaka.code.blockchain.AbstractSequentialBlockchain;
import io.takamaka.code.blockchain.Classpath;
import io.takamaka.code.blockchain.CodeExecutionException;
import io.takamaka.code.blockchain.ConstructorSignature;
import io.takamaka.code.blockchain.NonVoidMethodSignature;
import io.takamaka.code.blockchain.TransactionException;
import io.takamaka.code.blockchain.TransactionReference;
import io.takamaka.code.blockchain.VoidMethodSignature;
import io.takamaka.code.blockchain.request.ConstructorCallTransactionRequest;
import io.takamaka.code.blockchain.request.GameteCreationTransactionRequest;
import io.takamaka.code.blockchain.request.InstanceMethodCallTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreInitialTransactionRequest;
import io.takamaka.code.blockchain.request.JarStoreTransactionRequest;
import io.takamaka.code.blockchain.types.BasicTypes;
import io.takamaka.code.blockchain.types.ClassType;
import io.takamaka.code.blockchain.values.BigIntegerValue;
import io.takamaka.code.blockchain.values.IntValue;
import io.takamaka.code.blockchain.values.LongValue;
import io.takamaka.code.blockchain.values.StorageReference;
import io.takamaka.code.blockchain.values.StringValue;
import io.takamaka.code.memory.MemoryBlockchain;

/**
 * A test for a class that uses lambda expressions referring to entries.
 */
class Lambdas extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	private static final ClassType LAMBDAS = new ClassType("io.takamaka.tests.lambdas.Lambdas");

	private static final ConstructorSignature CONSTRUCTOR_LAMBDAS = new ConstructorSignature("io.takamaka.tests.lambdas.Lambdas", ClassType.BIG_INTEGER);

	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private AbstractSequentialBlockchain blockchain;

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference gamete;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_distribution/dist/io-takamaka-code-1.0.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference lambdas = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _100_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/lambdas.jar")), takamakaBase));

		classpath = new Classpath(lambdas, true);
	}

	@Test @DisplayName("new Lambdas()")
	void createLambdas() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
	}

	@Test @DisplayName("new Lambdas().invest(10)")
	void createLambdasThenInvest10() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(LAMBDAS, "invest", ClassType.BIG_INTEGER),
			lambdas, new BigIntegerValue(BigInteger.ONE)));
	}

	@Test @DisplayName("new Lambdas().testLambdaWithThis()")
	void testLambdaWithThis() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(LAMBDAS, "testLambdaWithThis"),
			lambdas));
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThis()")
	void testLambdaWithoutThis() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(LAMBDAS, "testLambdaWithoutThis"),
			lambdas));
	}

	@Test @DisplayName("new Lambdas().testLambdaWithoutThisGetStatic()")
	void testLambdaWithoutThisGetStatic() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(LAMBDAS, "testLambdaWithoutThisGetStatic"),
			lambdas));
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntry()")
	void testMethodReferenceToEntry() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		IntValue result = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new NonVoidMethodSignature(LAMBDAS, "testMethodReferenceToEntry", INT),
			lambdas));

		assertEquals(11, result.value);
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntryOfOtherClass()")
	void testMethodReferenceToEntryOfOtherClass() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(LAMBDAS, "testMethodReferenceToEntryOfOtherClass"),
			lambdas));
	}

	@Test @DisplayName("new Lambdas().testMethodReferenceToEntrySameContract()")
	void testMethodReferenceToEntrySameContract() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));

		throwsTransactionExceptionWithCause(ClassType.REQUIREMENT_VIOLATION_EXCEPTION.name, () ->
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new NonVoidMethodSignature(LAMBDAS, "testMethodReferenceToEntrySameContract", INT),
				lambdas))
		);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntry()")
	void testConstructorReferenceToEntry() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		IntValue result = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new NonVoidMethodSignature(LAMBDAS, "testConstructorReferenceToEntry", INT),
			lambdas));

		assertEquals(11, result.value);
	}

	@Test @DisplayName("new Lambdas().testConstructorReferenceToEntryPopResult()")
	void testConstructorReferenceToEntryPopResult() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath, new VoidMethodSignature(LAMBDAS, "testConstructorReferenceToEntryPopResult"),
			lambdas));
	}

	@Test @DisplayName("new Lambdas().whiteListChecks(13,1,1973)==7")
	void testWhiteListChecks() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		IntValue result = (IntValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath,
					new NonVoidMethodSignature(LAMBDAS, "whiteListChecks", INT, ClassType.OBJECT, ClassType.OBJECT, ClassType.OBJECT),
			lambdas, new BigIntegerValue(BigInteger.valueOf(13L)), new BigIntegerValue(BigInteger.valueOf(1L)), new BigIntegerValue(BigInteger.valueOf(1973L))));

		assertEquals(7, result.value);
	}

	@Test @DisplayName("new Lambdas().concatenation(\"hello\", \"hi\", self, 1973L, 13)==\"\"")
	void testConcatenation() throws TransactionException, CodeExecutionException {
		StorageReference lambdas = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(gamete, _10_000, classpath, CONSTRUCTOR_LAMBDAS, new BigIntegerValue(_100_000)));
		StringValue result = (StringValue) blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(gamete, _100_000, classpath,
					new NonVoidMethodSignature(LAMBDAS, "concatenation", ClassType.STRING,
					ClassType.STRING, ClassType.OBJECT, LAMBDAS, BasicTypes.LONG, INT),
			lambdas,
			new StringValue("hello"), new StringValue("hi"), lambdas, new LongValue(1973L), new IntValue(13)));

		assertEquals("hellohian externally owned account197313", result.value);
	}
}