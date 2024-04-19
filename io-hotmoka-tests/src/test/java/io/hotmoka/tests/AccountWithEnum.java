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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for an externally owned account with an enum field.
 */
class AccountWithEnum extends HotmokaTest {

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("accountwithenum.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(BigInteger.valueOf(1_000_000L));
	}

	@Test @DisplayName("creates account, funds it and checks that its ordinal() == 0")
	void callOrdinal() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.getEncoder().encodeToString(signature().encodingOf(keys.getPublic()));

		StorageReference account = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(),
			ConstructorSignatures.of("io.hotmoka.examples.accountwithenum.AccountWithEnum", StorageTypes.STRING), StorageValues.stringOf(publicKey));

		addInstanceMethodCallTransaction(privateKey(0), account(0), _50_000, BigInteger.ONE, jar(),
			MethodSignatures.RECEIVE_INT, account, StorageValues.intOf(100_000));

		IntValue result = (IntValue) addInstanceMethodCallTransaction(keys.getPrivate(), account, _100_000, BigInteger.ONE, jar(),
			MethodSignatures.of("io.hotmoka.examples.accountwithenum.AccountWithEnum", "ordinal", StorageTypes.INT), account);

		assertEquals(0, result.getValue());
	}
}