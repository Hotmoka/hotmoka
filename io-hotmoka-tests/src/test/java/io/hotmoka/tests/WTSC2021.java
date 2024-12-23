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

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;

/**
 * A test for generating many coin transfers and count their speed.
 */
class WTSC2021 extends HotmokaTest {
	private final static String MY_ACCOUNTS = "io.hotmoka.examples.wtsc2021.MyAccounts";
	private final static int NUMBER_OF_TRANSFERS = 1000;
	private static int NUMBER_OF_ACCOUNTS = 500;
	private static ForkJoinPool customThreadPool;
	private final static Logger LOGGER = Logger.getLogger(WTSC2021.class.getName());

	@BeforeAll
	static void beforeAll() throws Exception {
		customThreadPool = new ForkJoinPool(NUMBER_OF_ACCOUNTS);
		String cheapTests = System.getProperty("cheapTests");
		if ("true".equals(cheapTests)) {
			System.out.println("Running in cheap mode since cheapTests = true");
			NUMBER_OF_ACCOUNTS = 4;
		}

		setJar("wtsc2021.jar");
		transactions.getAndIncrement();
	}

	@BeforeEach
	void beforeEach() throws Exception {
		long start = System.currentTimeMillis();
		setAccounts(MY_ACCOUNTS, jar(), Stream.generate(() -> _50_000).limit(NUMBER_OF_ACCOUNTS)); // NUMBER_OF_ACCOUNTS accounts
		transactions.getAndIncrement();
		totalTime += System.currentTimeMillis() - start;
	}

	@AfterAll
	static void afterAll() {
		customThreadPool.shutdownNow();
		System.out.printf("%d money transfers, %d transactions in %d ms [%d tx/s]\n", transfers.get(), transactions.get(), totalTime, transactions.get() * 1000L / totalTime);
	}

	private final AtomicInteger ticket = new AtomicInteger();
	private final static AtomicInteger transfers = new AtomicInteger();
	private final static AtomicInteger transactions = new AtomicInteger();
	private final static AtomicInteger failed = new AtomicInteger();
	private static long totalTime;

	@RepeatedTest(10)
	@DisplayName(NUMBER_OF_TRANSFERS + " random transfers between accounts")
	void randomTransfers(RepetitionInfo repetitionInfo) throws InterruptedException, TransactionException, CodeExecutionException, TransactionRejectedException, ExecutionException, NodeException, TimeoutException {

		var remaining = new AtomicInteger(NUMBER_OF_ACCOUNTS);

		// we need a lock despite using an AtomicInteger, just to guarantee that
		// remaining workers are reported in order in the logs
		var lock = new Object();

		var accounts = accounts().toArray(StorageReference[]::new);

		class Worker implements Runnable {
			private final int num;

			private Worker(int num) {
				this.num = num;
			}

			@Override
			public void run() {
				try {
					StorageReference from = account(num);
					PrivateKey key = privateKey(num);
					var random = new Random();

					while (ticket.getAndIncrement() < NUMBER_OF_TRANSFERS) {
						StorageReference to = random.ints(0, NUMBER_OF_ACCOUNTS).filter(i -> i != num).mapToObj(i -> accounts[i]).findAny().get();
						int amount = 1 + random.nextInt(10);
						addInstanceVoidMethodCallTransaction(key, from, _50_000, ZERO, takamakaCode(), MethodSignatures.RECEIVE_INT, to, StorageValues.intOf(amount));
						transfers.getAndIncrement();
						transactions.getAndIncrement();
					}
				}
				catch (TimeoutException e) {
					// this occurs if the node is remote and very slow, so that the connection timeouts
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
							LOGGER.info("Worker #" + num + " exits since it has not enough funds anymore: " + remaining.decrementAndGet() + " workers remaining");
						}
				}
				catch (InvalidKeyException | SignatureException | CodeExecutionException | NodeException e) {
					failure(e);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					failure(e);
				}
			}

			private void failure(Exception exception) {
				int howManyFailedAlready = failed.incrementAndGet();
				LOGGER.warning("Worker #" + num + " failed, which is normal for nodes with probabilistic finality (" + (NUMBER_OF_ACCOUNTS - howManyFailedAlready) + " remaining workers): " + exception.getMessage());
			}
		}

		long start = System.currentTimeMillis();

		customThreadPool.submit(() -> IntStream.range(0, NUMBER_OF_ACCOUNTS).parallel().mapToObj(Worker::new).forEach(Runnable::run)).get();

		// we ask for the richest account
		StorageValue richest = runInstanceNonVoidMethodCallTransaction(account(0), _1_000_000, jar(), MethodSignatures.ofNonVoid(StorageTypes.classNamed(MY_ACCOUNTS), "richest", StorageTypes.EOA), containerOfAccounts());

		totalTime += System.currentTimeMillis() - start;

		System.out.println("iteration " + repetitionInfo.getCurrentRepetition() + "/" + repetitionInfo.getTotalRepetitions() + " complete, the richest is " + richest);

		// we compute the sum of the balances of the accounts
		BigInteger sum = ZERO;
		for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++)
			sum = sum.add(runInstanceNonVoidMethodCallTransaction(account(0), _50_000, takamakaCode(), MethodSignatures.BALANCE, account(i)).asBigInteger(value -> new NodeException()));

		// no money got lost in translation
		assertEquals(sum, BigInteger.valueOf(NUMBER_OF_ACCOUNTS).multiply(_50_000));
	}
}