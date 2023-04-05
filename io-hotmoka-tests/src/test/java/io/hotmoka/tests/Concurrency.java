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
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;

/**
 * A test for generating transactions in concurrency and check that everything
 * works without race conditions in the node. It uses a pool of threads to send
 * requests concurrently, including run requests, which is important since the
 * latter are executed in real parallelism. A race condition would generate
 * an exception, which makes the test fail.
 */
class Concurrency extends HotmokaTest {

	/**
	 * The number of threads that operate concurrently. At least 2 or this test will hang!
	 */
	private static int NUMBER_OF_THREADS = 100;

	@BeforeAll
	static void beforeAll() {
		String cheapTests = System.getProperty("cheapTests");
		if ("true".equals(cheapTests)) {
			System.out.println("Running in cheap mode since cheapTests = true");
			NUMBER_OF_THREADS = 4;
		}
	}

	@BeforeEach
	void beforeEach() throws Exception {
		// generate NUMBER_OF_THREADS externally-owned accounts with a balance of a hundred thousand each
		setAccounts(Stream.generate(() -> _500_000).limit(NUMBER_OF_THREADS));
	}

	private class Worker implements Runnable {
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
					int other = random.ints(0, NUMBER_OF_THREADS).filter(i -> i != num).findFirst().getAsInt();

					// we ask for the balance of the account bound to the this worker
					BigInteger ourBalance = ((BigIntegerValue) runInstanceMethodCallTransaction
						(account(num), _50_000, takamakaCode(), CodeSignature.BALANCE, account(num))).value;

					// we ask for the balance of the account bound to the other worker
					BigInteger otherBalance = ((BigIntegerValue) runInstanceMethodCallTransaction
						(account(num), _50_000, takamakaCode(), CodeSignature.BALANCE, account(other))).value;

					// if we are poorer than other, we send him only 5,000 units of coin; otherwise, we send him 10,000 units
					int sent = ourBalance.subtract(otherBalance).signum() < 0 ? 5_000 : 10_000;
					addInstanceMethodCallTransaction(privateKey(num), account(num), _50_000, ONE, takamakaCode(),
						CodeSignature.RECEIVE_INT, account(other), new IntValue(sent));
				}
			}
			catch (TransactionRejectedException e) {
				// eventually, the paying account "num" might have not enough gas to pay for a transaction
				if (e.getMessage().startsWith("the payer has not enough funds to buy 50000 units of gas")) {
					return;
				}
				else {
					failed = true;
					throw new RuntimeException(e);
				}
			}
			catch (TransactionException e) {
				// eventually, the paying account "num" might have not enough balance to pay the other account
				if (e.getMessage().startsWith("io.takamaka.code.lang.InsufficientFundsError")) {
					return;
				}
				else {
					failed = true;
					throw new RuntimeException(e);
				}
			}
			catch (InvalidKeyException | SignatureException | CodeExecutionException e) {
				failed = true;
				throw new RuntimeException(e);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test @DisplayName("More threads generate transactions concurrently")
	void concurrently() throws InterruptedException, ExecutionException {
		// we create an array of THREAD_NUMBER workers
		Worker[] workers = IntStream.range(0, NUMBER_OF_THREADS).mapToObj(Worker::new).toArray(Worker[]::new);

		ExecutorService customThreadPool = new ForkJoinPool(NUMBER_OF_THREADS);
		customThreadPool.submit(() -> IntStream.range(0, NUMBER_OF_THREADS).parallel().forEach(i -> workers[i].run())).get();
		customThreadPool.shutdownNow();

		// the workers are expected to throw no exceptions, or otherwise that is typically sign of a race condition
		assertTrue(Stream.of(workers).noneMatch(worker -> worker.failed));
	}
}