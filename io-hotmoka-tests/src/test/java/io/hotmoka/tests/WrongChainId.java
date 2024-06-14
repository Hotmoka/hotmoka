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

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.requests.SignedTransactionRequest;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the wrong use of the chain identifier in a transaction.
 */
class WrongChainId extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_50_000);
	}

	@Test @DisplayName("constructor call with wrong chain identifier fails")
	void createAbstractFailImpl() throws NoSuchElementException, NodeException, TimeoutException, InterruptedException {
		PrivateKey key = privateKey(0);
		StorageReference caller = account(0);

		throwsTransactionRejectedWithCause("Incorrect chain id", () ->
			node.addConstructorCallTransaction(TransactionRequests.constructorCall(signature().getSigner(key, SignedTransactionRequest::toByteArrayWithoutSignature), caller, BigInteger.ZERO, chainId() + "noise",
				_100_000, panarea(1), takamakaCode(), ConstructorSignatures.EOA_CONSTRUCTOR, StorageValues.bigIntegerOf(_50_000), StorageValues.stringOf("ciao")))
		);
	}
}