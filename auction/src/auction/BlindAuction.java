package auction;

import static takamaka.lang.Takamaka.event;
import static takamaka.lang.Takamaka.require;

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
        private final int deposit;
        private Bid(byte[] blindedBid, int deposit) {
        	this.blindedBid = blindedBid;
        	this.deposit = deposit;
        }
    }

    private final PayableContract beneficiary;
    private final StorageMap<PayableContract, StorageList<Bid>> bids = new StorageMap<>();
    private final long biddingEnd;
    private final long revealEnd;
    private boolean ended;

    private PayableContract highestBidder;
    private int highestBid;

    public BlindAuction(int _biddingTime, int _revealTime, PayableContract _beneficiary) {
        beneficiary = _beneficiary;
        biddingEnd = System.currentTimeMillis() + _biddingTime;
        revealEnd = biddingEnd + _revealTime;
    }

    /// Place a blinded bid with `_blindedBid` = keccak256(value, fake, secret).
    /// The sent money is only refunded if the bid is correctly
    /// revealed in the revealing phase. The bid is valid if the
    /// money sent together with the bid is at least "value" and
    /// "fake" is not true. Setting "fake" to true and sending
    /// not the exact amount are ways to hide the real bid but
    /// still make the required deposit. The same address can place multiple bids.
    public @Payable @Entry(PayableContract.class) void bid(int amount, byte[] blindedBid) {
    	onlyBefore(biddingEnd);
        bids.getOrDefault(caller(), StorageList::new).add(new Bid(blindedBid, amount));
    }

    private void onlyBefore(long when) {
    	require(System.currentTimeMillis() < when, "Too late.");
    }

    private void onlyAfter(long when) {
    	require(System.currentTimeMillis() > when, "Too early.");
    }

    /// Reveal your blinded bids. You will get a refund for all correctly
    /// blinded invalid bids and for all bids except for the totally highest.
    public @Entry(PayableContract.class) void reveal(int[] _values, boolean[] _fake, byte[][] _secret) {
        onlyAfter(biddingEnd);
        onlyBefore(revealEnd);

        StorageList<Bid> bids = this.bids.getOrDefault(caller(), StorageList::new);
        int length = bids.size();
        require(_values.length == length, "inconsistent parameters size");
        require(_fake.length == length, "inconsistent parameters size");
        require(_secret.length == length, "inconsistent parameters size");

        int refund = 0;
        for (int i = 0; i < length; i++) {
            Bid bid = bids.get(i);
            int value = _values[i];
            boolean fake = _fake[i];
            byte[] secret = _secret[i];

            if (!Arrays.equals(bid.blindedBid, Keccak256.of(value, fake, secret)))
                // Bid was not actually revealed. Do not refund deposit.
                continue;

            refund += bid.deposit;
            if (!fake && bid.deposit >= value && placeBid((PayableContract) caller(), value))
            	refund -= value;

            // Make it impossible for the sender to re-claim the same deposit.
            bid.blindedBid = new byte[32];
        }

        ((PayableContract) caller()).receive(refund);
    }

    private boolean placeBid(PayableContract bidder, int value) {
        if (value <= highestBid)
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
        require(!ended, "auction is already ended");
        
        ended = true;
        beneficiary.receive(highestBid);
        event(new AuctionEnd(highestBidder, highestBid));
    }
}