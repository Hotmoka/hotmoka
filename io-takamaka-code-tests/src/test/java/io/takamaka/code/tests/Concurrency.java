/**
 * 
 */
package io.takamaka.code.tests;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
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
 * works without race conditions in the node. It uses a pool of threads to send
 * requests concurrently, including run requests, which is important since the
 * latter are executed in real parallelism. A race condition typically generates
 * an exception that makes the test fail.
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
		// generate THREADS_NUMBER externally-owned accounts with a balance of a hundred thousand each
		setNode(Stream.iterate(_100_000, __ -> _100_000).limit(THREADS_NUMBER).toArray(BigInteger[]::new));
	}

	AtomicInteger deads = new AtomicInteger();

	private class Worker extends Thread {
		private final Random random = new Random();
		private final int num;
		private boolean failed;

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

					// if we are poorer than other, we send him only 5,000 units of coin; otherwise, we send him 10,000 units
					int sent = ourBalance.subtract(otherBalance).signum() < 0 ? 5_000 : 10_000;
					addInstanceMethodCallTransaction(privateKey(num), account(num), _10_000, ONE, takamakaCode(),
						CodeSignature.RECEIVE_INT, account(other), new IntValue(sent));

					System.out.print("*");
				}
			}
			catch (TransactionRejectedException e) {
				// eventually, the paying account "num" might have not enough gas to pay for a transaction
				if (e.getMessage().startsWith("the payer has not enough funds to buy 10000 units of gas")) {
					System.out.println("\ndead #" + deads.addAndGet(1));
					return;
				}
				else {
					failed = true;
					throw InternalFailureException.of(e);
				}
			}
			catch (TransactionException e) {
				// eventually, the paying account "num" might have not enough balance to pay the other account
				if (e.getMessage().startsWith("io.takamaka.code.lang.InsufficientFundsError")) {
					System.out.println("\ndead #" + deads.addAndGet(1));
					return;
				}
				else {
					failed = true;
					throw InternalFailureException.of(e);
				}
			}
			catch (InvalidKeyException | SignatureException | CodeExecutionException | InternalFailureException e) {
				failed = true;
				throw InternalFailureException.of(e);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void join(Thread thread) {
		try {
			thread.join();
		}
		catch (InterruptedException e) {}
	}

	@Test @DisplayName(THREADS_NUMBER + " threads generate transactions concurrently")
	void concurrently() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		// we create an array of THREAD_NUMBER workers
		Worker[] workers = IntStream.iterate(0, i -> i + 1).limit(THREADS_NUMBER).mapToObj(Worker::new).toArray(Worker[]::new);

		// we start all threads
		Stream.of(workers).parallel().forEach(Thread::start);

		// we wait until all threads terminate
		Stream.of(workers).parallel().forEach(Concurrency::join);

		// the workers are expected to throw no exceptions, or otherwise that is typically sign of a race condition
		assertTrue(Stream.of(workers).noneMatch(worker -> worker.failed));
	}
}