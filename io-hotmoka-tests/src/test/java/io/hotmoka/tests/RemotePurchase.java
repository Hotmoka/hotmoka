/*
Copyright 2021 Fausto Spoto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.hotmoka.tests;

import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.constants.Constants;
import io.hotmoka.nodes.api.Node.Subscription;

/**
 * A test for the remote purchase contract.
 */
class RemotePurchase extends HotmokaTest {
	private static final ClassType PURCHASE = new ClassType("io.hotmoka.examples.remotepurchase.Purchase");
	private static final String PURCHASE_CONFIRMED_NAME = PURCHASE.name + "$PurchaseConfirmed";
	private static final VoidMethodSignature CONFIRM_RECEIVED = new VoidMethodSignature(PURCHASE, "confirmReceived");
	private static final VoidMethodSignature CONFIRM_PURCHASED = new VoidMethodSignature(PURCHASE, "confirmPurchase", INT);
	private static final ConstructorSignature CONSTRUCTOR_PURCHASE = new ConstructorSignature("io.hotmoka.examples.remotepurchase.Purchase", INT);

	/**
	 * The seller contract.
	 */
	private StorageReference seller;

	/**
	 * The buyer contract.
	 */
	private StorageReference buyer;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("remotepurchase.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(BigInteger.valueOf(100_000_000L), BigInteger.valueOf(100_000_000L));
		seller = account(0);
		buyer = account(1);
	}

	@Test @DisplayName("new Purchase(21)")
	void oddDeposit() {
		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, new IntValue(21))
		);
	}

	@Test @DisplayName("new Purchase(20)")
	void evenDeposit() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(18)")
	void buyerCheats() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, new IntValue(18))
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(18); no event is generated")
	void buyerCheatsNoEvent() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException, ExecutionException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));

		CompletableFuture<Boolean> ok = new CompletableFuture<>();

		// the code of the smart contract uses events having the same contract as key
		try (Subscription subscription = node.subscribeToEvents(purchase, (key, event) -> ok.complete(false))) {
			throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
				addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, new IntValue(18))
			);
		}

		try {
			ok.get(20_000, TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException e) { // this is what is expected to happen
			return;
		}

		fail("Expected TimeoutException");
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20)")
	void buyerHonest() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));
		addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, new IntValue(20));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20); a purchase event is generated")
	void buyerHonestConfirmationEvent() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException, ExecutionException, TimeoutException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));

		CompletableFuture<StorageReference> received = new CompletableFuture<>();
		StorageReference event;

		// the code of the smart contract uses events having the same contract as key
		try (Subscription subscription = node.subscribeToEvents(purchase, (__, _event) -> received.complete(_event))) {
			addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, new IntValue(20));
			event = received.get(20_000, TimeUnit.MILLISECONDS);
		}

		assertNotNull(event);
		assertEquals(PURCHASE_CONFIRMED_NAME, node.getClassTag(event).clazz.name);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20); a purchase event is generated, subscription without key")
	void buyerHonestConfirmationEventNoKey() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, InterruptedException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));

		List<StorageReference> received = new ArrayList<>();

		// the use null to subscribe to all events
		try (Subscription subscription = node.subscribeToEvents(null, (__, _event) -> {
			// without key, many events might be notified, hence we look for one of a specific class
			received.add(_event);
		})) {
			addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, new IntValue(20));
			Thread.sleep(10_000);
		}

		assertTrue(received.stream().anyMatch(event -> PURCHASE_CONFIRMED_NAME.equals(node.getClassTag(event).clazz.name)));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20); subscription is closed and no purchase event is handled")
	void buyerHonestConfirmationEventSubscriptionClosed() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));

		AtomicBoolean ok = new AtomicBoolean(true);

		// the use null to subscribe to all events
		try (Subscription subscription = node.subscribeToEvents(null, (key, event) -> ok.set(false))) {			
		}

		// the subscription is closed now, hence the event generated below will not set ok to false
		addInstanceMethodCallTransaction(privateKey(1), buyer, _100_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, new IntValue(20));

		assertTrue(ok.get());
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmReceived()")
	void confirmReceptionBeforePaying() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_RECEIVED, purchase)
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20) and then purchase.confirmReceived()")
	void buyerPaysAndConfirmReception() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, new IntValue(20));
		addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, new IntValue(20));
		addInstanceMethodCallTransaction(privateKey(1), buyer, _50_000, BigInteger.ONE, jar(), CONFIRM_RECEIVED, purchase);
	}
}