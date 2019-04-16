package auction;

import takamaka.lang.Contract;
import takamaka.lang.Event;

public class AuctionEnd extends Event {
	public final Contract highestBidder;
	public final int highestBid;

	AuctionEnd(Contract highestBidder, int highestBid) {
		this.highestBidder = highestBidder;
		this.highestBid = highestBid;
	}
}