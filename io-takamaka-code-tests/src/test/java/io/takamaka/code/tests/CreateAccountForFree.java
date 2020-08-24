/**
 * 
 */
package io.takamaka.code.tests;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.NonInitialTransactionRequest.Signer;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.takamaka.TakamakaBlockchain;
import io.hotmoka.takamaka.beans.requests.RedGreenAccountCreationTransactionRequest;

/**
 * A test for creating an account for free in the Takamaka blockchain.
 */
class CreateAccountForFree extends TakamakaTest {
	private static final BigInteger _10_000 = BigInteger.valueOf(10_000);
	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(ALL_FUNDS);
	}

	@Test @DisplayName("create account")
	void createAccount() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

			StorageReference newAccount = node.addAccountCreationTransaction(new RedGreenAccountCreationTransactionRequest
				(takamakaCode(), chainId, _10_000, _10_000, publicKey));

			assertNotNull(newAccount);
		}
	}

	@Test @DisplayName("create account and use it to create another account")
	void mintCoins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		if (originalView instanceof TakamakaBlockchain) {
			TakamakaBlockchain node = (TakamakaBlockchain) originalView;
			KeyPair keys = signature().getKeyPair();
			String publicKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

			StorageReference newAccount = node.addAccountCreationTransaction(new RedGreenAccountCreationTransactionRequest
				(takamakaCode(), chainId, _10_000, _10_000, publicKey));

			// the second account has the same public key as the new account: not really clever
			StorageReference secondAccount = node.addConstructorCallTransaction(new ConstructorCallTransactionRequest
				(Signer.with(signature(), keys), newAccount, BigInteger.ZERO, publicKey,
				_10_000, BigInteger.ONE, takamakaCode(),
				new ConstructorSignature(ClassType.TRGEOA, ClassType.BIG_INTEGER, ClassType.STRING),
				new BigIntegerValue(BigInteger.valueOf(100L))));

			assertNotNull(secondAccount);
			assertNotEquals(newAccount, secondAccount);
		}
	}
}