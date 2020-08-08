/**
 * 
 */
package io.takamaka.code.tests;

import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.InternalFailureException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;

/**
 * A test for generating transactions in concurrency and check that everything
 * works without race conditions in the node.
 */
class Concurrency extends TakamakaTest {
	/**
	 * The number of threads that operate concurrently. At least 2 or this test will hang!
	 */
	private final static int THREADS_NUMBER = 100;

	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);

	@BeforeEach
	void beforeEach() throws Exception {
		// generate THREADS_NUMBER externally-owned accounts with a balance of ten millions each
		setNode(Stream.iterate(_100_000, __ -> _100_000).limit(THREADS_NUMBER).toArray(BigInteger[]::new));
	}

	private class Worker extends Thread {
		private final Random random = new Random();
		private final int num;

		private Worker(int num) {
			this.num = num;
		}

		@Override
		public void run() {
			try {
				while (true) {
					// we generate the number of a random distinct worker
					int other = random.ints().map(i -> i % THREADS_NUMBER).filter(i -> i >= 0 && i != num).findFirst().getAsInt();

					// we ask for the balance of the account bound to the this worker
					BigInteger ourBalance = ((BigIntegerValue) runViewInstanceMethodCallTransaction
						(privateKey(num), account(num), _10_000, ONE, takamakaCode(), CodeSignature.GET_BALANCE, account(num))).value;

					// we ask for the balance of the account bound to the other worker
					BigInteger otherBalance = ((BigIntegerValue) runViewInstanceMethodCallTransaction
						(privateKey(num), account(num), _10_000, ONE, takamakaCode(), CodeSignature.GET_BALANCE, account(other))).value;

					System.out.println(ourBalance + " vs " + otherBalance);

					if (ourBalance.subtract(otherBalance).signum() < 0) {
						// we are poorer than other: we sent him only 5,000 units of coin
						System.out.println("+5,000");
						addInstanceMethodCallTransaction(privateKey(num), account(num), _10_000, ONE, takamakaCode(),
							CodeSignature.RECEIVE_INT, account(other), new IntValue(5_000));
					}
					else {
						// we are richer than other: we sent him 10,000 units of coin
						System.out.println("+10,000");
						addInstanceMethodCallTransaction(privateKey(num), account(num), _10_000, ONE, takamakaCode(),
							CodeSignature.RECEIVE_INT, account(other), new IntValue(10_000));
					}
				}
			}
			catch (TransactionRejectedException e) {
				// eventually, the paying account "num" might have not enough gas to pay for a transaction
				if (e.getMessage().startsWith("caller has not enough funds to buy 10000 units of gas"))
					return;
				else
					throw InternalFailureException.of(e);
			}
			catch (TransactionException e) {
				// eventually, the paying account "num" might have not enough balance to pay the other account
				if (e.getMessage().startsWith("io.takamaka.code.lang.InsufficientFundsError"))
					return;
				else
					throw InternalFailureException.of(e);
			}
			catch (InvalidKeyException | SignatureException | CodeExecutionException e) {
				throw InternalFailureException.of(e);
			}
		}
	}

	private static void join(Thread thread) {
		try {
			thread.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Disabled
	@Test @DisplayName(THREADS_NUMBER + " threads generate transactions concurrently")
	void concurrently() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// we create an array of THREAD_NUMBER workers
		Worker[] workers = IntStream.iterate(0, i -> i + 1).limit(THREADS_NUMBER).mapToObj(Worker::new).toArray(Worker[]::new);

		// we start all threads
		Stream.of(workers).parallel().forEach(Thread::start);

		// we wait until all threads terminate
		Stream.of(workers).parallel().forEach(Concurrency::join);

		/*
		long start = System.currentTimeMillis();

		for (int i = 0; i < TRANSFERS; i++) {
			int num = random.nextInt(ACCOUNTS);
			StorageReference from = account(num);
			PrivateKey key = privateKey(num);

			StorageReference to;
			do {
				to = account(random.nextInt(ACCOUNTS));
			}
			while (to == from); // we want a different account than from

			int amount = 1 + random.nextInt(10);
			//System.out.println(amount + ": " + from + " -> " + to);
			if (i < TRANSFERS - 1)
				postInstanceMethodCallTransaction(key, from, _10_000, ZERO, takamakaCode(), CodeSignature.RECEIVE_INT, to, new IntValue(amount));
			else
				// the last transaction requires to wait until everything is committed
				addInstanceMethodCallTransaction(key, from, _10_000, ZERO, takamakaCode(), CodeSignature.RECEIVE_INT, to, new IntValue(amount));
		}

		long time = System.currentTimeMillis() - start;
		System.out.println(TRANSFERS + " money transfer transactions in " + time + "ms [" + (TRANSFERS * 1000L / time) + " tx/s]");

		// we compute the sum of the balances of the accounts
		BigInteger sum = ZERO;
		for (int i = 0; i < ACCOUNTS; i++)
			sum = sum.add(((BigIntegerValue) runViewInstanceMethodCallTransaction(privateKey(0), account(0), _10_000, ZERO, takamakaCode(), GET_BALANCE, account(i))).value);

		// no money got lost in translation
		assertEquals(sum, BigInteger.valueOf(ACCOUNTS).multiply(_10_000));
		*/
	}
}