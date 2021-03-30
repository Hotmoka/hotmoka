/**
 * 
 */
package io.hotmoka.tests;

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
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.takamaka.beans.requests.MintTransactionRequest;

/**
 * A test for minting and burning coins in the Takamaka blockchain.
 */
class MintAndBurn extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("mint coins")
	void mintCoins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		// minting 200 units of coins for account #0
		if (takamakaBlockchain != null) {
			Signer signer = Signer.with(signature(), privateKey(0));

			BigIntegerValue initialBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(account(0), _50_000, takamakaCode(), CodeSignature.BALANCE, account(0));

			// mint 200 units of coin into account #0
			takamakaBlockchain.addMintTransaction(new MintTransactionRequest(signer, account(0), ZERO, chainId, _50_000,
				ZERO, takamakaCode(), BigInteger.valueOf(200L), ZERO));

			BigIntegerValue finalBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(account(0), _50_000, takamakaCode(), CodeSignature.BALANCE, account(0));

			assertEquals(finalBalance.value.subtract(initialBalance.value), BigInteger.valueOf(200L));
		}
	}

	@Test @DisplayName("burn coins")
	void burnCoins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (takamakaBlockchain != null) {
			Signer signer = Signer.with(signature(), privateKey(0));

			BigIntegerValue initialBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(account(0), _50_000, takamakaCode(), CodeSignature.BALANCE, account(0));

			// burn 200 units of coin from account #0
			takamakaBlockchain.addMintTransaction(new MintTransactionRequest(signer, account(0), ZERO, chainId, _50_000,
				ZERO, takamakaCode(), BigInteger.valueOf(-200L), ZERO));

			BigIntegerValue finalBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(account(0), _50_000, takamakaCode(), CodeSignature.BALANCE, account(0));

			assertEquals(finalBalance.value.subtract(initialBalance.value), BigInteger.valueOf(-200L));
		}
	}

	@Test @DisplayName("burn coins but the account has not so much coins to burn")
	void burnCoinsNotEnough() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (takamakaBlockchain != null) {
			Signer signer = Signer.with(signature(), privateKey(0));

			BigIntegerValue initialBalance = (BigIntegerValue) runInstanceMethodCallTransaction
				(account(0), _50_000, takamakaCode(), CodeSignature.BALANCE, account(0));

			// burn too many (one more than possible) units of coin from account #0
			try {
				takamakaBlockchain.addMintTransaction(new MintTransactionRequest(signer, account(0), ZERO, chainId, _50_000,
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