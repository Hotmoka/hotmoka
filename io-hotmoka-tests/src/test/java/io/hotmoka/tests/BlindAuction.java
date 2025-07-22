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

import static io.hotmoka.node.StorageTypes.BOOLEAN;
import static io.hotmoka.node.StorageTypes.BYTE;
import static io.hotmoka.node.StorageTypes.INT;
import static io.hotmoka.node.StorageValues.byteOf;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.node.ConstructorSignatures;
import io.hotmoka.node.MethodSignatures;
import io.hotmoka.node.StorageTypes;
import io.hotmoka.node.StorageValues;
import io.hotmoka.node.api.ConstructorFuture;
import io.hotmoka.node.api.signatures.ConstructorSignature;
import io.hotmoka.node.api.signatures.NonVoidMethodSignature;
import io.hotmoka.node.api.signatures.VoidMethodSignature;
import io.hotmoka.node.api.types.ClassType;
import io.hotmoka.node.api.values.NullValue;
import io.hotmoka.node.api.values.StorageReference;
import io.hotmoka.node.api.values.StorageValue;
import io.takamaka.code.constants.Constants;

/**
 * A test for the blind auction contract.
 */
class BlindAuction extends HotmokaTest {

	/**
	 * The number of bids placed by the players.
	 */
	private static final int NUM_BIDS = 10;

	/**
	 * The bidding time of the experiments (in milliseconds).
	 */
	private static int BIDDING_TIME = 5_000;

	/**
	 * The reveal time of the experiments (in millisecond).
	 */
	private static int REVEAL_TIME = 8_000;

	private static final ClassType BLIND_AUCTION = StorageTypes.classNamed("io.hotmoka.examples.auction.BlindAuction");

	private static final ConstructorSignature CONSTRUCTOR_BLIND_AUCTION = ConstructorSignatures.of(BLIND_AUCTION, INT, INT);

	private static final ConstructorSignature CONSTRUCTOR_BYTES32_SNAPSHOT = ConstructorSignatures.of
		(StorageTypes.BYTES32_SNAPSHOT,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE);

	private static final ConstructorSignature CONSTRUCTOR_REVEALED_BID = ConstructorSignatures.of
		(StorageTypes.classNamed("io.hotmoka.examples.auction.BlindAuction$RevealedBid"),
			StorageTypes.BIG_INTEGER, BOOLEAN, StorageTypes.BYTES32_SNAPSHOT);

	private static final VoidMethodSignature BID = MethodSignatures.ofVoid(BLIND_AUCTION, "bid", StorageTypes.BIG_INTEGER, StorageTypes.BYTES32_SNAPSHOT);

	private static final VoidMethodSignature REVEAL = MethodSignatures.ofVoid(BLIND_AUCTION, "reveal",
			StorageTypes.classNamed("io.hotmoka.examples.auction.BlindAuction$RevealedBid"));

	private static final NonVoidMethodSignature AUCTION_END = MethodSignatures.ofNonVoid(BLIND_AUCTION, "auctionEnd", StorageTypes.PAYABLE_CONTRACT);

	/**
	 * The hashing algorithm used to hide the bids.
	 */
	private MessageDigest digest;

	private final static Logger LOGGER = Logger.getLogger(BlindAuction.class.getName());

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("auction.jar");

		String type = node.getInfo().getType();

