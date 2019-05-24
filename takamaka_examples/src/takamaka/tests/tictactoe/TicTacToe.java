package takamaka.tests.tictactoe;

import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.rangeClosed;
import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.lang.View;
import takamaka.util.StorageArray;

/**
 * A contract for the tic-tac-toe game. Two players play alternately.
 */
public class TicTacToe extends Contract {

	public static enum Tile {
		EMPTY(" "), CROSS("X"), CIRCLE("O");

		private final String name;

		private Tile(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

		private Tile nextTurn() {
			return this == CROSS ? CIRCLE : CROSS;
		}
	}

	private static final long MINIMUM_INVESTMENT = 100L;

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
				require(amount >= MINIMUM_INVESTMENT, () -> "you must invest at least " + MINIMUM_INVESTMENT + " coins");
				crossPlayer = player;
			}
			else
				require(player == crossPlayer, "it's not your turn");
		else
			if (circlePlayer == null) {
				require(crossPlayer != player, "you cannot play against yourself");
				long previousBet = balance().longValue() - amount;
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
			.collect(joining("\n-----"));
	}
}