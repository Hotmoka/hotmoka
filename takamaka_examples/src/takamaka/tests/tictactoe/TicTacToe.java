package takamaka.tests.tictactoe;

import static takamaka.lang.Takamaka.require;

import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.lang.View;
import takamaka.util.StorageArray;

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

	private final StorageArray<Tile> board = new StorageArray<>(9);
	private PayableContract crossPlayer, circlePlayer;
	private Tile turn = Tile.CROSS; // cross plays first
	private boolean gameOver;

	public TicTacToe() {
		rangeClosed(0, 8).forEach(index -> board.set(index, Tile.EMPTY));
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
			if (crossPlayer == null)
				crossPlayer = player;
			else
				require(player == crossPlayer, "it's not your turn");
		else
			if (circlePlayer == null) {
				require(amount >= balance().longValue(), () -> "You must bet at least " + balance() + " coins");
				circlePlayer = player;
			}
			else
				require(player == circlePlayer, "it's not your turn");

		set(x, y, turn);
		if (gameOver(x, y))
			player.receive(balance());

		turn = turn.nextTurn();
	}

	private boolean gameOver(int x, int y) {
		return gameOver =
			rangeClosed(1, 3).allMatch(_y -> at(x, _y) == turn) || // column x
			rangeClosed(1, 3).allMatch(_x -> at(_x, y) == turn) || // row y
			(x == y && rangeClosed(1, 3).allMatch(_x -> at(_x, _x) == turn)) || // first diagonal
			(x + y == 4 && rangeClosed(1, 3).allMatch(_x -> at(_x, 4 - _x) == turn)); // second diagonal
	}

	@Override
	public @View String toString() {
		return rangeClosed(0, 8)
			.mapToObj(index -> (index == 3 || index == 6) ? board.get(index) + "\n" : board.get(index).toString())
			.collect(Collectors.joining());
	}
}