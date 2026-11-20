/*
Copyright 2026 Fausto Spoto

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

package io.hotmoka.tests.gas;

import static io.hotmoka.node.StorageTypes.BIG_INTEGER;
import static io.hotmoka.node.StorageTypes.LONG;
import static io.hotmoka.node.StorageTypes.PAYABLE_CONTRACT;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import io.hotmoka.helpers.api.UnexpectedCodeException;
import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.ClosedNodeException;
import io.hotmoka.node.api.CodeExecutionException;
import io.hotmoka.node.api.MethodFuture;
import io.hotmoka.node.api.TransactionException;
import io.hotmoka.node.api.TransactionRejectedException;
import io.hotmoka.node.api.UnknownReferenceException;
import io.hotmoka.node.api.responses.NonInitialTransactionResponse;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.tests.HotmokaTest;

@EnabledIfSystemProperty(named = "gasExperiments", matches = "true")
class GasComputations extends HotmokaTest {
	private static final BigInteger GAS_PRICE = BigInteger.ONE;
	private static final BigInteger GAS_LIMIT = _10_000_000;
	private final static int ACCOUNTS_NUM = 6;

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(IntStream.range(0, ACCOUNTS_NUM).mapToObj(__ -> _1_000_000_000).toArray(BigInteger[]::new));
	}

	@Test
	@DisplayName("Create SimplePonzi, report gas of calls to invest()")
	void simplePonziInvest() throws Exception {
		System.out.println("SimplePonzi.invest():");
		setJar("ponzi.jar");

		var simplePonziType = StorageTypes.classNamed("io.hotmoka.examples.ponzi.SimplePonzi");
		// account(0) creates the contract and becomes the first investor
		var contract = addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, jar(), ConstructorSignatures.of(simplePonziType));

		var invest = MethodSignatures.ofVoid(simplePonziType, "invest", StorageTypes.BIG_INTEGER);
		int account = 0;
		for (BigInteger investment = BigInteger.valueOf(1000); investment.compareTo(_10_000_000) < 0; investment = investment.multiply(BigInteger.TWO)) {
			System.out.printf("account(%d) calls SimplePonzi.invest(%s): ", account, investment);
			reportGasFor(postInstanceMethodCallTransaction(privateKey(account), account(account), GAS_LIMIT, GAS_PRICE, jar(), invest, contract, StorageValues.bigIntegerOf(investment)));
			account++;
			account = account % ACCOUNTS_NUM;
		}
	}

	@Test
	@DisplayName("Create GradualPonzi, report gas of calls to invest()")
	void gradualPonziInvest() throws Exception {
		System.out.println("GradualPonzi.invest():");
		setJar("ponzi.jar");

		var gradualPonziType = StorageTypes.classNamed("io.hotmoka.examples.ponzi.GradualPonzi");
		var invest = MethodSignatures.ofVoid(gradualPonziType, "invest", StorageTypes.BIG_INTEGER);

		for (BigInteger investment = BigInteger.valueOf(1000); investment.compareTo(_500_000) < 0;investment = investment.multiply(BigInteger.TWO)) {
			// account(0) creates the contract and becomes the first investor
			var contract = addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, jar(), ConstructorSignatures.of(gradualPonziType));
			int account = 0;
			for (int l = 1; l < 20; l++) { // l is the number of investors...
				System.out.printf("[l=%d]: account(%d) calls GradualPonzi.invest(%s): ", l, account, investment);
				reportGasFor(postInstanceMethodCallTransaction(privateKey(account), account(account), GAS_LIMIT, GAS_PRICE, jar(), invest, contract, StorageValues.bigIntegerOf(investment)));
				account = ++account % ACCOUNTS_NUM;
			}
		}
	}

	@Test
	@DisplayName("Create SimpleSharedEntity, report gas of calls to place()")
	void simpleSharedEntityPlaceOffer() throws Exception {
		System.out.println("SimpleSharedEntity.place():");

		var offerConstructor = ConstructorSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, LONG);
		var place = MethodSignatures.ofVoid(StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity"), "place", BIG_INTEGER, StorageTypes.SHARED_ENTITY_OFFER);
		var takamakaCode = takamakaCode();
		// create the shared entity contract, with four shareholders
		int sharesOfFirstShareholder = 100;

		for (int S = 1; S <= 4; S++) { // S is the number of shareholders
			StorageReference contract = mkSimpleSharedEntity(sharesOfFirstShareholder, S);

			int howManySold = sharesOfFirstShareholder / 100;
			var offers = new StorageReference[40];
			for (int F = 0, account = 0; F < 40; F++, account = ++account % S) { // F is the number of already placed offers
				// create an offer of sale
				offers[F] = addConstructorCallTransaction(privateKey(account), account(account), GAS_LIMIT, GAS_PRICE, takamakaCode,
						offerConstructor, account(account), StorageValues.bigIntegerOf(howManySold),
						StorageValues.bigIntegerOf(0), StorageValues.longOf(1893456000)); // cost, duration
				System.out.printf("[S=%d, F=%d]: account(%d) calls SimpleSharedEntity.place(0, newOffer): ", S, F, account);
				// place the offer of sale
				reportGasFor(postInstanceMethodCallTransaction(privateKey(account), account(account), GAS_LIMIT, GAS_PRICE, takamakaCode,
						place, contract, StorageValues.bigIntegerOf(0), offers[F]));
			}
		}
	}

	@Test
	@DisplayName("Create SimpleSharedEntity, report gas of calls to accept()")
	void simpleSharedEntityAcceptOffer() throws Exception {
		System.out.println("SimpleSharedEntity.accept():");

		var offerConstructor = ConstructorSignatures.of(StorageTypes.SHARED_ENTITY_OFFER, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, LONG);
		ClassType simpleSharedEntityType = StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity");
		var place = MethodSignatures.ofVoid(simpleSharedEntityType, "place", BIG_INTEGER, StorageTypes.SHARED_ENTITY_OFFER);
		var accept = MethodSignatures.ofVoid(simpleSharedEntityType, "accept", BIG_INTEGER, PAYABLE_CONTRACT, StorageTypes.SHARED_ENTITY_OFFER);
		var takamakaCode = takamakaCode();
		// create the shared entity contract, with four shareholders
		int sharesOfFirstShareholder = 100;

		for (int S = 1; S <= 4; S++) { // S is the number of shareholders
			StorageReference contract = mkSimpleSharedEntity(sharesOfFirstShareholder, S);

			int howManySold = sharesOfFirstShareholder / 100;
			var offers = new StorageReference[40];
			int F, account;
			for (F = 0, account = 0; F < 40; F++, account = ++account % S) { // F is the number of already placed offers
				// create an offer of sale
				offers[F] = addConstructorCallTransaction(privateKey(account), account(account), GAS_LIMIT, GAS_PRICE, takamakaCode,
						offerConstructor, account(account), StorageValues.bigIntegerOf(howManySold),
						StorageValues.bigIntegerOf(0), StorageValues.longOf(1893456000)); // cost, duration
				// place the offer of sale
				addInstanceVoidMethodCallTransaction(privateKey(account), account(account), GAS_LIMIT, GAS_PRICE, takamakaCode,
						place, contract, StorageValues.bigIntegerOf(0), offers[F]);
			}

			for (; F > 0; F--) {
				System.out.printf("[S=%d, F=%d]: account(5) calls SimpleSharedEntity.accept(0, offer): ", S, F);
				// accept the offer of sale
				reportGasFor(postInstanceMethodCallTransaction(privateKey(5), account(5), GAS_LIMIT, GAS_PRICE, takamakaCode,
						accept, contract, StorageValues.bigIntegerOf(0), account(5), offers[F - 1]));
			}
		}
	}

	private StorageReference mkSimpleSharedEntity(int sharesOfFirstShareholder, int S) throws InvalidKeyException, ClosedNodeException, SignatureException,
			UnexpectedCodeException, TransactionRejectedException, TransactionException, CodeExecutionException,
			TimeoutException, InterruptedException, UnknownReferenceException {
		
		var simpleSharedEntityType = StorageTypes.classNamed("io.takamaka.code.dao.SimpleSharedEntity");

		return switch (S) {
		case 1 -> addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, takamakaCode(),
				ConstructorSignatures.of(simpleSharedEntityType, PAYABLE_CONTRACT, BIG_INTEGER),
				account(0),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder));
		case 2 -> addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, takamakaCode(),
				ConstructorSignatures.of(simpleSharedEntityType, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER),
				account(0), account(1),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder * 2));
		case 3 -> addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, takamakaCode(),
				ConstructorSignatures.of(simpleSharedEntityType, PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER),
				account(0), account(1), account(2),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder * 2),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder * 3));
		case 4 -> addConstructorCallTransaction(privateKey(0), account(0), GAS_LIMIT, GAS_PRICE, takamakaCode(),
				ConstructorSignatures.of(simpleSharedEntityType, PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, PAYABLE_CONTRACT, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER, BIG_INTEGER),
				account(0), account(1), account(2), account(3),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder * 2),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder * 3),
				StorageValues.bigIntegerOf(sharesOfFirstShareholder * 4));
		default -> throw new IllegalStateException();
		};
	}

	private void reportGasFor(MethodFuture future) throws Exception {
		future.get();
		var response = (NonInitialTransactionResponse) getResponse(future.getReferenceOfRequest());
		System.out.println("CPU: " + response.getGasConsumedForCPU() + ", RAM: " + response.getGasConsumedForRAM()
			+ ", Storage: " + response.getGasConsumedForStorage());
	}
}