/**
 * 
 */
package io.takamaka.tests;

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.BasicTypes.BYTE;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.CodeExecutionException;
import io.hotmoka.beans.TransactionException;
import io.hotmoka.beans.TransactionRejectedException;
import io.hotmoka.beans.signatures.ConstructorSignature;
import io.hotmoka.beans.signatures.MethodSignature;
import io.hotmoka.beans.signatures.NonVoidMethodSignature;
import io.hotmoka.beans.signatures.VoidMethodSignature;
import io.hotmoka.beans.types.ClassType;
import io.hotmoka.beans.values.BigIntegerValue;
import io.hotmoka.beans.values.BooleanValue;
import io.hotmoka.beans.values.ByteValue;
import io.hotmoka.beans.values.IntValue;
import io.hotmoka.beans.values.NullValue;
import io.hotmoka.beans.values.StorageReference;
import io.hotmoka.beans.values.StorageValue;
import io.hotmoka.nodes.Node.CodeSupplier;
import io.takamaka.code.constants.Constants;

/**
 * A test for the blind auction contract.
 */
class BlindAuction extends TakamakaTest {

	/**
	 * The number of bids placed by the players.
	 */
	private static final int NUM_BIDS = 15;

	/**
	 * The bidding time of the experiments (in milliseconds).
	 */
	private static final int BIDDING_TIME = 40_000;

	/**
	 * The reveal time of the experiments (in millisecond).
	 */
	private static final int REVEAL_TIME = 20_000;

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

	private static final BigInteger _10_000_000_000 = BigInteger.valueOf(10_000_000_000L);

	private static final BigInteger _10_000_000 = BigInteger.valueOf(10_000_000);

	/**
	 * The hashing algorithm used to hide the bids.
	 */
	private MessageDigest digest;

	@BeforeEach
	void beforeEach() throws Exception {
		digest = MessageDigest.getInstance("SHA-256");
		mkBlockchain("auction.jar", _10_000_000_000, _10_000_000_000, _10_000_000_000, _10_000_000_000);
	}

