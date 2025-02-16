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

package io.hotmoka.examples.tictactoe;

import static io.takamaka.code.lang.Takamaka.require;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.FromContract;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.Storage;
import io.takamaka.code.lang.StringSupport;
import io.takamaka.code.lang.View;
import io.takamaka.code.math.BigIntegerSupport;
import io.takamaka.code.util.StorageTreeArray;

/**
 * A contract for the tic-tac-toe game. Two players place, alternately,
 * a cross and a circle on a 3x3 board, initially empty. The winner is the
 * player who places three crosses or three circles on the same row, or
 * column or diagonal. The first player specifies a bet; the second must
 * bet at least as much as the first. At the end, the winner gets 90%
 * of the jackpot, while the rest goes to the creator of the contract.
 * In case of a draw, the jackpot goes entirely to the creator of the contract.
 */
public class TicTacToe extends Contract {

	public class Tile extends Storage {
		private final char c;

		private Tile(char c) {
			this.c = c;
		}

		@Override
		public String toString() {
			return String.valueOf(c);
		}

		private Tile nextTurn() {
			return this == CROSS ? CIRCLE : CROSS;
		}
	}

	private final Tile EMPTY = new Tile(' ');
	private final Tile CROSS = new Tile('X');
	private final Tile CIRCLE = new Tile('O');

	private static final long MINIMUM_BET = 100L;

	private final StorageTreeArray<Tile> board = new StorageTreeArray<>(9, EMPTY);
	private final PayableContract creator;
	private PayableContract crossPlayer;
	private PayableContract circlePlayer;
	private Tile turn = CROSS; // cross plays first
	private boolean gameOver;

	public @FromContract(PayableContract.class) TicTacToe() {
		creator = (PayableContract) caller();
	}

	public @View Tile at(int x, int y) {
		require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
		return board.get((y - 1) * 3 + x - 1);
	}

	private void set(int x, int y, Tile tile) {
		board.set((y - 1) * 3 + x - 1, tile);
	}

	public @Payable @FromContract(PayableContract.class) void play(long amount, int x, int y) {
		require(!gameOver, "the game is over");
		require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
		require(at(x, y) == EMPTY, "the selected tile is not empty");

		PayableContract player = (PayableContract) caller();

		if (turn == CROSS)
			if (crossPlayer == null) {
				require(amount >= MINIMUM_BET, () -> "you must invest at least " + MINIMUM_BET + " coins");
				crossPlayer = player;
			}
			else
				require(player == crossPlayer, "it's not your turn");
		else
			if (circlePlayer == null) {
				require(crossPlayer != player, "you cannot play against yourself");
				long previousBet = BigIntegerSupport.subtract(balance(), BigInteger.valueOf(amount)).longValue();
				require(amount >= previousBet, () -> StringSupport.concat("you must bet at least ", previousBet, " coins"));
				circlePlayer = player;
			}
			else
				require(player == circlePlayer, "it's not your turn");

		set(x, y, turn);
		if (isGameOver(x, y)) {
			// 90% goes to the winner
			player.receive(BigIntegerSupport.divide(BigIntegerSupport.multiply(balance(), BigInteger.valueOf(9L)), BigInteger.valueOf(10L)));
			// the rest to the creator of the game
			creator.receive(balance());
		}
		else if (isDraw())
			// everything goes to the creator of the game
			creator.receive(balance());
		else
			turn = turn.nextTurn();
	}

	private boolean isGameOver(int x, int y) {
		if (at(x, 1) == turn && at(x, 2) == turn && at(x, 3) == turn) // column x
			return gameOver = true;

		if (at(1, y) == turn && at(2, y) == turn && at(3, y) == turn) // row y
			return gameOver = true;

		if (x == y && at(1, 1) == turn && at (2, 2) == turn && at(3, 3) == turn) // first diagonal
			return gameOver = true;

		if (x + y == 4 && at(1, 3) == turn && at(2, 2) == turn && at(3, 1) == turn) // second diagonal
			return gameOver = true;

		return gameOver = false;
	}

	private boolean isDraw() {
		for (var tile: board)
			if (tile == EMPTY)
				return false;

		return true;
	}

	@Override
	public @View String toString() {
		return StringSupport.concat(at(1, 1), "|", at(2, 1), "|", at(3, 1), "\n-----\n", at(1, 2), "|", at(2, 2), "|", at(3, 2), "\n-----\n", at(1, 3), "|", at(2, 3), "|", at(3, 3));
	}
}