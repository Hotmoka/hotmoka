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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.api.Signer;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.responses.JarStoreTransactionSuccessfulResponse;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.values.BigIntegerValue;
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
	void installJar() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, NoSuchElementException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		var request = TransactionRequests.jarStore(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0), getNonceOf(account(0)), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		TransactionReference reference = node.addJarStoreTransaction(request);
		assertTrue(node.getResponse(reference) instanceof JarStoreTransactionSuccessfulResponse);
	}

	@Test @DisplayName("install jar twice")
	void installJarTwice() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		var request = TransactionRequests.jarStore(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0), getNonceOf(account(0)), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		node.addJarStoreTransaction(request);
		assertThrows(TransactionRejectedException.class, () -> node.addJarStoreTransaction(request));
	}

	@Test @DisplayName("install jar twice concurrently")
	void installJarTwiceConcurrently() throws InvalidKeyException, SignatureException, TransactionRejectedException, IOException, NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		var request = TransactionRequests.jarStore(signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature), account(0), getNonceOf(account(0)), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		node.postJarStoreTransaction(request);
		assertThrows(TransactionRejectedException.class, () -> node.postJarStoreTransaction(request));
	}

	@Test @DisplayName("install jar twice, the first time fails, the second succeeds")
	void installJarFirstTimeFailsSecondTimeSucceeds() throws InvalidKeyException, SignatureException, TransactionException, TransactionRejectedException, IOException, CodeExecutionException, NoSuchElementException, NodeException, TimeoutException, InterruptedException, UnknownReferenceException {
		BigInteger nonce = getNonceOf(account(0));
		Signer<SignedTransactionRequest<?>> signer = signature().getSigner(privateKey(0), SignedTransactionRequest::toByteArrayWithoutSignature);

		// the following request uses the wrong nonce, hence it will be rejected now
		// it will charge 20,000 units of coin to account(0), for penalty
		var request = TransactionRequests.jarStore(signer, account(0), nonce.add(ONE), chainId, _500_000, ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());

		assertThrows(TransactionRejectedException.class, () -> node.addJarStoreTransaction(request));

		// we run a transaction now, with the correct nonce, that increases the nonce of account(0)
		BigInteger balance = ((BigIntegerValue) node.addInstanceMethodCallTransaction(TransactionRequests.instanceMethodCall(signer, account(0), nonce, chainId, _100_000, ONE, takamakaCode(), MethodSignatures.BALANCE, account(0)))
			.orElseThrow(() -> new NodeException(MethodSignatures.BALANCE + " should not return void"))).getValue();
		assertEquals(BigInteger.valueOf(999900000), balance);

		// we perform a similar request now, that will pass since the nonce is correct this time: we need to change for instance the gas limit
		// otherwise this second request might be rejected (for instance by Tendermint), being considered as a repeated transaction
		var request2 = TransactionRequests.jarStore(signer, account(0), nonce.add(ONE), chainId, _500_000.add(ONE), ONE, takamakaCode(), bytesOf("calleronthis.jar"), takamakaCode());
		TransactionReference reference = node.addJarStoreTransaction(request2);

		// getResponse() agrees
		assertTrue(node.getResponse(reference) instanceof JarStoreTransactionSuccessfulResponse);
	}
}