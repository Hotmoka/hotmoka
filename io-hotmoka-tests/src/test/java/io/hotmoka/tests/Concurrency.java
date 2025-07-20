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

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.helpers.UnexpectedValueException;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for generating transactions in concurrency and check that everything
 * works without race conditions in the node. It uses a pool of threads to send
 * requests concurrently, including run requests, which is important since the
 * latter are executed in real parallelism. A race condition would generate
 * an exception, which makes the test fail.
 */
class Concurrency extends HotmokaTest {

	private final static Logger LOGGER = Logger.getLogger(Concurrency.class.getName());

	/**
	 * The number of threads that operate concurrently. At least 2 or this test will hang!
	 */
	private static int NUMBER_OF_THREADS = 100;

	@BeforeAll
	static void beforeAll() {
		String cheapTests = System.getProperty("cheapTests");
		if ("true".equals(cheapTests)) {
			LOGGER.info("Running in cheap mode since cheapTests = true");
			NUMBER_OF_THREADS = 4;
		}
	}

	@BeforeEach
	void beforeEach() throws Exception {
		// generate NUMBER_OF_THREADS externally-owned accounts with a balance of a hundred thousand each
		setAccounts(Stream.generate(() -> _500_000).limit(NUMBER_OF_THREADS));
	}

	@Test @DisplayName("More threads generate transactions concurrently")
	void concurrently() throws InterruptedException, ExecutionException {
		var remaining = new AtomicInteger(NUMBER_OF_THREADS);

		// we need a lock despite using an AtomicInteger, just to guarantee that
		// remaining workers are reported in order in the logs
		var lock = new Object();

		class Worker implements Runnable {
			private final int num;
			private boolean failed;

			private Worker(int num) {
				this.num = num;
			}

			@Override
			public void run() {
				try {
					var random = new Random();
					StorageReference ourAccount = account(num);

					while (true) {
						// we generate the number of another random distinct worker
						int other = random.ints(0, NUMBER_OF_THREADS).filter(i -> i != num).findFirst().getAsInt();
						StorageReference otherAccount = account(other);

						// we ask for the balance of the account bound to the this worker
						BigInteger ourBalance = runInstanceNonVoidMethodCallTransaction(ourAccount, _500_000, takamakaCode(), MethodSignatures.BALANCE, ourAccount)
							.asReturnedBigInteger(MethodSignatures.BALANCE, UnexpectedValueException::new);

						// we ask for the balance of the account bound to the other worker
						BigInteger otherBalance = runInstanceNonVoidMethodCallTransaction(ourAccount, _500_000, takamakaCode(), MethodSignatures.BALANCE, otherAccount)
							.asReturnedBigInteger(MethodSignatures.BALANCE, UnexpectedValueException::new);

						// if we are poorer than other, we send him only 5,000 units of coin; otherwise, we send him 10,000 units
						int sent = ourBalance.subtract(otherBalance).signum() < 0 ? 5_000 : 10_000;
						addInstanceVoidMethodCallTransaction(privateKey(num), ourAccount, _500_000, ONE, takamakaCode(),
							MethodSignatures.RECEIVE_INT, otherAccount, StorageValues.intOf(sent));
					}
				}
				catch (TransactionRejectedException e) {
					// eventually, the paying account "num" might not have enough gas to pay for a transaction
					if (!e.getMessage().startsWith("The payer has not enough funds to buy"))
						failure(e);
					else
						synchronized (lock) {
							LOGGER.info("Worker #" + num + " exits since it has not enough funds for buying gas: " + remaining.decrementAndGet() + " workers remaining");
						}
				}
				catch (TransactionException e) {
					// eventually, the paying account "num" might not have enough balance to pay the other account
					if (!e.getMessage().startsWith("io.takamaka.code.lang.InsufficientFundsError"))
						failure(e);
					else
						synchronized (lock) {
							LOGGER.info("Worker #" + num + " exits since it has not enough funds for paying: " + remaining.decrementAndGet() + " workers remaining");
						}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					failure(e);
				}
				catch (Exception e) {
					failure(e);
				}
			}

			private void failure(Exception exception) {
				failed = true;
				LOGGER.log(Level.SEVERE, "Unexpected exception", exception);
			}
		}

		// we create an array of THREAD_NUMBER workers
		var workers = IntStream.range(0, NUMBER_OF_THREADS).mapToObj(Worker::new).toArray(Worker[]::new);
		var customThreadPool = new ForkJoinPool(NUMBER_OF_THREADS);
		customThreadPool.submit(() -> IntStream.range(0, NUMBER_OF_THREADS).parallel().forEach(i -> workers[i].run())).get();
		customThreadPool.shutdown();

		// the workers are expected to throw no exceptions, or otherwise that is typically sign of a race condition
		assertTrue(Stream.of(workers).noneMatch(worker -> worker.failed));
	}
}