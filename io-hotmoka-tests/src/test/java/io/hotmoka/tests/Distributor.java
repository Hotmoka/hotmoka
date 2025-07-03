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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.security.KeyPair;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.crypto.Base64;
import io.hotmoka.crypto.SignatureAlgorithms;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.UnexpectedValueException;
import io.hotmoka.node.api.IllegalAssignmentToFieldInStorageException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.transactions.TransactionReference;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the distribution of coins.
 */
class Distributor extends HotmokaTest {
	private static final ClassType DISTRIBUTOR = StorageTypes.classNamed("io.hotmoka.examples.distributor.Distributor");
	private static final VoidMethodSignature ADD_AS_PAYEE = MethodSignatures.ofVoid(DISTRIBUTOR, "addAsPayee");
	private static final VoidMethodSignature DISTRIBUTE = MethodSignatures.ofVoid(DISTRIBUTOR, "distribute", StorageTypes.BIG_INTEGER);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("distributor.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(
			BigInteger.valueOf(1_100_000L), // balance of first account
			BigInteger.valueOf(100_000L), // balance of second account
			BigInteger.valueOf(100_000L), // balance of third account
			BigInteger.valueOf(90_000L), // balance of fourth account
			ZERO  // balance of fifth account
		);
	}

	@Test @DisplayName("new Distributor()")
	void createDistributor() throws Exception {
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));
	}

	@Test @DisplayName("new Distributor() then fails while adding a payee without enough coins")
	void createDistributorThenFailsByAddingPayeeWithoutCoins() throws Exception {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		assertThrows(TransactionRejectedException.class, () ->
			addInstanceVoidMethodCallTransaction(
				privateKey(3), account(3),
				_500_000,
				ONE,
				jar(),
				ADD_AS_PAYEE,
				distributor
			)
		);
	}

	@Test @DisplayName("new Distributor() then adds three payees than first payee distributes")
	void createDistributorAddsFourPayeesThenFirstPayeeDistributes() throws Exception {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		final int numPayees = 3;

		for  (int payee = 1; payee <= numPayees; payee++)
			addInstanceVoidMethodCallTransaction(privateKey(payee), account(payee), _50_000, ONE, jar(), ADD_AS_PAYEE, distributor);

		var initialBalances = new BigInteger[numPayees];
		for  (int payee = 1; payee <= numPayees; payee++)
			initialBalances[payee - 1] = runInstanceNonVoidMethodCallTransaction(account(payee), _50_000, jar(), MethodSignatures.BALANCE, account(payee)).asReturnedBigInteger(MethodSignatures.BALANCE, UnexpectedValueException::new);

		// the first account distributes 100 to the three payees
		long distributed = 100L;
		addInstanceVoidMethodCallTransaction(privateKey(0), account(0), _50_000, ONE, jar(), DISTRIBUTE, distributor, StorageValues.bigIntegerOf(distributed));

		// each payee gets distributed / numPayees coins
		var finalBalances = new BigInteger[numPayees];
		for  (int payee = 1; payee <= numPayees; payee++)
			finalBalances[payee - 1] = runInstanceNonVoidMethodCallTransaction(account(payee), _50_000, jar(), MethodSignatures.BALANCE, account(payee)).asReturnedBigInteger(MethodSignatures.BALANCE, UnexpectedValueException::new);

		BigInteger difference = BigInteger.valueOf(distributed / numPayees);
		for (int payee = 1; payee <= numPayees; payee++)
			assertEquals(finalBalances[payee - 1], initialBalances[payee - 1].add(difference));
	}

	@Test @DisplayName("new Distributor() then add a special payee with its own classpath unrelated to that of the distributor and distributes")
	void createDistributorAddsSpecialPayeeWithOwnClasspathThenDistributes() throws Exception {
		// we create the distributor with classpath distributor.jar and then takamakaCode.jar
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		// we install specialaccount.jar with dependency distributor.jar and then takamakaCode.jar
		TransactionReference classpathOfSpecialAccount = addJarStoreTransaction(privateKey(0), account(0), _100_000, ONE, takamakaCode(), bytesOf("specialaccount.jar"), jar());

		// we create a special account with classpath specialaccount.jar then distributor.jar and then takamakaCode.jar
		var signature = SignatureAlgorithms.ed25519();
		KeyPair keysOfSpecialAccount = signature.getKeyPair();
		var specialAccountClass = StorageTypes.classNamed("io.hotmoka.examples.specialaccount.SpecialAccount");
		StorageReference specialAccount = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ZERO, classpathOfSpecialAccount,
				ConstructorSignatures.of(specialAccountClass, StorageTypes.INT, StorageTypes.STRING),
				StorageValues.intOf(1_000_000),
				StorageValues.stringOf(Base64.toBase64String(signature.encodingOf(keysOfSpecialAccount.getPublic()))));

		// we add the special account as a payee of the distributor: this will fail since the classpath of the creation transaction
		// of the special account is not reachable from the classpath of the creation transaction of the distributor
		throwsTransactionExceptionWithCause(IllegalAssignmentToFieldInStorageException.class, () -> addInstanceVoidMethodCallTransaction(keysOfSpecialAccount.getPrivate(), specialAccount, _50_000, ONE, classpathOfSpecialAccount, ADD_AS_PAYEE, distributor));
	}
}