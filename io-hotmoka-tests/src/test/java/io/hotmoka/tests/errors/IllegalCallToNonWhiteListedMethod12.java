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

import java.math.BigInteger;
import java.security.KeyPair;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.local.NonWhiteListedCallException;
import io.hotmoka.tests.HotmokaTest;

class IllegalCallToNonWhiteListedMethod12 extends HotmokaTest {

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(_1_000_000_000);
	}

	@Test @DisplayName("new ExternallyOwnedAccount().hashCode()")
	void testNonWhiteListedCall() throws Exception {
		KeyPair keys = signature().getKeyPair();
		String publicKey = Base64.toBase64String(signature().encodingOf(keys.getPublic()));
		StorageReference eoa = addConstructorCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, takamakaCode(), ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.STRING), StorageValues.stringOf(publicKey));

		throwsTransactionExceptionWithCause(NonWhiteListedCallException.class, () ->
			addInstanceNonVoidMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, takamakaCode(), MethodSignatures.ofNonVoid(StorageTypes.OBJECT, "hashCode", StorageTypes.INT), eoa)
		);
	}
}