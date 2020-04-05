/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.INT;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class RemotePurchase extends TakamakaTest {

	private static final BigInteger _10_000 = BigInteger.valueOf(10000);

	private static final ClassType PURCHASE = new ClassType("io.takamaka.tests.remotepurchase.Purchase");

	private static final ConstructorSignature CONSTRUCTOR_PURCHASE = new ConstructorSignature("io.takamaka.tests.remotepurchase.Purchase", INT);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

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
		mkBlockchain(BigInteger.valueOf(100_000_000L), BigInteger.valueOf(100_000_000L));
		seller = account(0);
		buyer = account(1);

		TransactionReference purchase = addJarStoreTransaction(seller, _20_000, BigInteger.ONE, takamakaCode(), bytesOf("remotepurchase.jar"), takamakaCode());
		classpath = new Classpath(purchase, true);
	}

	@Test @DisplayName("new Purchase(21)")
	void oddDeposit() throws TransactionException, CodeExecutionException {
		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addConstructorCallTransaction(seller, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_PURCHASE, new IntValue(21))
		);
	}

	@Test @DisplayName("new Purchase(20)")
	void evenDeposit() throws TransactionException, CodeExecutionException {
		addConstructorCallTransaction(seller, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_PURCHASE, new IntValue(20));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(18)")
	void buyerCheats() throws TransactionException, CodeExecutionException {
		StorageReference purchase = addConstructorCallTransaction(seller, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_PURCHASE, new IntValue(20));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(buyer, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(18))
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20)")
	void buyerHonest() throws TransactionException, CodeExecutionException {
		StorageReference purchase = addConstructorCallTransaction(seller, _10_000, BigInteger.ONE,classpath, CONSTRUCTOR_PURCHASE, new IntValue(20));
		addInstanceMethodCallTransaction(buyer, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(20));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmReceived()")
	void confirmReceptionBeforePaying() throws TransactionException, CodeExecutionException {
		StorageReference purchase = addConstructorCallTransaction(seller, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_PURCHASE, new IntValue(20));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(buyer, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(PURCHASE, "confirmReceived"), purchase)
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20) and then purchase.confirmReception()")
	void buyerPaysAndConfirmReception() throws TransactionException, CodeExecutionException {
		StorageReference purchase = addConstructorCallTransaction(seller, _10_000, BigInteger.ONE, classpath, CONSTRUCTOR_PURCHASE, new IntValue(20));
		postInstanceMethodCallTransaction(buyer, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(PURCHASE, "confirmPurchase", INT), purchase, new IntValue(20));
		addInstanceMethodCallTransaction(buyer, _10_000, BigInteger.ONE, classpath, new VoidMethodSignature(PURCHASE, "confirmReceived"), purchase);
	}
}