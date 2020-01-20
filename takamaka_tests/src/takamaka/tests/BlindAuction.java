/**
 * 
 */
package takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.BasicTypes.BYTE;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.references.Classpath;
import io.hotmoka.beans.references.TransactionReference;
import io.hotmoka.beans.requests.ConstructorCallTransactionRequest;
import io.hotmoka.beans.requests.InstanceMethodCallTransactionRequest;
import io.hotmoka.beans.requests.JarStoreTransactionRequest;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.StorageReference;
import io.takamaka.code.constants.Constants;
import io.takamaka.code.engine.CodeExecutionException;
import io.takamaka.code.memory.InitializedMemoryBlockchain;

/**
 * A test for the blind auction contract.
 */
class BlindAuction extends TakamakaTest {

	/**
	 * The number of bids placed by the players.
	 */
	private static final int NUM_BIDS = 80;

	/**
	 * The bidding time of the experiments (in milliseconds).
	 */
	private static final int BIDDING_TIME = 40_000;

	/**
	 * The reveal time of the experiments (in millisecond).
	 */
	private static final int REVEAL_TIME = 60_000;

	private static final BigInteger _100_000 = BigInteger.valueOf(100_000);

	private static final ClassType BLIND_AUCTION = new ClassType("io.takamaka.tests.auction.BlindAuction");

	private static final ConstructorSignature CONSTRUCTOR_BLIND_AUCTION = new ConstructorSignature(BLIND_AUCTION, INT, INT);

	private static final ConstructorSignature CONSTRUCTOR_BYTES32 = new ConstructorSignature
		(ClassType.BYTES32,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE);

	private static final ConstructorSignature CONSTRUCTOR_STORAGE_LIST = new ConstructorSignature(ClassType.STORAGE_LIST);

	private static final ConstructorSignature CONSTRUCTOR_REVEALED_BID = new ConstructorSignature(new ClassType("io.takamaka.tests.auction.BlindAuction$RevealedBid"),
			ClassType.BIG_INTEGER, BOOLEAN, ClassType.BYTES32);

	private static final MethodSignature BID = new VoidMethodSignature(BLIND_AUCTION, "bid", ClassType.BIG_INTEGER, ClassType.BYTES32);

	private static final MethodSignature REVEAL = new VoidMethodSignature(BLIND_AUCTION, "reveal", ClassType.STORAGE_LIST);

	private static final MethodSignature AUCTION_END = new NonVoidMethodSignature(BLIND_AUCTION, "auctionEnd", ClassType.PAYABLE_CONTRACT);

	private static final MethodSignature GET_BALANCE = new NonVoidMethodSignature(ClassType.TEOA, "getBalance", ClassType.BIG_INTEGER);

	private static final MethodSignature ADD = new VoidMethodSignature(ClassType.STORAGE_LIST, "add", ClassType.OBJECT);

	private static final BigInteger _10_000_000 = BigInteger.valueOf(10_000_000);

	/**
	 * The blockchain under test. This is recreated before each test.
	 */
	private InitializedMemoryBlockchain blockchain;

	/**
	 * The classpath of the classes being tested.
	 */
	private Classpath classpath;

	/**
	 * The hashing algorithm used to hide the bids.
	 */
	private MessageDigest digest;

	@BeforeEach
	void beforeEach() throws Exception {
		digest = MessageDigest.getInstance("SHA-256");
		blockchain = new InitializedMemoryBlockchain(Paths.get("../distribution/dist/io-takamaka-code-1.0.jar"), _10_000_000, _10_000_000, _10_000_000, _10_000_000);

		TransactionReference auctions = blockchain.addJarStoreTransaction
			(new JarStoreTransactionRequest(blockchain.account(0), _10_000_000, blockchain.takamakaBase,
			Files.readAllBytes(Paths.get("../takamaka_examples/dist/auctions.jar")), blockchain.takamakaBase));

		classpath = new Classpath(auctions, true);
	}

