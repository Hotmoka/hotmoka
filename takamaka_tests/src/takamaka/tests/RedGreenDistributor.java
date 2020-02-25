/**
 * 
 */
package takamaka.tests;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.CodeExecutionException;
import io.takamaka.code.memory.InitializedRedGreenMemoryBlockchain;

/**
 * A test for the remote purchase contract.
 */
class RedGreenDistributor extends TakamakaTest {
	private static final ClassType DISTRIBUTOR = new ClassType("io.takamaka.tests.redgreendistributor.Distributor");
	
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedRedGreenMemoryBlockchain blockchain;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new InitializedRedGreenMemoryBlockchain(Paths.get("../distribution/dist/io-takamaka-code-1.0.jar"),
			BigInteger.valueOf(1_100_000L), BigInteger.valueOf(1_100_000L), // green/red of first account
			BigInteger.valueOf(100_000L), BigInteger.ZERO, // green/red of second account
			BigInteger.valueOf(100_000L), BigInteger.ZERO, // green/red of third account
			BigInteger.ZERO, BigInteger.valueOf(100_000L), // green/red of fourth account
			BigInteger.ZERO, BigInteger.ZERO  // green/red of fifth account
		);

		TransactionReference distributor = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/redgreendistributor.jar")), blockchain.takamakaBase));

		classpath = new Classpath(distributor, true);
	}

	@Test @DisplayName("new RedGreenDistributor()")
	void createDistributor() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(DISTRIBUTOR)));
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 green and their red balance is zero")
	void createDistributorAndTwoPayees() throws TransactionException, CodeExecutionException {
		StorageReference distributor = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(DISTRIBUTOR)));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(1),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(2),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(0),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "distributeGreen", ClassType.BIG_INTEGER),
			distributor, new BigIntegerValue(BigInteger.valueOf(1_000))));

		BigIntegerValue balanceRed1 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(0),
			_20_000,
			BigInteger.ONE,
			classpath,
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			blockchain.account(1)));

		BigIntegerValue balanceRed2 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(0),
			_20_000,
			BigInteger.ONE,
			classpath,
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			blockchain.account(2)));

		Assertions.assertEquals(BigInteger.ZERO, balanceRed1.value);
		Assertions.assertEquals(BigInteger.ZERO, balanceRed2.value);
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 red and their red balance is 500")
	void createDistributorAndTwoPayeesThenDistributes1000Red() throws TransactionException, CodeExecutionException {
		StorageReference distributor = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(DISTRIBUTOR)));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(1),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(2),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(0),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "distributeRed", ClassType.BIG_INTEGER),
			distributor, new BigIntegerValue(BigInteger.valueOf(1_000))));

		BigIntegerValue balanceRed1 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(0),
			_20_000,
			BigInteger.ONE,
			classpath,
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			blockchain.account(1)));

		BigIntegerValue balanceRed2 = (BigIntegerValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(0),
			_20_000,
			BigInteger.ONE,
			classpath,
			new NonVoidMethodSignature(ClassType.TRGEOA, "getBalanceRed", ClassType.BIG_INTEGER),
			blockchain.account(2)));

		Assertions.assertEquals(500, balanceRed1.value.intValue());
		Assertions.assertEquals(500, balanceRed2.value.intValue());
	}

	@Test @DisplayName("distributeRed() cannot be called from an externally owned account that is not red/green")
	void distributeRedCannotBeCalledFromNOnRedGreen() throws TransactionException, CodeExecutionException {
		StorageReference distributor = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(DISTRIBUTOR)));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(1),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		));

		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			blockchain.account(2),
			_20_000,
			BigInteger.ONE,
			classpath,
			new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
			distributor
		));

		StorageReference eoa = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(
			blockchain.account(0),
			_20_000,
			BigInteger.ONE,
			classpath,
			new ConstructorSignature(ClassType.EOA, ClassType.BIG_INTEGER),
			new BigIntegerValue(_20_000)));

		throwsTransactionException(() ->
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
				eoa,
				_20_000,
				BigInteger.ONE,
				classpath,
				new VoidMethodSignature(DISTRIBUTOR, "distributeRed", ClassType.BIG_INTEGER),
				distributor, new BigIntegerValue(BigInteger.valueOf(1_000))))
		);
	}

	@Test @DisplayName("new RedGreenDistributor() then fails while adding a payee without green coins")
	void createDistributorThenFailsByAddingPayeeWithoutGreen() throws TransactionException, CodeExecutionException {
		StorageReference distributor = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _20_000, BigInteger.ONE, classpath, new ConstructorSignature(DISTRIBUTOR)));

		throwsTransactionException(() ->
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
				blockchain.account(3),
				_20_000,
				BigInteger.ONE,
				classpath,
				new VoidMethodSignature(DISTRIBUTOR, "addAsPayee"),
				distributor))
		);
	}
}