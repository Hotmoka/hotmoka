package auction;

import takamaka.lang.Contract;
import takamaka.lang.Entry;
import takamaka.lang.LoggableContract;
import takamaka.lang.Payable;

public class SimpleAuction extends LoggableContract {
    // Parameters of the auction
	private final Contract beneficiary;
	private final long auctionEnd;

    // Current state of the auction.
	private Contract highestBidder;
	private int highestBid;

    // Set to true at the end, disallows any change
	private boolean ended;

    /// Create a simple auction with `_biddingTime`
    /// seconds bidding time on behalf of the
    /// beneficiary address `_beneficiary`.
	public SimpleAuction(int _biddingTime, Contract _beneficiary) {
        beneficiary = _beneficiary;
        // how can this be accepted into consensus?
        auctionEnd = System.currentTimeMillis() + _biddingTime;
    }

    /// Bid on the auction with the value sent together with this transaction.
    /// The value will only be refunded if the auction is not won.
	public @Payable @Entry void bid(int amount) {
        // Revert the call if the bidding period is over.
        require(System.currentTimeMillis() <= auctionEnd, "Auction already ended.");

        // If the bid is not higher, send the money back.
        require(amount > highestBid, "There already is a higher bid.");

        if (highestBid != 0)
        	// pay cannot be redefined, hence there is no risk of reentrancy
        	pay(highestBidder, highestBid);

        highestBidder = caller();
        highestBid = amount;
        log("bid increased", caller(), amount);
    }

    /// End the auction and send the highest bid to the beneficiary.
	public void auctionEnd() {
        require(System.currentTimeMillis() >= auctionEnd, "Auction not yet ended.");
        require(!ended, "auctionEnd has already been called.");

        ended = true;
        log("auction end", highestBidder, highestBid);

        pay(beneficiary, highestBid);
    }
}