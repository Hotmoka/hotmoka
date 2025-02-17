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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.node.TransactionRequests;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.remote.api.RemoteNode;

/**
 * A test for creating an account for free in the Takamaka blockchain.
 */
class CreateAccountForFree extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("create account")
	void createAccount() throws Exception {
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));

		if (!(node instanceof RemoteNode)) {
			// all other nodes are expected to reject this, since the node is already initialized
			TransactionRejectedException e = assertThrows(TransactionRejectedException.class, () -> node.addGameteCreationTransaction(TransactionRequests.gameteCreation(takamakaCode(), _50_000, publicKey)));
			assertTrue(e.getMessage().contains("Cannot run an initial transaction request in an already initialized node"));
		}
	}
}