/**
 * 
 */
package io.hotmoka.tests;

import static io.hotmoka.beans.Coin.panarea;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.crypto.SignatureAlgorithm;

/**
 * A test for wrong use of keys for signing a transaction.
 */
class WrongKey extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_100_000, _100_000);
	}

	@Test @DisplayName("constructor call with wrong key fails")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		// the empty signature algorithm cannot fail
		if (consensus != null && "empty".equals(consensus.getSignature().getName()))
			return;

		SignatureAlgorithm<SignedTransactionRequest> signature = node.getSignatureAlgorithmForRequests();

		// key 1 for account 0 !
		PrivateKey key = privateKey(1);
		StorageReference caller = account(0);

		throwsTransactionRejectedWithCause("invalid request signature", () ->
			node.addConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, BigInteger.ZERO, chainId,
				_100_000, panarea(1), takamakaCode(), CodeSignature.EOA_CONSTRUCTOR, new BigIntegerValue(_10_000), new StringValue("ciao")))
		);
	}
}