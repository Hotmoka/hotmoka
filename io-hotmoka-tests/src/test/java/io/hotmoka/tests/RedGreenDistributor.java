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
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.NodeException;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.BigIntegerValue;
import io.hotmoka.node.api.values.StorageReference;

/**
 * A test for the remote purchase contract.
 */
class RedGreenDistributor extends HotmokaTest {
	private static final ClassType DISTRIBUTOR = StorageTypes.classNamed("io.hotmoka.examples.redgreendistributor.Distributor");
	private static final VoidMethodSignature ADD_AS_PAYEE = MethodSignatures.ofVoid(DISTRIBUTOR, "addAsPayee");
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
	void createDistributor() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 green and their red balance is zero")
	void createDistributorAndTwoPayees() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		addInstanceVoidMethodCallTransaction(
			privateKey(1), account(1),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceVoidMethodCallTransaction(
			privateKey(2), account(2),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceVoidMethodCallTransaction(
			privateKey(0), account(0),
			_20_000,
			ONE,
			jar(),
			MethodSignatures.ofVoid(DISTRIBUTOR, "distributeGreen", StorageTypes.BIG_INTEGER),
			distributor, StorageValues.bigIntegerOf(1_000)
		);

		var balanceRed1 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			MethodSignatures.of(StorageTypes.EOA, "balanceRed", StorageTypes.BIG_INTEGER),
			account(1)
		);

		var balanceRed2 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			MethodSignatures.of(StorageTypes.EOA, "balanceRed", StorageTypes.BIG_INTEGER),
			account(2)
		);

		Assertions.assertEquals(ZERO, balanceRed1.getValue());
		Assertions.assertEquals(ZERO, balanceRed2.getValue());
	}

	@Test @DisplayName("new RedGreenDistributor() then adds two payees without red, distributes 1000 red and their red balance is 500")
	void createDistributorAndTwoPayeesThenDistributes1000Red() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		addInstanceVoidMethodCallTransaction(
			privateKey(1), account(1),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceVoidMethodCallTransaction(
			privateKey(2), account(2),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceVoidMethodCallTransaction(
			privateKey(0), account(0),
			_20_000,
			ONE,
			jar(),
			MethodSignatures.ofVoid(DISTRIBUTOR, "distributeRed", StorageTypes.BIG_INTEGER),
			distributor, StorageValues.bigIntegerOf(1_000)
		);

		BigIntegerValue balanceRed1 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			MethodSignatures.of(StorageTypes.EOA, "balanceRed", StorageTypes.BIG_INTEGER),
			account(1)
		);

		BigIntegerValue balanceRed2 = (BigIntegerValue) runInstanceMethodCallTransaction(
			account(0),
			_20_000,
			jar(),
			MethodSignatures.of(StorageTypes.EOA, "balanceRed", StorageTypes.BIG_INTEGER),
			account(2)
		);

		Assertions.assertEquals(500, balanceRed1.getValue().intValue());
		Assertions.assertEquals(500, balanceRed2.getValue().intValue());
	}

	@Test @DisplayName("distributeRed() cannot be called from an externally owned account that is not red/green")
	void distributeRedCannotBeCalledFromNonRedGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		KeyPair keys = signature().getKeyPair();
		ConstructorFuture eoa = postConstructorCallTransaction(
			privateKey(0), account(0),
			_20_000,
			ONE,
			jar(),
			ConstructorSignatures.of(StorageTypes.EOA, StorageTypes.BIG_INTEGER),
			StorageValues.bigIntegerOf(_20_000)
		);

		addInstanceVoidMethodCallTransaction(
			privateKey(1), account(1),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		addInstanceVoidMethodCallTransaction(
			privateKey(2), account(2),
			_20_000,
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);

		throwsTransactionException(() ->
			addInstanceVoidMethodCallTransaction(
				keys.getPrivate(),
				eoa.get(),
				_20_000,
				ONE,
				jar(),
				MethodSignatures.ofVoid(DISTRIBUTOR, "distributeRed", StorageTypes.BIG_INTEGER),
				distributor, StorageValues.bigIntegerOf(1_000)
			)
		);
	}

	@Test @DisplayName("new RedGreenDistributor() then fails while adding a payee without enough coins")
	void createDistributorThenFailsByAddingPayeeWithoutGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		throwsTransactionRejectedException(() ->
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

	@Test @DisplayName("new RedGreenDistributor() then adds a payee paying with both red and green")
	void createDistributorThenAddsPayeePayingWithRedAndGreen() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference distributor = addConstructorCallTransaction(privateKey(0), account(0), _100_000, ONE, jar(), ConstructorSignatures.of(DISTRIBUTOR));

		addInstanceVoidMethodCallTransaction(
			privateKey(3), account(3),
			BigInteger.valueOf(100_000), // more than 90,000, but it can use green after red are exhausted
			ONE,
			jar(),
			ADD_AS_PAYEE,
			distributor
		);
	}
}