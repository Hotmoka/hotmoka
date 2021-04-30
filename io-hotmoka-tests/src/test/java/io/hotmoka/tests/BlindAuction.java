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

import static io.hotmoka.beans.types.BasicTypes.BOOLEAN;
import static io.hotmoka.beans.types.BasicTypes.BYTE;
import static io.hotmoka.beans.types.BasicTypes.INT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
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
import io.hotmoka.remote.RemoteNode;
import io.takamaka.code.constants.Constants;

/**
 * A test for the blind auction contract.
 */
class BlindAuction extends TakamakaTest {

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

	private static final ClassType BLIND_AUCTION = new ClassType("io.hotmoka.examples.auction.BlindAuction");

	private static final ConstructorSignature CONSTRUCTOR_BLIND_AUCTION = new ConstructorSignature(BLIND_AUCTION, INT, INT);

	private static final ConstructorSignature CONSTRUCTOR_BYTES32_SNAPSHOT = new ConstructorSignature
		(ClassType.BYTES32_SNAPSHOT,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE,
			BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE, BYTE);

	private static final ConstructorSignature CONSTRUCTOR_REVEALED_BID = new ConstructorSignature(new ClassType("io.hotmoka.examples.auction.BlindAuction$RevealedBid"),
			ClassType.BIG_INTEGER, BOOLEAN, ClassType.BYTES32_SNAPSHOT);

	private static final MethodSignature BID = new VoidMethodSignature(BLIND_AUCTION, "bid", ClassType.BIG_INTEGER, ClassType.BYTES32_SNAPSHOT);

	private static final MethodSignature REVEAL = new VoidMethodSignature(BLIND_AUCTION, "reveal", new ClassType("io.hotmoka.examples.auction.BlindAuction$RevealedBid"));

	private static final MethodSignature AUCTION_END = new NonVoidMethodSignature(BLIND_AUCTION, "auctionEnd", ClassType.PAYABLE_CONTRACT);

	/**
	 * The hashing algorithm used to hide the bids.
	 */
	private MessageDigest digest;

	@BeforeAll
	static void beforeAll() throws Exception {
		setJar("auction.jar");

		if (tendermintBlockchain != null || node instanceof RemoteNode) {
			// the Tendermint blockchain is slower and requires more time for all transactions in this test
			BIDDING_TIME = 40_000;
			REVEAL_TIME = 70_000;
		}
	}

	@BeforeEach
	void beforeEach() throws Exception {
		digest = MessageDigest.getInstance("SHA-256");
		setAccounts(_10_000_000_000, _10_000_000_000, _10_000_000_000, _10_000_000_000);
	}

	@Test @DisplayName("three players put bids before end of bidding time")
	void bids() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> auction = postConstructorCallTransaction
			(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, new IntValue(BIDDING_TIME), new IntValue(REVEAL_TIME));

		Random random = new Random();
		for (int i = 1; i <= NUM_BIDS; i++) {
			int player = 1 + random.nextInt(3);
			BigInteger deposit = BigInteger.valueOf(random.nextInt(1000));
			BigInteger value = BigInteger.valueOf(random.nextInt(1000));
			boolean fake = random.nextBoolean();
			byte[] salt = new byte[32];
			random.nextBytes(salt);
			StorageReference bytes32 = codeAsBytes32(player, value, fake, salt);
			addInstanceMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), new BigIntegerValue(deposit), bytes32);
		}
	}

	@Test @DisplayName("three players put bids but bidding time expires")
	void biddingTimeExpires() throws TransactionRejectedException, InvalidKeyException, SignatureException {
		CodeSupplier<StorageReference> auction = postConstructorCallTransaction
			(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, new IntValue(4000), new IntValue(REVEAL_TIME));

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
				addInstanceMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), new BigIntegerValue(deposit), bytes32);
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
		private StorageReference bytes32;

		private BidToReveal(int player, BigInteger value, boolean fake, byte[] salt) {
			this.player = player;
			this.value = value;
			this.fake = fake;
			this.salt = salt;
		}

		private StorageReference intoBlockchain() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
			return addConstructorCallTransaction
        		(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_REVEALED_BID, new BigIntegerValue(value), new BooleanValue(fake), bytes32);
		}

		private void createBytes32() throws TransactionRejectedException, InvalidKeyException, SignatureException, TransactionException, CodeExecutionException {
			this.bytes32 = addConstructorCallTransaction
				(privateKey(player), account(player), _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_BYTES32_SNAPSHOT,
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
	void bidsThenReveal() throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		long start = System.currentTimeMillis();
		CodeSupplier<StorageReference> auction = postConstructorCallTransaction
			(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), CONSTRUCTOR_BLIND_AUCTION, new IntValue(BIDDING_TIME), new IntValue(REVEAL_TIME));

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
			addInstanceMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), BID, auction.get(), new BigIntegerValue(deposit), bytes32);

        	i++;
		}

		waitUntil(BIDDING_TIME + 5000, start);

		// we create the revealed bids in blockchain; this is safe now, since the bidding time is over
		for (BidToReveal bid: bids)
			bid.createBytes32();

		List<StorageReference> bidsInStore = new ArrayList<>();
		for (BidToReveal bid: bids)
			bidsInStore.add(bid.intoBlockchain());

		Iterator<BidToReveal> it = bids.iterator();
		for (StorageReference bidInStore: bidsInStore) {
			int player = it.next().player;
			addInstanceMethodCallTransaction(privateKey(player), account(player), _100_000, BigInteger.ONE, jar(), REVEAL, auction.get(), bidInStore);
		}

		waitUntil(BIDDING_TIME + REVEAL_TIME + 5000, start);

		// the winner can be a StorageReference but also a NullValue, if all bids were fake
		StorageValue winner = addInstanceMethodCallTransaction(privateKey(0), account(0), _100_000, BigInteger.ONE, jar(), AUCTION_END, auction.get());
		if (winner instanceof NullValue)
			winner = null;

		assertEquals(expectedWinner, winner);
	}

	private void waitUntil(long duration, long start) {
		while (System.currentTimeMillis() - start < duration) {
			sleep(100);
		}
	}

	private StorageReference codeAsBytes32(int player, BigInteger value, boolean fake, byte[] salt) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		digest.reset();
		digest.update(value.toByteArray());
		digest.update(fake ? (byte) 0 : (byte) 1);
		digest.update(salt);
		byte[] hash = digest.digest();
		return createBytes32(player, hash);
	}

	private StorageReference createBytes32(int player, byte[] hash) throws TransactionException, CodeExecutionException, TransactionRejectedException, InvalidKeyException, SignatureException {
		return addConstructorCallTransaction
			(privateKey(player), account(player), _500_000, BigInteger.ONE, jar(), CONSTRUCTOR_BYTES32_SNAPSHOT,
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