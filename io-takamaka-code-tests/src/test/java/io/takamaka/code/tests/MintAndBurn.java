/**
 * 
 */
package io.takamaka.code.tests;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.ONE;

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
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.hotmoka.takamaka.beans.requests.MintTransactionRequest;

/**
 * A test for minting and burning coins in the Takamaka blockchain.
 */
class MintAndBurn extends TakamakaTest {
	private static final BigInteger _1_000 = BigInteger.valueOf(1_000);
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(ALL_FUNDS, ZERO);
	}

	@Test @DisplayName("mint coins")
	void mintCoins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		// minting 200 units of coins for account #1
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;
			Signer signer = Signer.with(signature(), privateKey(1));
			node.addMintTransaction(new MintTransactionRequest(signer, account(1), ZERO, chainId, _1_000,
				ONE, takamakaCode(), BigInteger.valueOf(200), ZERO));
		}
	}

	@Test @DisplayName("burn coins")
	void burnCoins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;
			Signer signer = Signer.with(signature(), privateKey(1));

			// first we transfer 300 units of coin to account #1
			addTransferTransaction(privateKey(0), account(0), _1_000, takamakaCode(), account(1), 300);

			// then we burn 200 units of coin from account #1
			node.addMintTransaction(new MintTransactionRequest(signer, account(1), ZERO, chainId, _1_000,
				ONE, takamakaCode(), BigInteger.valueOf(-200), ZERO));
		}
	}

	@Test @DisplayName("burn coins but the account has not so much coins to burn")
	void burnCoinsNotEnough() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;

			// first we transfer 180 units of coin to account #1
			addTransferTransaction(privateKey(0), account(0), _1_000, takamakaCode(), account(1), 180);

			// then we burn 200 units of coin from account #1
			node.addMintTransaction(new MintTransactionRequest(Signer.with(signature(), privateKey(1)), account(1), ZERO, chainId, _1_000,
				ONE, takamakaCode(), BigInteger.valueOf(-200), ZERO));
		}
	}
}