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

package io.hotmoka.tests.errors;

import static java.math.BigInteger.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.beans.responses.TransactionResponse;
import io.hotmoka.beans.signatures.CodeSignature;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.nodes.Signer;
import io.hotmoka.tests.HotmokaTest;

/**
 * A test of a repeated transaction request. The second request fails.
 */
class Repeated extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("install jar")
	void installJar() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException {
		JarStoreTransactionRequest request = new JarStoreTransactionRequest(Signer.with(signature(), privateKey(0)), account(0), getNonceOf(account(0)), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		TransactionReference reference = node.addJarStoreTransaction(request);
		TransactionResponse response = node.getResponse(reference);

		assertTrue(response instanceof JarStoreTransactionSuccessfulResponse);
	}

	@Test @DisplayName("install jar twice")
	void installJarTwice() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException {
		JarStoreTransactionRequest request = new JarStoreTransactionRequest(Signer.with(signature(), privateKey(0)), account(0), getNonceOf(account(0)), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		node.addJarStoreTransaction(request);
		
		throwsTransactionRejectedException(() -> node.addJarStoreTransaction(request));
	}

	@Test @DisplayName("install jar twice concurrently")
	void installJarTwiceConcurrently() throws InvalidKeyException, SignatureException, TransactionRejectedException, IOException {
		JarStoreTransactionRequest request = new JarStoreTransactionRequest(Signer.with(signature(), privateKey(0)), account(0), getNonceOf(account(0)), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		node.postJarStoreTransaction(request);
		throwsTransactionRejectedException(() -> node.postJarStoreTransaction(request));
	}

	@Test @DisplayName("install jar twice, the first time fails, the second succeeds")
	void installJarFirstTimeFailsSecondTimeSucceeds() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, CodeExecutionException {
		BigInteger nonce = getNonceOf(account(0));
		Signer signer = Signer.with(signature(), privateKey(0));

		// the following request uses the wrong nonce, hence it will be rejected now
		// it will charge 20,000 units of coin to account(0), for penalty
		JarStoreTransactionRequest request = new JarStoreTransactionRequest(signer, account(0), nonce.add(ONE), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());

		try {
			node.addJarStoreTransaction(request);
			fail();
		}
		catch (TransactionRejectedException e) {
			// expected
		}

		// we run a transaction now, with the correct nonce, that increases the nonce of account(0)
		BigInteger balance = ((BigIntegerValue) node.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(signer, account(0), nonce, chainId, _100_000, ONE, takamakaCode(), CodeSignature.BALANCE, account(0)))).value;
		assertEquals(BigInteger.valueOf(999900000), balance);

		// we run the original request now, that will pass since the nonce is correct this time
		TransactionReference reference = node.addJarStoreTransaction(request);

		// getResponse() agrees
		TransactionResponse response = node.getResponse(reference);
		assertTrue(response instanceof JarStoreTransactionSuccessfulResponse);
	}
}