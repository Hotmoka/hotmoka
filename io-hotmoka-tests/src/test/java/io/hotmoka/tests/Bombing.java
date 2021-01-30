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
import io.hotmoka.nodes.Node.CodeSupplier;

/**
 * A test for generating many coin transfers and count their speed.
 */
class Bombing extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final int TRANSFERS = 100;
	private static final int ACCOUNTS = 16;

	@BeforeEach
	void beforeEach() throws Exception {
		// ACCOUNTS accounts
		BigInteger[] funds = Stream.generate(() -> _10_000).limit(ACCOUNTS).toArray(BigInteger[]::new);
		setNode(funds);
	}

	@Test @DisplayName(TRANSFERS + " random transfers between accounts")
	void randomTranfers() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		Random random = new Random();
		long start = System.currentTimeMillis();

		CodeSupplier<?>[] futures = new CodeSupplier<?>[ACCOUNTS];
		int transfers = 0;
		while (transfers < TRANSFERS) {
			for (int num = 0; num < ACCOUNTS && transfers < TRANSFERS; num++, transfers++) {
				StorageReference from = account(num);
				PrivateKey key = privateKey(num);

				StorageReference to;
				do {
					to = account(random.nextInt(ACCOUNTS));
				}
				while (to == from); // we want a different account than from

				int amount = 1 + random.nextInt(10);
				futures[num] = postInstanceMethodCallTransaction(key, from, _10_000, ZERO, takamakaCode(), CodeSignature.RECEIVE_INT, to, new IntValue(amount));
			}

			// we wait until the last group is committed
			for (CodeSupplier<?> future: futures)
				future.get();
		}

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