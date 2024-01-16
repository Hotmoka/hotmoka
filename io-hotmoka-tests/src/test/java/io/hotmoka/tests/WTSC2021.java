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
import java.security.PrivateKey;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.values.BigIntegerValue;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;

/**
 * A test for generating many coin transfers and count their speed.
 */
class WTSC2021 extends HotmokaTest {
	private final static String MY_ACCOUNTS = "io.hotmoka.examples.wtsc2021.MyAccounts";
	private final static int NUMBER_OF_TRANSFERS = 1000;
	private static int NUMBER_OF_ACCOUNTS = 500;
	private static ForkJoinPool customThreadPool;

	@BeforeAll
	static void beforeAll() {
		customThreadPool = new ForkJoinPool(NUMBER_OF_ACCOUNTS);
		String cheapTests = System.getProperty("cheapTests");
		if ("true".equals(cheapTests)) {
			System.out.println("Running in cheap mode since cheapTests = true");
			NUMBER_OF_ACCOUNTS = 4;
		}
	}

	@BeforeEach
	void beforeEach() throws Exception {
		long start = System.currentTimeMillis();
		setJar("wtsc2021.jar");
		transactions.getAndIncrement();
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
	private static long totalTime;

	private void run(int num) {
		StorageReference from = account(num);
		PrivateKey key = privateKey(num);
		var random = new Random();

		try {
			while (ticket.getAndIncrement() < NUMBER_OF_TRANSFERS) {
				StorageReference to = random.ints(0, NUMBER_OF_ACCOUNTS).filter(i -> i != num).mapToObj(this::account).findAny().get();
				int amount = 1 + random.nextInt(10);
				addInstanceMethodCallTransaction(key, from, _50_000, ZERO, takamakaCode(), MethodSignatures.RECEIVE_INT, to, StorageValues.intOf(amount));
				transfers.getAndIncrement();
				transactions.getAndIncrement();
			}
		}
		catch (TransactionException e) {
			// this occurs when a contract has no more money for running further transactions
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RepeatedTest(10)
	@DisplayName(NUMBER_OF_TRANSFERS + " random transfers between accounts")
	void randomTransfers(RepetitionInfo repetitionInfo) throws InterruptedException, TransactionException, CodeExecutionException, TransactionRejectedException, ExecutionException {
		long start = System.currentTimeMillis();

		customThreadPool.submit(() -> IntStream.range(0, NUMBER_OF_ACCOUNTS).parallel().forEach(this::run)).get();

		// we ask for the richest account
		StorageValue richest = runInstanceMethodCallTransaction(account(0), _1_000_000, jar(), MethodSignatures.of(StorageTypes.classNamed(MY_ACCOUNTS), "richest", StorageTypes.EOA), containerOfAccounts());

		totalTime += System.currentTimeMillis() - start;

		System.out.println("iteration " + repetitionInfo.getCurrentRepetition() + "/" + repetitionInfo.getTotalRepetitions() + " complete, the richest is " + richest);

		// we compute the sum of the balances of the accounts
		BigInteger sum = ZERO;
		for (int i = 0; i < NUMBER_OF_ACCOUNTS; i++)
			sum = sum.add(((BigIntegerValue) runInstanceMethodCallTransaction(account(0), _50_000, takamakaCode(), MethodSignatures.BALANCE, account(i))).getValue());

		// no money got lost in translation
		assertEquals(sum, BigInteger.valueOf(NUMBER_OF_ACCOUNTS).multiply(_50_000));
	}
}