/**
 * 
 */
package io.hotmoka.tests;

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;

/**
 * A test for generating many coin transfers and count their speed.
 */
class Bombing extends TakamakaTest {
	private final static int TRANSFERS = 1000;
	private final static int ACCOUNTS = 500;

	@BeforeEach
	void beforeEach() throws Exception {
		// ACCOUNTS accounts
		setAccounts(Stream.generate(() -> _10_000).limit(ACCOUNTS));
	}

	private final AtomicInteger ticket = new AtomicInteger();
	private final Random random = new Random(1311973L);

	private class Worker implements Runnable {
		private final StorageReference from;
		private final PrivateKey key;
	
		private Worker(int num) {
			from = account(num);
			key = privateKey(num);
		}

		@Override
		public void run() {
			while (true) {
				if (ticket.getAndIncrement() >= TRANSFERS)
					return;

				StorageReference to;
				do {
					to = account(random.nextInt(ACCOUNTS));
				}
				while (to == from); // we want a different account than from

				int amount = 1 + random.nextInt(10);
				try {
					addInstanceMethodCallTransaction(key, from, _10_000, ZERO, takamakaCode(), CodeSignature.RECEIVE_INT, to, new IntValue(amount));
				}
				catch (InvalidKeyException | SignatureException | TransactionException | CodeExecutionException | TransactionRejectedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Test @DisplayName(TRANSFERS + " random transfers between accounts")
	void randomTranfers() throws InterruptedException, TransactionException, CodeExecutionException, TransactionRejectedException {
		long start = System.currentTimeMillis();
		IntStream.range(0, ACCOUNTS).parallel().mapToObj(Worker::new).forEach(Worker::run);
		long time = System.currentTimeMillis() - start;
		System.out.println(TRANSFERS + " money transfer transactions in " + time + "ms [" + (TRANSFERS * 1000L / time) + " tx/s]");

		// we compute the sum of the balances of the accounts
		BigInteger sum = ZERO;
		for (int i = 0; i < ACCOUNTS; i++)
			sum = sum.add(((BigIntegerValue) runInstanceMethodCallTransaction(account(0), _10_000, takamakaCode(), CodeSignature.GET_BALANCE, account(i))).value);

		// no money got lost in translation
		assertEquals(sum, BigInteger.valueOf(ACCOUNTS).multiply(_10_000));
	}
}