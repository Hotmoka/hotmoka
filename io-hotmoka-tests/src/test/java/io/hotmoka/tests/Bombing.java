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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
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
		setAccounts(Stream.generate(() -> _10_000).limit(ACCOUNTS)); // ACCOUNTS accounts
	}

	private final AtomicInteger ticket = new AtomicInteger();

	private void run(int num) {
		StorageReference from = account(num);
		PrivateKey key = privateKey(num);
		Random random = new Random();

		while (ticket.getAndIncrement() < TRANSFERS) {
			StorageReference to = random.ints(0, ACCOUNTS).filter(i -> i != num).mapToObj(i -> account(i)).findAny().get();
			int amount = 1 + random.nextInt(10);

			try {
				addInstanceMethodCallTransaction(key, from, _10_000, ZERO, takamakaCode(), CodeSignature.RECEIVE_INT, to, new IntValue(amount));
			}
			catch (InvalidKeyException | SignatureException | TransactionException | CodeExecutionException | TransactionRejectedException e) {
				e.printStackTrace();
			}
		}
	}

	@Test @DisplayName(TRANSFERS + " random transfers between accounts")
	void randomTranfers() throws InterruptedException, TransactionException, CodeExecutionException, TransactionRejectedException, ExecutionException {
		long start = System.currentTimeMillis();
		ExecutorService customThreadPool = new ForkJoinPool(ACCOUNTS);
		customThreadPool.submit(() -> IntStream.range(0, ACCOUNTS).parallel().forEach(this::run)).get();
		long time = System.currentTimeMillis() - start;
		System.out.printf("%d money transfer transactions in %d ms [%d tx/s]\n", TRANSFERS, time, TRANSFERS * 1000L / time);

		// we compute the sum of the balances of the accounts
		BigInteger sum = ZERO;
		for (int i = 0; i < ACCOUNTS; i++)
			sum = sum.add(((BigIntegerValue) runInstanceMethodCallTransaction(account(0), _10_000, takamakaCode(), CodeSignature.GET_BALANCE, account(i))).value);

		// no money got lost in translation
		assertEquals(sum, BigInteger.valueOf(ACCOUNTS).multiply(_10_000));
	}
}