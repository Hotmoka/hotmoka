package auction;

import java.math.BigInteger;

import takamaka.lang.Contract;
import takamaka.lang.Event;

public class AuctionEnd extends Event {
	public final Contract highestBidder;
	public final BigInteger highestBid;

	AuctionEnd(Contract highestBidder, BigInteger highestBid) {
		this.highestBidder = highestBidder;
		this.highestBid = highestBid;
	}
}