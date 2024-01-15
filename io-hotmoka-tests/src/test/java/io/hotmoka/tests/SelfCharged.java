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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.InvalidKeyException;
import java.security.SignatureException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.api.types.ClassType;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.signatures.VoidMethodSignature;

/**
 * A test of the @SelfCharged annotation.
 */
class SelfCharged extends HotmokaTest {
	private final static ClassType SELF_CHARGEABLE = StorageTypes.classNamed("io.hotmoka.examples.selfcharged.SelfChargeable");

	@BeforeAll
	static void beforeAll() throws Exception {
		if (consensus != null && consensus.allowsSelfCharged())
			setJar("selfcharged.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		if (consensus != null && consensus.allowsSelfCharged())
			setAccounts(_1_000_000, ZERO);
	}

	@Test @DisplayName("new C(100_000).foo() fails when called by an account with zero balance")
	void failsForNonSelfCharged() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus != null && consensus.allowsSelfCharged()) {
			StorageReference sc = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), ConstructorSignatures.of(SELF_CHARGEABLE, StorageTypes.INT), StorageValues.intOf(100_000));
			try {
				addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), new VoidMethodSignature(SELF_CHARGEABLE, "foo"), sc);
			}
			catch (TransactionRejectedException e) {
				assertEquals("the payer has not enough funds to buy 10000 units of gas", e.getMessage());
				return;
			}

			fail();
		}
	}

	@Test @DisplayName("new C(100_000).goo() succeeds when called by an account with zero balance")
	void succeedsForSelfCharged() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		if (consensus != null && consensus.allowsSelfCharged()) {
			StorageReference sc = addConstructorCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), ConstructorSignatures.of(SELF_CHARGEABLE, StorageTypes.INT), StorageValues.intOf(100_000));
			addInstanceMethodCallTransaction(privateKey(1), account(1), _50_000, ONE, jar(), new VoidMethodSignature(SELF_CHARGEABLE, "goo"), sc);
		}
	}
}