	@Test @DisplayName("three players put bids before end of bidding time")
	void bids() throws TransactionException, CodeExecutionException, Exception {
		CodeSupplier<StorageReference> auction = postConstructorCallTransaction
			(account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, new IntValue(BIDDING_TIME), new IntValue(REVEAL_TIME));

		Random random = new Random();
		for (int i = 1; i <= NUM_BIDS; i++) {
			int player = 1 + random.nextInt(3);
			BigInteger deposit = BigInteger.valueOf(random.nextInt(1000));
			BigInteger value = BigInteger.valueOf(random.nextInt(1000));
			boolean fake = random.nextBoolean();
			byte[] salt = new byte[32];
			random.nextBytes(salt);
			StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);
			postInstanceMethodCallTransaction(account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), new BigIntegerValue(deposit), bytes32);
		}
	}

	@Test @DisplayName("three players put bids but bidding time expires")
	void biddingTimeExpires() throws TransactionException, CodeExecutionException, TransactionRejectedException {
		CodeSupplier<StorageReference> auction = postConstructorCallTransaction
			(account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, new IntValue(4000), new IntValue(REVEAL_TIME));

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
				addInstanceMethodCallTransaction(account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), new BigIntegerValue(deposit), bytes32);
				sleep(2000);
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
		private CodeSupplier<StorageReference> bytes32;

		private BidToReveal(int player, BigInteger value, boolean fake, byte[] salt) {
			this.player = player;
			this.value = value;
			this.fake = fake;
			this.salt = salt;
		}

		private CodeSupplier<StorageReference> intoBlockchain() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException {
			return postConstructorCallTransaction
        		(account(player), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_REVEALED_BID, new BigIntegerValue(value), new BooleanValue(fake), bytes32.get());
		}

		private void createBytes32() throws TransactionRejectedException {
			this.bytes32 = postConstructorCallTransaction
				(account(player), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BYTES32,
					new ByteValue(salt[0]), new ByteValue(salt[1]), new ByteValue(salt[2]), new ByteValue(salt[3]),
					new ByteValue(salt[4]), new ByteValue(salt[5]), new ByteValue(salt[6]), new ByteValue(salt[7]),
					new ByteValue(salt[8]), new ByteValue(salt[9]), new ByteValue(salt[10]), new ByteValue(salt[11]),
					new ByteValue(salt[12]), new ByteValue(salt[13]), new ByteValue(salt[14]), new ByteValue(salt[15]),
					new ByteValue(salt[16]), new ByteValue(salt[17]), new ByteValue(salt[18]), new ByteValue(salt[19]),
					new ByteValue(salt[20]), new ByteValue(salt[21]), new ByteValue(salt[22]), new ByteValue(salt[23]),
					new ByteValue(salt[24]), new ByteValue(salt[25]), new ByteValue(salt[26]), new ByteValue(salt[27]),
					new ByteValue(salt[28]), new ByteValue(salt[29]), new ByteValue(salt[30]), new ByteValue(salt[31]));
		}
	}

	@Test @DisplayName("three players put bids before end of bidding time then reveal")
	void bidsThenReveal() throws TransactionException, CodeExecutionException, TransactionRejectedException, InterruptedException {
		long start = System.currentTimeMillis();
		CodeSupplier<StorageReference> auction = postConstructorCallTransaction
			(account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, new IntValue(BIDDING_TIME), new IntValue(REVEAL_TIME));

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
        			expectedWinner = account(player);
        		}
        		else if (value.equals(maxBid))
        			// we do not allow ex aequos, since the winner would depend on the fastest player to reveal
        			continue;

			// we store the explicit bid in memory, not yet in blockchain, since it would be visible there
			bids.add(new BidToReveal(player, value, fake, salt));
			postInstanceMethodCallTransaction(account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), new BigIntegerValue(deposit), bytes32);

        	i++;
		}

		waitUntil(BIDDING_TIME + 5000, start);

		// we create a storage list for each of the players
		CodeSupplier<?>[] lists = {
			null, // unused, since player 0 is the beneficiary
			postConstructorCallTransaction(account(1), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_STORAGE_LIST),
			postConstructorCallTransaction(account(2), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_STORAGE_LIST),
			postConstructorCallTransaction(account(3), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_STORAGE_LIST)
		};

		// we create the revealed bids in blockchain; this is safe now, since the bidding time is over
		List<CodeSupplier<?>> revealedBids = new ArrayList<>();
		for (BidToReveal bid: bids)
			bid.createBytes32();

		for (BidToReveal bid: bids)
			revealedBids.add(bid.intoBlockchain());

		Iterator<CodeSupplier<?>> it = revealedBids.iterator();
		for (BidToReveal bid: bids)
			postInstanceMethodCallTransaction(account(bid.player), _100_000, BigInteger.ONE, jar(), ADD, (StorageReference) lists[bid.player].get(), it.next().get());

		for (int player = 1; player <= 3; player++)
			postInstanceMethodCallTransaction(account(player), _10_000_000, BigInteger.ONE, jar(), REVEAL, auction.get(), lists[player].get());

		waitUntil(BIDDING_TIME + REVEAL_TIME + 5000, start);

		// the winner can be a StorageReference but also a NullValue, if all bids were fake
		StorageValue winner = addInstanceMethodCallTransaction(account(0), _100_000, BigInteger.ONE, jar(), AUCTION_END, auction.get());
		if (winner instanceof NullValue)
			winner = null;

		assertEquals(expectedWinner, winner);
	}

	private void waitUntil(long duration, long start) throws TransactionException, CodeExecutionException, TransactionRejectedException {
		while (System.currentTimeMillis() - start < duration) {
			sleep(100);
			// we need to perform dummy transactions, otherwise the blockchain time does not progress
			addInstanceMethodCallTransaction(account(0), _100_000, BigInteger.ONE, jar(), GET_BALANCE, account(0));
		}
	}

	private StorageReference codeAsBytes32(int player, BigInteger value, boolean fake, byte[] salt) throws TransactionException, CodeExecutionException, TransactionRejectedException {
		digest.reset();
		digest.update(value.toByteArray());
		digest.update(fake ? (byte) 0 : (byte) 1);
		digest.update(salt);
		byte[] hash = digest.digest();
		return createBytes32(player, hash);
	}

	private StorageReference createBytes32(int player, byte[] hash) throws TransactionException, CodeExecutionException, TransactionRejectedException {
		return addConstructorCallTransaction
			(account(player), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BYTES32,
				new ByteValue(hash[0]), new ByteValue(hash[1]), new ByteValue(hash[2]), new ByteValue(hash[3]),
				new ByteValue(hash[4]), new ByteValue(hash[5]), new ByteValue(hash[6]), new ByteValue(hash[7]),
				new ByteValue(hash[8]), new ByteValue(hash[9]), new ByteValue(hash[10]), new ByteValue(hash[11]),
				new ByteValue(hash[12]), new ByteValue(hash[13]), new ByteValue(hash[14]), new ByteValue(hash[15]),
				new ByteValue(hash[16]), new ByteValue(hash[17]), new ByteValue(hash[18]), new ByteValue(hash[19]),
				new ByteValue(hash[20]), new ByteValue(hash[21]), new ByteValue(hash[22]), new ByteValue(hash[23]),
				new ByteValue(hash[24]), new ByteValue(hash[25]), new ByteValue(hash[26]), new ByteValue(hash[27]),
				new ByteValue(hash[28]), new ByteValue(hash[29]), new ByteValue(hash[30]), new ByteValue(hash[31]));
	}

	private static void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException e) {}
	}
}