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

import static io.hotmoka.helpers.Coin.filicudi;
import static io.hotmoka.helpers.Coin.panarea;
import static io.hotmoka.helpers.Coin.stromboli;
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageTypes.LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;
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
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.IntValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StringValue;
import io.takamaka.code.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class TicTacToe extends HotmokaTest {
	private static final ClassType TIC_TAC_TOE = StorageTypes.classNamed("io.hotmoka.examples.tictactoe.TicTacToe");
	private static final ConstructorSignature CONSTRUCTOR_TIC_TAC_TOE = ConstructorSignatures.of(TIC_TAC_TOE);
	private static final IntValue _1 = StorageValues.intOf(1);
	private static final IntValue _2 = StorageValues.intOf(2);
	private static final IntValue _3 = StorageValues.intOf(3);

	/**
	 * The creator of the game.
	 */
	private StorageReference creator;

	/**
	 * The first player.
	 */
	private StorageReference player1;

	/**
	 * The second player.
	 */
	private StorageReference player2;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("tictactoe.jar");
	}

	@BeforeEach
	void beforeEach() throws Exception {
		setAccounts(stromboli(1), filicudi(100), filicudi(100), filicudi(100));
		creator = account(1);
		player1 = account(2);
		player2 = account(3);
	}

	@Test @DisplayName("new TicTacToe()")
	void createTicTacToe() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
	}

	@Test @DisplayName("new TicTacToe() then first player plays")
	void crossPlays() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(100).longValue()),
			_1, _1);
		StringValue toString = (StringValue) runInstanceMethodCallTransaction(
			player1, 
			_100_000,
			jar(),
			MethodSignatures.of(TIC_TAC_TOE, "toString", StorageTypes.STRING),
			ticTacToe);

		assertEquals("X| | \n-----\n | | \n-----\n | | ", toString.getValue());
	}

	@Test @DisplayName("new TicTacToe(), first player plays, second player plays same position")
	void bothPlaySamePosition() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(100).longValue()),
			_1, _1);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(3),
				player2,
				_100_000,
				panarea(1),
				jar(),
				MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				StorageValues.longOf(panarea(100).longValue()),
				_1, _1)
		);
	}

	@Test @DisplayName("new TicTacToe(), same player plays twice")
	void samePlayerPlaysTwice() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(100).longValue()),
			_1, _1);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(2),
				player1,
				_100_000,
				panarea(1),
				jar(),
				MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				StorageValues.longOf(panarea(100).longValue()),
				_1, _2)
		);
	}

	@Test @DisplayName("new TicTacToe(), second player bets too little")
	void circleBetsTooLittle() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1,
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(120).longValue()),
			_1, _1);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(3),
				player2,
				_100_000,
				panarea(1),
				jar(),
				MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				StorageValues.longOf(panarea(119).longValue()),
				_1, _2)
		);
	}

	@Test @DisplayName("first player wins")
	void crossWins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(100).longValue()),
			_1, _1);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(100).longValue()),
			_2, _1);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(0).longValue()),
			_1, _2);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(0).longValue()),
			_2, _2);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(0).longValue()),
			_1, _3);

		StringValue toString = (StringValue) runInstanceMethodCallTransaction(
			player1, 
			_100_000,
			jar(),
			MethodSignatures.of(TIC_TAC_TOE, "toString", StorageTypes.STRING),
			ticTacToe);

		assertEquals("X|O| \n-----\nX|O| \n-----\nX| | ", toString.getValue());
	}


	@Test @DisplayName("first player wins but second continues to play")
	void crossWinsButCircleContinues() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException, NodeException, TimeoutException, InterruptedException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(100).longValue()),
			_1, _1);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(100).longValue()),
			_2, _1);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(0).longValue()),
			_1, _2);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(0).longValue()),
			_2, _2);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			StorageValues.longOf(panarea(0).longValue()),
			_1, _3);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(3),
				player2, 
				_100_000,
				panarea(1),
				jar(),
				MethodSignatures.ofVoid(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				StorageValues.longOf(panarea(0).longValue()),
				_2, _3)
		);
	}
}