/**
 * 
 */
package io.takamaka.code.tests;

import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class Bombing extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final int TRANSFERS = 5000;
	private static final int ACCOUNTS = 16;
	private static final NonVoidMethodSignature GET_BALANCE = new NonVoidMethodSignature(Constants.TEOA_NAME, "getBalance", ClassType.BIG_INTEGER);

	@BeforeEach
	void beforeEach() throws Exception {
		// ACCOUNTS accounts
		setNode(_10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000, _10_000);
	}

	@Test @DisplayName(TRANSFERS + " random transfers between accounts")
	void randomTranfers() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		Random random = new Random();
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
	}
}