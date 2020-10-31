/**
 * 
 */
package io.takamaka.code.tests;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.hotmoka.takamaka.beans.requests.MintTransactionRequest;

/**
 * A test for minting and burning coins in the Takamaka blockchain.
 */
class MintAndBurn extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);
	private static final MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(ALL_FUNDS);
	}

	@Test @DisplayName("mint coins")
	void mintCoins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		// minting 200 units of coins for account #0
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;
			Signer signer = Signer.with(signature(), privateKey(0));

			BigIntegerValue initialBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(privateKey(0), account(0), _10_000, ZERO, takamakaCode(), GET_BALANCE, account(0));

			// mint 200 units of coin into account #0
			node.addMintTransaction(new MintTransactionRequest(signer, account(0), ZERO, chainId, _10_000,
				ZERO, takamakaCode(), BigInteger.valueOf(200L), ZERO));

			BigIntegerValue finalBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(privateKey(0), account(0), _10_000, ZERO, takamakaCode(), GET_BALANCE, account(0));

			assertEquals(finalBalance.value.subtract(initialBalance.value), BigInteger.valueOf(200L));
		}
	}

	@Test @DisplayName("burn coins")
	void burnCoins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;
			Signer signer = Signer.with(signature(), privateKey(0));

			BigIntegerValue initialBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(privateKey(0), account(0), _10_000, ZERO, takamakaCode(), GET_BALANCE, account(0));

			// burn 200 units of coin from account #0
			node.addMintTransaction(new MintTransactionRequest(signer, account(0), ZERO, chainId, _10_000,
				ZERO, takamakaCode(), BigInteger.valueOf(-200L), ZERO));

			BigIntegerValue finalBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(privateKey(0), account(0), _10_000, ZERO, takamakaCode(), GET_BALANCE, account(0));

			assertEquals(finalBalance.value.subtract(initialBalance.value), BigInteger.valueOf(-200L));
		}
	}

	@Test @DisplayName("burn coins but the account has not so much coins to burn")
	void burnCoinsNotEnough() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;
			Signer signer = Signer.with(signature(), privateKey(0));

			BigIntegerValue initialBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(privateKey(0), account(0), _10_000, ZERO, takamakaCode(), GET_BALANCE, account(0));

			// burn too many (one more than possible) units of coin from account #0
			try {
				node.addMintTransaction(new MintTransactionRequest(signer, account(0), ZERO, chainId, _10_000,
					ZERO, takamakaCode(), initialBalance.value.negate().subtract(ONE), ZERO));
			}
			catch (TransactionException e) {
				if (e.getMessage().startsWith(IllegalStateException.class.getName())
						&& e.getMessage().endsWith("not enough balance to burn 1000000001 green coins"))
					return;

				fail("wrong exception");
			}

			fail("expected exception");
		}
	}
}