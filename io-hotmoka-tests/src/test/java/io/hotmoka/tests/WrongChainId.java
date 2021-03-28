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
 * A test for the wrong use of the chain identifier in a transaction.
 */
class WrongChainId extends TakamakaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_50_000);
	}

	@Test @DisplayName("constructor call with wrong chain identifier fails")
	void createAbstractFailImpl() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NoSuchAlgorithmException {
		SignatureAlgorithm<SignedTransactionRequest> signature = node.getSignatureAlgorithmForRequests();

		PrivateKey key = privateKey(0);
		StorageReference caller = account(0);

		throwsTransactionRejectedWithCause("incorrect chain id", () ->
			node.addConstructorCallTransaction(new ConstructorCallTransactionRequest(Signer.with(signature, key), caller, BigInteger.ZERO, chainId + "noise",
				_100_000, panarea(1), takamakaCode(), CodeSignature.EOA_CONSTRUCTOR, new BigIntegerValue(_50_000), new StringValue("ciao")))
		);
	}
}