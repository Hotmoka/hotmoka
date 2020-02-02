/**
 * 
 */
package takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.INT;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.GameteCreationTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreInitialTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.AbstractSequentialNode;
import io.hotmoka.nodes.CodeExecutionException;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.memory.MemoryBlockchain;

/**
 * A test for the remote purchase contract.
 */
class Purchase extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10000);

	private static final ClassType PURCHASE = new ClassType("io.takamaka.tests.remotepurchase.Purchase");

	private static final ConstructorSignature CONSTRUCTOR_PURCHASE = new ConstructorSignature("io.takamaka.tests.remotepurchase.Purchase", INT);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private AbstractSequentialNode blockchain;

	/**
	 * The first object, that holds all funds initially.
	 */
	private StorageReference gamete;

	/**
	 * The seller contract.
	 */
	private StorageReference seller;

	/**
	 * The buyer contract.
	 */
	private StorageReference buyer;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../distribution/dist/io-takamaka-code-1.0.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference purchase = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/purchase.jar")), takamakaBase));

		classpath = new Classpath(purchase, true);

		seller = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _10_000, classpath, new ConstructorSignature(ClassType.EOA, INT), new IntValue(10000)));

		buyer = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _10_000, classpath, new ConstructorSignature(ClassType.EOA, INT), new IntValue(10000)));
	}

	@Test @DisplayName("new Purchase(21)")
	void oddDeposit() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(seller, _10_000, classpath, CONSTRUCTOR_PURCHASE,
				new IntValue(21)))
		);
	}

	@Test @DisplayName("new Purchase(20)")
	void evenDeposit() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _10_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(18)")
	void buyerCheats() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _10_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(buyer, _10_000, classpath, new VoidMethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(18)))
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20)")
	void buyerHonest() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _10_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(buyer, _10_000, classpath, new VoidMethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(20)));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmReceived()")
	void confirmReceptionBeforePaying() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _10_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			blockchain.addInstanceMethodCallTransaction
				(new InstanceMethodCallTransactionRequest(buyer, _10_000, classpath, new VoidMethodSignature(PURCHASE, "confirmReceived"), purchase))
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20) and then purchase.confirmReception()")
	void buyerPaysAndConfirmReception() throws TransactionException, CodeExecutionException {
		StorageReference purchase = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(seller, _10_000, classpath, CONSTRUCTOR_PURCHASE,
			new IntValue(20)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(buyer, _10_000, classpath, new VoidMethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(20)));

		blockchain.addInstanceMethodCallTransaction
			(new InstanceMethodCallTransactionRequest(buyer, _10_000, classpath, new VoidMethodSignature(PURCHASE, "confirmReceived"), purchase));
	}
}