	@Test @DisplayName("three players put bids before end of bidding time")
	void bids() throws TransactionException, CodeExecutionException {
		StorageReference auction = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _100_000, classpath, CONSTRUCTOR_BLIND_AUCTION, new IntValue(BIDDING_TIME), new IntValue(REVEAL_TIME)));

		Random random = new Random();
		for (int i = 1; i <= NUM_BIDS; i++) {
			int player = 1 + random.nextInt(3);
			BigInteger deposit = BigInteger.valueOf(random.nextInt(1000));
			BigInteger value = BigInteger.valueOf(random.nextInt(1000));
			boolean fake = random.nextBoolean();
			byte[] salt = new byte[32];
			random.nextBytes(salt);
			StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);
        	blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(player), _100_000, classpath, BID, auction, new BigIntegerValue(deposit), bytes32));
		}
	}

	@Test @DisplayName("three players put bids but bidding time expires")
	void biddingTimeExpires() throws TransactionException, CodeExecutionException {
		StorageReference auction = blockchain.addConstructorCallTransaction
				(new ConstructorCallTransactionRequest(blockchain.account(0), _100_000, classpath, CONSTRUCTOR_BLIND_AUCTION, new IntValue(4000), new IntValue(REVEAL_TIME)));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
		{
			Random random = new Random();
			for (int i = 1; i <= NUM_BIDS; i++) {
				int player = 1 + random.nextInt(3);
				BigInteger deposit = BigInteger.valueOf(random.nextInt(1000));
				BigInteger value = BigInteger.valueOf(random.nextInt(1000));
				boolean fake = random.nextBoolean();
				byte[] salt = new byte[32];
				random.nextBytes(salt);
				StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);
				blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
						(blockchain.account(player), _100_000, classpath, BID, auction, new BigIntegerValue(deposit), bytes32));
				sleep(1000);
			}
		});
	}

	/**
	 * Class used to keep in memory the bids placed by each player,
	 * that will be revealed at the end.
	 */
	private class BidToReveal {
		private final int player;
		private final BigInteger value;
		private final boolean fake;
		private final byte[] salt;

		private BidToReveal(int player, BigInteger value, boolean fake, byte[] salt) {
			this.player = player;
			this.value = value;
			this.fake = fake;
			this.salt = salt;
		}

		private StorageReference intoBlockchain() throws TransactionException, CodeExecutionException {
			return blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest
        		(blockchain.account(player), _100_000, classpath, CONSTRUCTOR_REVEALED_BID, new BigIntegerValue(value), new BooleanValue(fake), createBytes32(player, salt)));
		}
	}

	@Test @DisplayName("three players put bids before end of bidding time then reveal")
	void bidsThenReveal() throws TransactionException, CodeExecutionException {
		long start = System.currentTimeMillis();
		StorageReference auction = blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(0), _100_000, classpath, CONSTRUCTOR_BLIND_AUCTION, new IntValue(BIDDING_TIME), new IntValue(REVEAL_TIME)));

		List<BidToReveal> bids = new ArrayList<>();

		BigInteger maxBid = BigInteger.ZERO;
		StorageReference expectedWinner = null;
		Random random = new Random();
		int i = 1;
		while (i <= NUM_BIDS) {
			int player = 1 + random.nextInt(3);
			BigInteger deposit = BigInteger.valueOf(random.nextInt(1000));
			BigInteger value = BigInteger.valueOf(random.nextInt(1000));
			boolean fake = random.nextBoolean();
			byte[] salt = new byte[32];
			random.nextBytes(salt);
			StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);

			if (!fake && deposit.compareTo(value) >= 0)
        		if (expectedWinner == null || value.compareTo(maxBid) > 0) {
        			maxBid = value;
        			expectedWinner = blockchain.account(player);
        		}
        		else if (value.equals(maxBid))
        			// we do not allow ex aequos, since the winner would depend on the fastest player to reveal
        			continue;

			// we store the explicit bid in memory, not yet in blockchain, since it would be visible there
			bids.add(new BidToReveal(player, value, fake, salt));
        	blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(player), _100_000, classpath, BID, auction, new BigIntegerValue(deposit), bytes32));

        	i++;
		}

		waitUntil(BIDDING_TIME + 5000, start);

		// we create a storage list for each of the players
		StorageReference[] lists = {
			null, // unused, since player 0 is the beneficiary
			blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(blockchain.account(1), _100_000, classpath, CONSTRUCTOR_STORAGE_LIST)),
			blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(blockchain.account(2), _100_000, classpath, CONSTRUCTOR_STORAGE_LIST)),
			blockchain.addConstructorCallTransaction(new ConstructorCallTransactionRequest(blockchain.account(3), _100_000, classpath, CONSTRUCTOR_STORAGE_LIST))
		};

		// we create the revealed bids in blockchain; this is safe now, since the bidding time is over
		for (BidToReveal bid: bids)
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(bid.player), _100_000, classpath, ADD, lists[bid.player], bid.intoBlockchain()));

		for (int player = 1; player <= 3; player++)
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(player), _10_000_000, classpath, REVEAL, auction, lists[player]));

		waitUntil(BIDDING_TIME + REVEAL_TIME + 5000, start);

		StorageReference winner = (StorageReference) blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
			(blockchain.account(0), _100_000, classpath, AUCTION_END, auction));

		assertEquals(expectedWinner, winner);
	}

	private void waitUntil(long duration, long start) throws TransactionException, CodeExecutionException {
		while (System.currentTimeMillis() - start < duration) {
			//System.out.println(System.currentTimeMillis() - start);
			sleep(100);
			// we need to perform dummy transactions, otherwise the blockchain time does not progress
			blockchain.addInstanceMethodCallTransaction(new InstanceMethodCallTransactionRequest
				(blockchain.account(0), _100_000, classpath, GET_BALANCE, blockchain.account(0)));
		}
	}

	private StorageReference codeAsBytes32(int player, BigInteger value, boolean fake, byte[] salt) throws TransactionException, CodeExecutionException {
		digest.reset();
		digest.update(value.toByteArray());
		digest.update(fake ? (byte) 0 : (byte) 1);
		digest.update(salt);
		byte[] hash = digest.digest();
		return createBytes32(player, hash);
	}

	private StorageReference createBytes32(int player, byte[] hash) throws TransactionException, CodeExecutionException {
		return blockchain.addConstructorCallTransaction
			(new ConstructorCallTransactionRequest(blockchain.account(player), _100_000, classpath, CONSTRUCTOR_BYTES32,
				new ByteValue(hash[0]), new ByteValue(hash[1]), new ByteValue(hash[2]), new ByteValue(hash[3]),
				new ByteValue(hash[4]), new ByteValue(hash[5]), new ByteValue(hash[6]), new ByteValue(hash[7]),
				new ByteValue(hash[8]), new ByteValue(hash[9]), new ByteValue(hash[10]), new ByteValue(hash[11]),
				new ByteValue(hash[12]), new ByteValue(hash[13]), new ByteValue(hash[14]), new ByteValue(hash[15]),
				new ByteValue(hash[16]), new ByteValue(hash[17]), new ByteValue(hash[18]), new ByteValue(hash[19]),
				new ByteValue(hash[20]), new ByteValue(hash[21]), new ByteValue(hash[22]), new ByteValue(hash[23]),
				new ByteValue(hash[24]), new ByteValue(hash[25]), new ByteValue(hash[26]), new ByteValue(hash[27]),
				new ByteValue(hash[28]), new ByteValue(hash[29]), new ByteValue(hash[30]), new ByteValue(hash[31])));
	}

	private static void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e) {}
	}
}