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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.SignatureException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.nodes.api.Node.CodeSupplier;

/**
 * A test for the remote purchase contract.
 */
class RedGreenDistributor extends HotmokaTest {
	private static final ClassType DISTRIBUTOR = new ClassType("io.hotmoka.examples.redgreendistributor.Distributor");
	private static final VoidMethodSignature ADD_AS_PAYEE = new VoidMethodSignature(DISTRIBUTOR, "addAsPayee");
	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("redgreendistributor.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setGreenRedAccounts(
			BigInteger.valueOf(1_100_000L), BigInteger.valueOf(1_100_000L), // green then red of first account
			BigInteger.valueOf(100_000L), ZERO, // green then red of second account
			BigInteger.valueOf(100_000L), ZERO, // green then red of third account
			BigInteger.valueOf(90_000L), BigInteger.valueOf(90_000L), // green then red of fourth account
			ZERO, ZERO  // green then red of fifth account
		);
	}

	@Test @DisplayName("new RedGreenDistributor()")
	void createDistributor() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(DISTRIBUTOR));
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 green and their red balance is zero")
	void createDistributorAndTwoPayees() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		addInstanceMethodCallTransaction(
			privateKey(1), account(1),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceMethodCallTransaction(
			privateKey(2), account(2),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceMethodCallTransaction(
			privateKey(0), account(0),
			_20_000,
			ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "distributeGreen", ClassType.BIG_INTEGER),
			distributor, new BigIntegerValue(BigInteger.valueOf(1_000))
		);

		BigIntegerValue balanceRed1 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			new NonVoidMethodSignature(ClassType.EOA, "balanceRed", ClassType.BIG_INTEGER),
			account(1)
		);

		BigIntegerValue balanceRed2 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			new NonVoidMethodSignature(ClassType.EOA, "balanceRed", ClassType.BIG_INTEGER),
			account(2)
		);

		Assertions.assertEquals(ZERO, balanceRed1.value);
		Assertions.assertEquals(ZERO, balanceRed2.value);
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 red and their red balance is 500")
	void createDistributorAndTwoPayeesThenDistributes1000Red() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		addInstanceMethodCallTransaction(
			privateKey(1), account(1),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceMethodCallTransaction(
			privateKey(2), account(2),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceMethodCallTransaction(
			privateKey(0), account(0),
			_20_000,
			ONE,
			jar(),
			new VoidMethodSignature(DISTRIBUTOR, "distributeRed", ClassType.BIG_INTEGER),
			distributor, new BigIntegerValue(BigInteger.valueOf(1_000))
		);

		BigIntegerValue balanceRed1 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			new NonVoidMethodSignature(ClassType.EOA, "balanceRed", ClassType.BIG_INTEGER),
			account(1)
		);

		BigIntegerValue balanceRed2 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			new NonVoidMethodSignature(ClassType.EOA, "balanceRed", ClassType.BIG_INTEGER),
			account(2)
		);

		Assertions.assertEquals(500, balanceRed1.value.intValue());
		Assertions.assertEquals(500, balanceRed2.value.intValue());
	}

	@Test @DisplayName("distributeRed() cannot be called from an externally owned account that is not red/green")
	void distributeRedCannotBeCalledFromNonRedGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		KeyPair keys = signature().getKeyPair();
		CodeSupplier<StorageReference> eoa = postConstructorCallTransaction(
			privateKey(0), account(0),
			_20_000,
			ONE,
			jar(),
			new ConstructorSignature(ClassType.EOA, ClassType.BIG_INTEGER),
			new BigIntegerValue(_20_000)
		);

		addInstanceMethodCallTransaction(
			privateKey(1), account(1),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceMethodCallTransaction(
			privateKey(2), account(2),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		throwsTransactionException(() ->
			addInstanceMethodCallTransaction(
				keys.getPrivate(),
				eoa.get(),
				_20_000,
				ONE,
				jar(),
				new VoidMethodSignature(DISTRIBUTOR, "distributeRed", ClassType.BIG_INTEGER),
				distributor, new BigIntegerValue(BigInteger.valueOf(1_000))
			)
		);
	}

	@Test @DisplayName("new RedGreenDistributor() then fails while adding a payee without enough coins")
	void createDistributorThenFailsByAddingPayeeWithoutGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		throwsTransactionRejectedException(() ->
			addInstanceMethodCallTransaction(
				privateKey(3), account(3),
				_500_000,
				ONE,
				jar(),
				ADD_AS_PAYEE,
				distributor
			)
		);
	}

	@Test @DisplayName("new RedGreenDistributor() then adds a payee paying with both red and green")
	void createDistributorThenAddsPayeePayingWithRedAndGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), new ConstructorSignature(DISTRIBUTOR));

		addInstanceMethodCallTransaction(
			privateKey(3), account(3),
			BigInteger.valueOf(100_000), // more than 90,000, but it can use green after red are exhausted
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);
	}
}