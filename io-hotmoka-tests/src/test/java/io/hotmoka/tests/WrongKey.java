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

package io.hotmoka.tests;

import static io.hotmoka.helpers.Coin.panarea;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionRequests;
import io.hotmoka.beans.api.requests.SignedTransactionRequest;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.node.api.NodeException;

/**
 * A test for wrong use of keys for signing a transaction.
 */
class WrongKey extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_100_000, _100_000);
	}

	@Test @DisplayName("constructor call with wrong key fails")
	void createAbstractFailImpl() throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		// the empty signature algorithm cannot fail
		if (consensus != null && "empty".equals(consensus.getSignatureForRequests().getName()))
			return;

		// key 1 for account 0 !
		PrivateKey key = privateKey(1);
		StorageReference caller = account(0);

		throwsTransactionRejectedWithCause("invalid request signature", () ->
			node.addConstructorCallTransaction(TransactionRequests.constructorCall(signature().getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, BigInteger.ZERO, chainId,
				_100_000, panarea(1), takamakaCode(), ConstructorSignatures.EOA_CONSTRUCTOR, StorageValues.bigIntegerOf(_50_000), StorageValues.stringOf("ciao")))
		);
	}
}