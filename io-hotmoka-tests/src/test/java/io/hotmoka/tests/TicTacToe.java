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

import static io.hotmoka.beans.Coin.filicudi;
import static io.hotmoka.beans.Coin.panarea;
import static io.hotmoka.beans.Coin.stromboli;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static io.hotmoka.beans.types.BasicTypes.LONG;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SignatureException;

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
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.LongValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StringValue;
import io.hotmoka.constants.Constants;

/**
 * A test for the remote purchase contract.
 */
class TicTacToe extends TakamakaTest {
	private static final ClassType TIC_TAC_TOE = new ClassType("io.hotmoka.examples.tictactoe.TicTacToe");
	private static final ConstructorSignature CONSTRUCTOR_TIC_TAC_TOE = new ConstructorSignature(TIC_TAC_TOE);
	private static final IntValue _1 = new IntValue(1);
	private static final IntValue _2 = new IntValue(2);
	private static final IntValue _3 = new IntValue(3);

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
	void createTicTacToe() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
	}

	@Test @DisplayName("new TicTacToe() then first player plays")
	void crossPlays() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(100)),
			_1, _1);
		StringValue toString = (StringValue) runInstanceMethodCallTransaction(
			player1, 
			_100_000,
			jar(),
			new NonVoidMethodSignature(TIC_TAC_TOE, "toString", ClassType.STRING),
			ticTacToe);

		assertEquals("X| | \n-----\n | | \n-----\n | | ", toString.value);
	}

	@Test @DisplayName("new TicTacToe(), first player plays, second player plays same position")
	void bothPlaySamePosition() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(100)),
			_1, _1);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(3),
				player2,
				_100_000,
				panarea(1),
				jar(),
				new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(panarea(100)),
				_1, _1)
		);
	}

	@Test @DisplayName("new TicTacToe(), same player plays twice")
	void samePlayerPlaysTwice() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(100)),
			_1, _1);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(2),
				player1,
				_100_000,
				panarea(1),
				jar(),
				new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(panarea(100)),
				_1, _2)
		);
	}

	@Test @DisplayName("new TicTacToe(), second player bets too little")
	void circleBetsTooLittle() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1,
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(120)),
			_1, _1);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(3),
				player2,
				_100_000,
				panarea(1),
				jar(),
				new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(panarea(119)),
				_1, _2)
		);
	}

	@Test @DisplayName("first player wins")
	void crossWins() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(100)),
			_1, _1);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(100)),
			_2, _1);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(0)),
			_1, _2);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(0)),
			_2, _2);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(0)),
			_1, _3);

		StringValue toString = (StringValue) runInstanceMethodCallTransaction(
			player1, 
			_100_000,
			jar(),
			new NonVoidMethodSignature(TIC_TAC_TOE, "toString", ClassType.STRING),
			ticTacToe);

		assertEquals("X|O| \n-----\nX|O| \n-----\nX| | ", toString.value);
	}


	@Test @DisplayName("first player wins but second continues to play")
	void crossWinsButCircleContinues() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		StorageReference ticTacToe = addConstructorCallTransaction(privateKey(1), creator, _500_000, panarea(1), jar(), CONSTRUCTOR_TIC_TAC_TOE);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(100)),
			_1, _1);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(100)),
			_2, _1);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(0)),
			_1, _2);
		addInstanceMethodCallTransaction(
			privateKey(3),
			player2,
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(0)),
			_2, _2);
		addInstanceMethodCallTransaction(
			privateKey(2),
			player1, 
			_100_000,
			panarea(1),
			jar(),
			new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(panarea(0)),
			_1, _3);

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
			addInstanceMethodCallTransaction(
				privateKey(3),
				player2, 
				_100_000,
				panarea(1),
				jar(),
				new VoidMethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(panarea(0)),
				_2, _3)
		);
	}
}