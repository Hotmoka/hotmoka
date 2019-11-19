package io.takamaka.tests.tictactoe;

import static io.takamaka.code.lang.Takamaka.require;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;

import java.math.BigInteger;

import io.takamaka.code.lang.Contract;
import io.takamaka.code.lang.Entry;
import io.takamaka.code.lang.Payable;
import io.takamaka.code.lang.PayableContract;
import io.takamaka.code.lang.View;
import io.takamaka.code.util.StorageArray;

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

	public static enum Tile {
		EMPTY, CROSS, CIRCLE;

		@Override
		public String toString() {
			switch (this) {
			case EMPTY: return " ";
			case CROSS: return "X";
			default: return "O";
			}
		}

		private Tile nextTurn() {
			return this == CROSS ? CIRCLE : CROSS;
		}
	}

	private static final long MINIMUM_BET = 100L;

	private final StorageArray<Tile> board = new StorageArray<>(9, Tile.EMPTY);
	private PayableContract creator, crossPlayer, circlePlayer;
	private Tile turn = Tile.CROSS; // cross plays first
	private boolean gameOver;

	public @Entry(PayableContract.class) TicTacToe() {
		creator = (PayableContract) caller();
	}

	public @View Tile at(int x, int y) {
		require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
		return board.get((y - 1) * 3 + x - 1);
	}

	private void set(int x, int y, Tile tile) {
		board.set((y - 1) * 3 + x - 1, tile);
	}

	public @Payable @Entry(PayableContract.class) void play(long amount, int x, int y) {
		require(!gameOver, "the game is over");
		require(1 <= x && x <= 3 && 1 <= y && y <= 3, "coordinates must be between 1 and 3");
		require(at(x, y) == Tile.EMPTY, "the selected tile is not empty");

		PayableContract player = (PayableContract) caller();

		if (turn == Tile.CROSS)
			if (crossPlayer == null) {
				require(amount >= MINIMUM_BET, () -> "you must invest at least " + MINIMUM_BET + " coins");
				crossPlayer = player;
			}
			else
				require(player == crossPlayer, "it's not your turn");
		else
			if (circlePlayer == null) {
				require(crossPlayer != player, "you cannot play against yourself");
				long previousBet = balance().subtract(BigInteger.valueOf(amount)).longValue();
				require(amount >= previousBet, () -> "you must bet at least " + previousBet + " coins");
				circlePlayer = player;
			}
			else
				require(player == circlePlayer, "it's not your turn");

		set(x, y, turn);
		if (isGameOver(x, y)) {
			// 90% goes to the winner
			player.receive(balance().multiply(BigInteger.valueOf(9L)).divide(BigInteger.valueOf(10L)));
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
		return gameOver =
			rangeClosed(1, 3).allMatch(_y -> at(x, _y) == turn) || // column x
			rangeClosed(1, 3).allMatch(_x -> at(_x, y) == turn) || // row y
			(x == y && rangeClosed(1, 3).allMatch(_x -> at(_x, _x) == turn)) || // first diagonal
			(x + y == 4 && rangeClosed(1, 3).allMatch(_x -> at(_x, 4 - _x) == turn)); // second diagonal
	}

	private boolean isDraw() {
		return rangeClosed(0, 8).mapToObj(board::get).noneMatch(Tile.EMPTY::equals);
	}

	@Override
	public @View String toString() {
		return rangeClosed(1, 3)
			.mapToObj(y -> rangeClosed(1, 3).mapToObj(x -> at(x, y).toString()).collect(joining("|")))
			.collect(joining("\n-----\n"));
	}
}