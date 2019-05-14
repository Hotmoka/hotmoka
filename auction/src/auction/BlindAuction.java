package auction;

import static takamaka.lang.Takamaka.event;
import static takamaka.lang.Takamaka.now;
import static takamaka.lang.Takamaka.require;

import java.math.BigInteger;
import java.util.Arrays;

import takamaka.crypto.Keccak256;
import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.Payable;
import takamaka.lang.PayableContract;
import takamaka.lang.Storage;
import takamaka.util.StorageList;
import takamaka.util.StorageMap;

public class BlindAuction extends Contract {

	private static class Bid extends Storage {
        private byte[] blindedBid; // 32 bytes hash
        private final BigInteger deposit;
        private Bid(byte[] blindedBid, BigInteger deposit) {
        	this.blindedBid = blindedBid;
        	this.deposit = deposit;
        }
    }

	public static class RevealedBid extends Storage {
		private final BigInteger value;
		private final boolean fake;
		private final byte[] secret;

		public RevealedBid(BigInteger value, boolean fake, byte[] secret) {
			this.value = value;
			this.fake = fake;
			this.secret = secret;
		}
	}

	private final PayableContract beneficiary;
    private final StorageMap<PayableContract, StorageList<Bid>> bids = new StorageMap<>();
    private final long biddingEnd;
    private final long revealEnd;

    private PayableContract highestBidder;
    private BigInteger highestBid = BigInteger.ZERO;

    public BlindAuction(int biddingTime, int revealTime, PayableContract _beneficiary) {
    	this.beneficiary = _beneficiary;
        this.biddingEnd = now() + biddingTime;
        this.revealEnd = biddingEnd + revealTime;
    }

    /// Place a blinded bid with `_blindedBid` = keccak256(value, fake, secret).
    /// The sent money is only refunded if the bid is correctly
    /// revealed in the revealing phase. The bid is valid if the
    /// money sent together with the bid is at least "value" and
    /// "fake" is not true. Setting "fake" to true and sending
    /// not the exact amount are ways to hide the real bid but
    /// still make the required deposit. The same address can place multiple bids.
    public @Payable @Entry(PayableContract.class) void bid(BigInteger amount, byte[] blindedBid) {
    	onlyBefore(biddingEnd);
        bids.computeIfAbsent((PayableContract) caller(), StorageList::new).add(new Bid(blindedBid, amount));
    }

    /// Reveal your blinded bids. You will get a refund for all correctly
    /// blinded invalid bids and for all bids except for the totally highest.
    public @Entry(PayableContract.class) void reveal(StorageList<RevealedBid> revealedBids) {
        onlyAfter(biddingEnd);
        onlyBefore(revealEnd);

        StorageList<Bid> bids = this.bids.getOrDefault(caller(), StorageList::new);
        int length = bids.size();
        require(revealedBids.size() == length, () -> "Expecting " + length + " revealed bid");

        BigInteger refund = BigInteger.ZERO;
        for (int i = 0; i < length; i++) {
            Bid bid = bids.get(i);
            RevealedBid revealedBid = revealedBids.get(i);
            BigInteger value = revealedBid.value;
            boolean fake = revealedBid.fake;
            byte[] secret = revealedBid.secret;

            if (!Arrays.equals(bid.blindedBid, Keccak256.of(value, fake, secret)))
                // Bid was not actually revealed. Do not refund deposit.
                continue;

            refund = refund.add(bid.deposit);
            if (!fake && bid.deposit.compareTo(value) >= 0 && placeBid((PayableContract) caller(), value))
            	refund = refund.subtract(value);

            // Make it impossible for the sender to re-claim the same deposit.
            bid.blindedBid = null;
        }

        ((PayableContract) caller()).receive(refund);
    }

    private boolean placeBid(PayableContract bidder, BigInteger value) {
        if (value.compareTo(highestBid) <= 0)
            return false;

        if (highestBidder != null)
            // Refund the previously highest bidder.
            highestBidder.receive(highestBid);

        highestBid = value;
        highestBidder = bidder;
        event(new BidIncrease(bidder, value));

        return true;
    }

    /// End the auction and send the highest bid to the beneficiary.
    public void auctionEnd() {
        onlyAfter(revealEnd);

        if (highestBidder != null) {
        	beneficiary.receive(highestBid);
        	event(new AuctionEnd(highestBidder, highestBid));
        	highestBidder = null;
        }
    }

	private static void onlyBefore(long when) {
		require(now() < when, "Too late");
	}

	private static void onlyAfter(long when) {
		require(now() > when, "Too early");
	}
}