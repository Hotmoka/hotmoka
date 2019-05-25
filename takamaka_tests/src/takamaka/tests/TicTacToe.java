/**
 * 
 */
package takamaka.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static takamaka.blockchain.types.BasicTypes.INT;
import static takamaka.blockchain.types.BasicTypes.LONG;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import takamaka.blockchain.Blockchain;
import takamaka.blockchain.Classpath;
import takamaka.blockchain.CodeExecutionException;
import takamaka.blockchain.ConstructorSignature;
import takamaka.blockchain.MethodSignature;
import takamaka.blockchain.TransactionException;
import takamaka.blockchain.TransactionReference;
import takamaka.blockchain.request.ConstructorCallTransactionRequest;
import takamaka.blockchain.request.GameteCreationTransactionRequest;
import takamaka.blockchain.request.InstanceMethodCallTransactionRequest;
import takamaka.blockchain.request.JarStoreInitialTransactionRequest;
import takamaka.blockchain.request.JarStoreTransactionRequest;
import takamaka.blockchain.types.ClassType;
import takamaka.blockchain.values.IntValue;
import takamaka.blockchain.values.LongValue;
import takamaka.blockchain.values.StorageReference;
import takamaka.blockchain.values.StringValue;
import takamaka.lang.RequirementViolationException;
import takamaka.memory.MemoryBlockchain;

/**
 * A test for the remote purchase contract.
 */
class TicTacToe {

	private static final ClassType TIC_TAC_TOE = new ClassType("takamaka.tests.tictactoe.TicTacToe");

	private static final ConstructorSignature CONSTRUCTOR_TIC_TAC_TOE = new ConstructorSignature(TIC_TAC_TOE);

	private static final BigInteger _1_000 = BigInteger.valueOf(1_000);

	private static final BigInteger _20_000 = BigInteger.valueOf(20_000);

	private static final BigInteger ALL_FUNDS = BigInteger.valueOf(1_000_000_000);

	private static final IntValue _1 = new IntValue(1);
	private static final IntValue _2 = new IntValue(2);
	private static final IntValue _3 = new IntValue(3);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private Blockchain blockchain;

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

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	@BeforeEach
	void beforeEach() throws Exception {
		blockchain = new MemoryBlockchain(Paths.get("chain"));

		TransactionReference takamaka_base = blockchain.addJarStoreInitialTransaction(new JarStoreInitialTransactionRequest(Files.readAllBytes(Paths.get("../takamaka_runtime/dist/takamaka_base.jar"))));
		Classpath takamakaBase = new Classpath(takamaka_base, false);  // true/false irrelevant here

		StorageReference gamete = blockchain.addGameteCreationTransaction(new GameteCreationTransactionRequest(takamakaBase, ALL_FUNDS));

		TransactionReference tictactoe = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(gamete, _20_000, takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/tictactoe.jar")), takamakaBase));

		classpath = new Classpath(tictactoe, true);

		creator = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount", INT), new IntValue(100_000)));

		player1 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount", INT), new IntValue(1_000_000)));

		player2 = blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
			(gamete, _1_000, classpath, new ConstructorSignature("takamaka.lang.ExternallyOwnedAccount", INT), new IntValue(1_000_000)));
	}

	@Test @DisplayName("new TicTacToe()")
	void createTicTacToe() throws TransactionException, CodeExecutionException {
		blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(creator, _20_000, classpath, CONSTRUCTOR_TIC_TAC_TOE));
	}

	@Test @DisplayName("new TicTacToe() then first player plays")
	void crossPlays() throws TransactionException, CodeExecutionException {
		StorageReference ticTacToe = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(creator, _20_000, classpath, CONSTRUCTOR_TIC_TAC_TOE));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(100L),
			_1, _1));
		StringValue toString = (StringValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "toString"),
			ticTacToe));

		assertEquals("X| | \n-----\n | | \n-----\n | | ", toString.value);
	}

	@Test @DisplayName("new TicTacToe(), first player plays, second player plays same position")
	void bothPlaySamePosition() throws TransactionException, CodeExecutionException {
		StorageReference ticTacToe = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(creator, _20_000, classpath, CONSTRUCTOR_TIC_TAC_TOE));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(100L),
			_1, _1));

		try {
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
				player2,
				_20_000,
				classpath,
				new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(100L),
				_1, _1));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;

			fail("wrong exception");
		}

		fail("no exception");
	}

	@Test @DisplayName("new TicTacToe(), same player plays twice")
	void samePlayerPlaysTwice() throws TransactionException, CodeExecutionException {
		StorageReference ticTacToe = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(creator, _20_000, classpath, CONSTRUCTOR_TIC_TAC_TOE));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(100L),
			_1, _1));

		try {
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
				player1,
				_20_000,
				classpath,
				new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(100L),
				_1, _2));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;

			fail("wrong exception");
		}

		fail("no exception");
	}

	@Test @DisplayName("new TicTacToe(), second player bets too little")
	void circleBetsTooLittle() throws TransactionException, CodeExecutionException {
		StorageReference ticTacToe = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(creator, _20_000, classpath, CONSTRUCTOR_TIC_TAC_TOE));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1,
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(120L),
			_1, _1));

		try {
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
				player2,
				_20_000,
				classpath,
				new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(119L),
				_1, _2));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;

			fail("wrong exception");
		}

		fail("no exception");
	}

	@Test @DisplayName("first player wins")
	void crossWins() throws TransactionException, CodeExecutionException {
		StorageReference ticTacToe = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(creator, _20_000, classpath, CONSTRUCTOR_TIC_TAC_TOE));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(100L),
			_1, _1));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player2,
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(100L),
			_2, _1));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(0L),
			_1, _2));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player2,
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(0L),
			_2, _2));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(0L),
			_1, _3));

		StringValue toString = (StringValue) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "toString"),
			ticTacToe));

		assertEquals("X|O| \n-----\nX|O| \n-----\nX| | ", toString.value);
	}


	@Test @DisplayName("first player wins but second continues to play")
	void crossWinsButCircleContinues() throws TransactionException, CodeExecutionException {
		StorageReference ticTacToe = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(creator, _20_000, classpath, CONSTRUCTOR_TIC_TAC_TOE));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(100L),
			_1, _1));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player2,
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(100L),
			_2, _1));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(0L),
			_1, _2));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player2,
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(0L),
			_2, _2));
		blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
			player1, 
			_20_000,
			classpath,
			new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
			ticTacToe,
			new LongValue(0L),
			_1, _3));
		try {
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest(
				player2, 
				_20_000,
				classpath,
				new MethodSignature(TIC_TAC_TOE, "play", LONG, INT, INT),
				ticTacToe,
				new LongValue(0L),
				_2, _3));
		}
		catch (TransactionException e) {
			if (e.getCause() instanceof RequirementViolationException)
				return;

			fail("wrong exception");
		}

		fail("no exception");
	}
}