		if (type.contains("TendermintNode")) {
			// the Tendermint blockchain is slower and requires more time for all transactions in this test
			BIDDING_TIME = 30_000;
			REVEAL_TIME = 40_000;
		}
		else if (type.contains("MokamintNode")) {
			// the Mokamint blockchain is slower and requires more time for all transactions in this test
			BIDDING_TIME = 90_000;
			REVEAL_TIME = 170_000;
		}
	}

	@BeforeEach
	void beforeEach() throws Exception {
		digest = MessageDigest.getInstance("SHA-256");
		setAccounts(_10_000_000_000, _10_000_000_000, _10_000_000_000, _10_000_000_000);
	}

	@Test @DisplayName("three players put bids before end of bidding time")
	void bids() throws Exception {
		ConstructorFuture auction = postConstructorCallTransaction
			(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, StorageValues.intOf(BIDDING_TIME), StorageValues.intOf(REVEAL_TIME));

		var random = new Random();
		for (int i = 1; i <= NUM_BIDS; i++) {
			int player = 1 + random.nextInt(3);
			var deposit = BigInteger.valueOf(random.nextInt(1000));
			var value = BigInteger.valueOf(random.nextInt(1000));
			boolean fake = random.nextInt(100) >= 80; // fake in 20% of the cases
			var salt = new byte[32];
			random.nextBytes(salt);
			StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);
			addInstanceVoidMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), StorageValues.bigIntegerOf(deposit), bytes32);
			LOGGER.info("bidding " + i + "/" + NUM_BIDS);
		}
	}

	@Test @DisplayName("three players put bids but bidding time expires")
	void biddingTimeExpires() throws Exception {
		ConstructorFuture auction = postConstructorCallTransaction
			(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, StorageValues.intOf(4000), StorageValues.intOf(REVEAL_TIME));

		throwsTransactionExceptionWithCause(Constants.REQUIREMENT_VIOLATION_EXCEPTION_NAME, () ->
		{
			var random = new Random();
			for (int i = 1; i <= NUM_BIDS; i++) {
				int player = 1 + random.nextInt(3);
				var deposit = BigInteger.valueOf(random.nextInt(1000));
				var value = BigInteger.valueOf(random.nextInt(1000));
				var fake = random.nextBoolean();
				var salt = new byte[32];
				random.nextBytes(salt);
				StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);
				addInstanceVoidMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), StorageValues.bigIntegerOf(deposit), bytes32);
				sleep(2000);
				LOGGER.info("bidding " + i + "/" + NUM_BIDS);
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
		private StorageReference bytes32;

		private BidToReveal(int player, BigInteger value, boolean fake, byte[] salt) {
			this.player = player;
			this.value = value;
			this.fake = fake;
			this.salt = salt;
		}

		private StorageReference intoStore() throws Exception {
			return addConstructorCallTransaction
        		(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_REVEALED_BID, StorageValues.bigIntegerOf(value), StorageValues.booleanOf(fake), bytes32);
		}

		private void createBytes32() throws Exception {
			this.bytes32 = addConstructorCallTransaction
				(privateKey(player), account(player), _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_BYTES32_SNAPSHOT,
					byteOf(salt[0]), byteOf(salt[1]), byteOf(salt[2]), byteOf(salt[3]),
					byteOf(salt[4]), byteOf(salt[5]), byteOf(salt[6]), byteOf(salt[7]),
					byteOf(salt[8]), byteOf(salt[9]), byteOf(salt[10]), byteOf(salt[11]),
					byteOf(salt[12]), byteOf(salt[13]), byteOf(salt[14]), byteOf(salt[15]),
					byteOf(salt[16]), byteOf(salt[17]), byteOf(salt[18]), byteOf(salt[19]),
					byteOf(salt[20]), byteOf(salt[21]), byteOf(salt[22]), byteOf(salt[23]),
					byteOf(salt[24]), byteOf(salt[25]), byteOf(salt[26]), byteOf(salt[27]),
					byteOf(salt[28]), byteOf(salt[29]), byteOf(salt[30]), byteOf(salt[31]));
		}
	}

	@Test @DisplayName("three players put bids before end of bidding time then reveal")
	void bidsThenReveal() throws Exception {
		final long start = System.currentTimeMillis();
		ConstructorFuture auction = postConstructorCallTransaction
			(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, StorageValues.intOf(BIDDING_TIME), StorageValues.intOf(REVEAL_TIME));

		List<BidToReveal> bids = new ArrayList<>();

		var maxBid = BigInteger.ZERO;
		StorageReference expectedWinner = null;
		var random = new Random();
		int i = 1;
		while (i <= NUM_BIDS) {
			int player = 1 + random.nextInt(3);
			var deposit = BigInteger.valueOf(random.nextInt(1000));
			var value = BigInteger.valueOf(random.nextInt(1000));
			boolean fake = random.nextBoolean();
			var salt = new byte[32];
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
			addInstanceVoidMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), StorageValues.bigIntegerOf(deposit), bytes32);

			LOGGER.info("bidding " + i + "/" + NUM_BIDS);
        	i++;
		}

		waitUntil(BIDDING_TIME + 5000, start);

		// we create the revealed bids in the node; this is safe now, since the bidding time is over
		for (BidToReveal bid: bids)
			bid.createBytes32();

		List<StorageReference> bidsInStore = new ArrayList<>();
		for (BidToReveal bid: bids)
			bidsInStore.add(bid.intoStore());

		int counter = 1;
		Iterator<BidToReveal> it = bids.iterator();
		for (StorageReference bidInStore: bidsInStore) {
			int player = it.next().player;
			addInstanceVoidMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), REVEAL, auction.get(), bidInStore);
			LOGGER.info("revealing " + counter + "/" + bids.size());
			counter++;
		}

		waitUntil(BIDDING_TIME + REVEAL_TIME + 5000, start);

		LOGGER.info("ending the auction");
		// the winner can be a StorageReference but also a NullValue, if all bids were fake
		StorageValue winner = addInstanceNonVoidMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), AUCTION_END, auction.get());
		LOGGER.info("auction ended");
		if (winner instanceof NullValue)
			winner = null;

		assertEquals(expectedWinner, winner);
	}

	private void waitUntil(long duration, long start) throws InterruptedException {
		long toSleep = duration - (System.currentTimeMillis() - start);
		LOGGER.info("sleeping for " + toSleep + "ms...");
		if (toSleep > 0)
			sleep(toSleep);

		LOGGER.info("waking up");
	}

	private StorageReference codeAsBytes32(int player, BigInteger value, boolean fake, byte[] salt) throws Exception {
		digest.reset();
		digest.update(value.toByteArray());
		digest.update(fake ? (byte) 0 : (byte) 1);
		digest.update(salt);
		byte[] hash = digest.digest();
		return createBytes32(player, hash);
	}

	private StorageReference createBytes32(int player, byte[] hash) throws Exception {
		return addConstructorCallTransaction
			(privateKey(player), account(player), _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_BYTES32_SNAPSHOT,
				byteOf(hash[0]), byteOf(hash[1]), byteOf(hash[2]), byteOf(hash[3]),
				byteOf(hash[4]), byteOf(hash[5]), byteOf(hash[6]), byteOf(hash[7]),
				byteOf(hash[8]), byteOf(hash[9]), byteOf(hash[10]), byteOf(hash[11]),
				byteOf(hash[12]), byteOf(hash[13]), byteOf(hash[14]), byteOf(hash[15]),
				byteOf(hash[16]), byteOf(hash[17]), byteOf(hash[18]), byteOf(hash[19]),
				byteOf(hash[20]), byteOf(hash[21]), byteOf(hash[22]), byteOf(hash[23]),
				byteOf(hash[24]), byteOf(hash[25]), byteOf(hash[26]), byteOf(hash[27]),
				byteOf(hash[28]), byteOf(hash[29]), byteOf(hash[30]), byteOf(hash[31]));
	}
}