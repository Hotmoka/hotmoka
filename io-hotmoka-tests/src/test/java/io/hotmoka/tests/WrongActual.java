/*
Copyright 2025 Fausto Spoto

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

import java.math.BigInteger;
import java.security.PrivateKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.UnmatchedTargetException;

/**
 * A test for wrong use of keys for signing a transaction.
 */
class WrongActual extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000, _1_000_000);
	}

	@Test @DisplayName("receive int with long actual fails")
	void receiveIntButActualIsLongFails() throws Exception {
		PrivateKey key = privateKey(0);
		StorageReference sender = account(0);
		StorageReference receiver = account(1);

		throwsTransactionExceptionWithCause(UnmatchedTargetException.class, () ->
			addInstanceVoidMethodCallTransaction(key, sender, _100_000, BigInteger.ONE, takamakaCode(), MethodSignatures.RECEIVE_INT, receiver, StorageValues.longOf(13L)));
	}

	@Test @DisplayName("method expected String but receives BigInteger and fails")
	void rotateReceivesStringButActualIsBigIntegerFails() throws Exception {
		PrivateKey key = privateKey(0);
		StorageReference eoa = account(0);

		throwsTransactionExceptionWithCause(UnmatchedTargetException.class, () ->
			addInstanceVoidMethodCallTransaction(key, eoa, _100_000, BigInteger.ONE, takamakaCode(), MethodSignatures.ROTATE_PUBLIC_KEY, eoa, StorageValues.bigIntegerOf(13L)));
	}
}