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

import static io.hotmoka.node.StorageTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.Subscription;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class RemotePurchase extends HotmokaTest {
	private static final ClassType PURCHASE = StorageTypes.classNamed("io.hotmoka.examples.remotepurchase.Purchase");
	private static final String PURCHASE_CONFIRMED_NAME = PURCHASE + "$PurchaseConfirmed";
	private static final VoidMethodSignature CONFIRM_RECEIVED = MethodSignatures.ofVoid(PURCHASE, "confirmReceived");
	private static final VoidMethodSignature CONFIRM_PURCHASED = MethodSignatures.ofVoid(PURCHASE, "confirmPurchase", INT);
	private static final ConstructorSignature CONSTRUCTOR_PURCHASE = ConstructorSignatures.of(PURCHASE, INT);

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
			addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(21))
		);
	}

	@Test @DisplayName("new Purchase(20)")
	void evenDeposit() throws Exception {
		addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(18)")
	void buyerCheats() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, StorageValues.intOf(18))
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(18); no event is generated")
	void buyerCheatsNoEvent() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));

		var ok = new CompletableFuture<Boolean>();

		// the code of the smart contract uses events having the same contract as key
		try (Subscription subscription = node.subscribeToEvents(purchase, (__, event) -> ok.complete(false))) {
			throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
				addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, StorageValues.intOf(18))
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
	void buyerHonest() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));
		addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, StorageValues.intOf(20));
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20); a purchase event is generated")
	void buyerHonestConfirmationEvent() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));

		var received = new CompletableFuture<StorageReference>();
		StorageReference event;

		// the code of the smart contract uses events having the same contract as key
		try (Subscription subscription = node.subscribeToEvents(purchase, (__, _event) -> received.complete(_event))) {
			addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, StorageValues.intOf(20));
			event = received.get(20_000, TimeUnit.MILLISECONDS);
		}

		assertNotNull(event);
		assertEquals(PURCHASE_CONFIRMED_NAME, node.getClassTag(event).getClazz().getName());
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20); a purchase event is generated, subscription without key")
	void buyerHonestConfirmationEventNoKey() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));

		List<StorageReference> received = new ArrayList<>();

		// the use null to subscribe to all events
		try (Subscription subscription = node.subscribeToEvents(null, (__, _event) -> {
			// without key, many events might be notified, hence we look for one of a specific class
			received.add(_event);
		})) {
			addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, StorageValues.intOf(20));
			Thread.sleep(10_000);
		}

		boolean anyMatch = false;
		for (var event: received)
			if (PURCHASE_CONFIRMED_NAME.equals(node.getClassTag(event).getClazz().getName()))
				anyMatch = true;

		assertTrue(anyMatch);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20); subscription is closed and no purchase event is handled")
	void buyerHonestConfirmationEventSubscriptionClosed() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE,jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));

		AtomicBoolean ok = new AtomicBoolean(true);

		// the use null to subscribe to all events
		try (Subscription subscription = node.subscribeToEvents(null, (__, event) -> ok.set(false))) {			
		}

		// the subscription is closed now, hence the event generated below will not set ok to false
		addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, StorageValues.intOf(20));

		assertTrue(ok.get());
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmReceived()")
	void confirmReceptionBeforePaying() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_RECEIVED, purchase)
		);
	}

	@Test @DisplayName("seller runs purchase = new Purchase(20); buyer runs purchase.confirmPurchase(20) and then purchase.confirmReceived()")
	void buyerPaysAndConfirmReception() throws Exception {
		StorageReference purchase = addConstructorCallTransaction(privateKey(0), seller, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_PURCHASE, StorageValues.intOf(20));
		addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_PURCHASED, purchase, StorageValues.intOf(20));
		addInstanceVoidMethodCallTransaction(privateKey(1), buyer, _500_000, BigInteger.ONE, jar(), CONFIRM_RECEIVED, purchase);
	}
}