/**
 * 
 */
package io.takamaka.code.tests.errors;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.requests.SignedTransactionRequest.Signer;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.takamaka.code.tests.TakamakaTest;

/**
 * A test of a repeated transaction request. The second request fails.
 */
class Repeated extends TakamakaTest {
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);
	private static final BigInteger _1_000_000_000 = BigInteger.valueOf(1_000_000_000);

	@BeforeEach
	void beforeEach() throws Exception {
		setNode(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, NoSuchAlgorithmException {
		JarStoreTransactionRequest request = new JarStoreTransactionRequest(Signer.with(signature(), privateKey(0)), account(0), getNonceOf(account(0), privateKey(0)), chainId, _20_000, BigInteger.ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		TransactionReference reference = originalView.addJarStoreTransaction(request);
		TransactionResponse response = originalView.getResponse(reference);

		assertTrue(response instanceof JarStoreTransactionSuccessfulResponse);
	}

	@Test @DisplayName("install jar twice")
	void installJarTwice() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, NoSuchAlgorithmException {
		JarStoreTransactionRequest request = new JarStoreTransactionRequest(Signer.with(signature(), privateKey(0)), account(0), getNonceOf(account(0), privateKey(0)), chainId, _20_000, BigInteger.ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		originalView.addJarStoreTransaction(request);
		
		throwsTransactionRejectedException(() -> originalView.addJarStoreTransaction(request));
	}

	@Test @DisplayName("install jar twice concurrently")
	void installJarTwiceConcurrently() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, NoSuchAlgorithmException {
		JarStoreTransactionRequest request = new JarStoreTransactionRequest(Signer.with(signature(), privateKey(0)), account(0), getNonceOf(account(0), privateKey(0)), chainId, _20_000, BigInteger.ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		originalView.postJarStoreTransaction(request);
		throwsTransactionRejectedException(() -> originalView.postJarStoreTransaction(request));
	}
}